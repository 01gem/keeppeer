package com.gem.keeppeer.security

import android.app.KeyguardManager
import android.content.Context
import android.os.SystemClock
import com.gem.keeppeer.data.local.EncryptedDatabaseProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.crypto.Cipher

enum class VaultStage { FIRST_RUN, LEGACY_RESET, LOCKED, UNLOCKING, UNLOCKED, RECOVERABLE_ERROR }
data class VaultState(
    val stage: VaultStage = VaultStage.LOCKED,
    val access: VaultAccess? = null,
    val message: String? = null,
    val generation: Long = 0,
)
enum class LockTimeout(val milliseconds: Long, val label: String) {
    IMMEDIATE(0, "Immediate"), MINUTE(60_000, "1 minute"), FIVE_MINUTES(300_000, "5 minutes")
}

/** Application-scoped authentication boundary. Neither Activity nor a UI flag can open Room. */
class VaultSession(
    private val context: Context, val clipboard: SensitiveClipboard,
    namespace: String = "keeppeer",
    private val elapsedTime: () -> Long = SystemClock::elapsedRealtime,
    private val deviceLocked: () -> Boolean = { context.getSystemService(KeyguardManager::class.java).isKeyguardLocked },
) {
    private val crypto = CryptoManager()
    private val envelopes = EnvelopeStore(context, "$namespace.storage.v1")
    val biometrics = BiometricVault(context, "$namespace.biometric.v1")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val gate = Mutex()
    private val preferences = context.getSharedPreferences("lock-settings", Context.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(initialState())
    val state = mutableState.asStateFlow()
    private var serial = 0L
    private var attempt: Job? = null
    private val managementJobs = mutableSetOf<Job>()
    private fun manage(block: suspend CoroutineScope.() -> Unit) {
        val work = scope.launch(start = CoroutineStart.LAZY, block = block)
        managementJobs.add(work)
        work.invokeOnCompletion { managementJobs.remove(work) }
        work.start()
    }
    private var timer: Job? = null
    private var backgroundAt: Long? = null
    private var inForeground = false
    private var biometricApproval: Long? = null
    private val mutableTimeout = MutableStateFlow(
        LockTimeout.entries.firstOrNull { it.name == preferences.getString("timeout", null) } ?: LockTimeout.IMMEDIATE)
    val timeout = mutableTimeout.asStateFlow()

    private fun legacyExists() = listOf("", "-wal", "-shm", "-journal").any { suffix ->
        java.io.File(context.getDatabasePath(EncryptedDatabaseProvider.LEGACY_NAME).path + suffix).exists()
    }
    private fun activeDatabaseExists(): Boolean {
        val file = context.getDatabasePath(EncryptedDatabaseProvider.VAULT_NAME)
        // SQLCipher's default page size is 4096. Never let SQLite initialize a
        // truncated active file as a new empty vault.
        return file.isFile && file.length() >= 4096
    }
    private fun initialState(): VaultState = when {
        envelopes.exists() -> VaultState(VaultStage.LOCKED)
        context.getDatabasePath(EncryptedDatabaseProvider.VAULT_NAME).exists() ->
            VaultState(VaultStage.RECOVERABLE_ERROR, message = "Vault metadata is missing. Existing files were preserved. Restore is unavailable; do not clear app data.")
        legacyExists() ->
            VaultState(VaultStage.LEGACY_RESET)
        else -> VaultState(VaultStage.FIRST_RUN)
    }
    fun setTimeout(value: LockTimeout) {
        mutableTimeout.value = value
        preferences.edit().putString("timeout", value.name).apply()
    }
    fun foreground() {
        inForeground = true
        val started = backgroundAt
        if (deviceLocked() ||
            started != null && elapsedTime() - started >= timeout.value.milliseconds) lock()
        backgroundAt = null
        timer?.cancel()
        // Retry clipboard clearing when Android once again permits access.
        clipboard.clear()
    }
    fun background() {
        inForeground = false
        clipboard.clear()
        biometricApproval = null
        backgroundAt = elapsedTime()
        if (state.value.stage != VaultStage.UNLOCKED || timeout.value == LockTimeout.IMMEDIATE) lock()
        else {
            timer?.cancel()
            timer = scope.launch { delay(timeout.value.milliseconds); lock() }
        }
    }
    fun lock() {
        serial++
        attempt?.cancel()
        attempt = null
        managementJobs.toList().forEach { it.cancel() }
        managementJobs.clear()
        biometricApproval = null
        clipboard.clear()
        timer?.cancel()
        val access = state.value.access
        access?.revoke()
        mutableState.value = initialState().copy(generation = serial)
        if (access != null) scope.launch(Dispatchers.IO) { gate.withLock { access.close() } }
    }

    /** User must explicitly confirm the development reset in the UI. Never touches encrypted files. */
    fun resetLegacy() {
        if (state.value.stage != VaultStage.LEGACY_RESET || attempt?.isActive == true) return
        attempt = scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    gate.withLock {
                        check(!envelopes.exists() && !context.getDatabasePath(EncryptedDatabaseProvider.VAULT_NAME).exists())
                        context.deleteDatabase(EncryptedDatabaseProvider.LEGACY_NAME)
                        check(!legacyExists())
                    }
                }
                mutableState.value = initialState()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                mutableState.value = VaultState(VaultStage.LEGACY_RESET, message = "Reset failed. No encrypted vault was changed.")
            }
        }
    }

    fun setup(password: CharArray, confirmation: CharArray) {
        if (state.value.stage != VaultStage.FIRST_RUN) { password.fill('\u0000'); confirmation.fill('\u0000'); return }
        if (password.size < 12 || !password.contentEquals(confirmation)) {
            password.fill('\u0000'); confirmation.fill('\u0000')
            mutableState.value = VaultState(VaultStage.FIRST_RUN, message = "Use at least 12 characters and matching passwords.")
            return
        }
        confirmation.fill('\u0000')
        authenticate(password, setup = true)
    }
    fun unlock(password: CharArray) {
        if (state.value.stage !in listOf(VaultStage.LOCKED, VaultStage.RECOVERABLE_ERROR) || !envelopes.exists()) {
            password.fill('\u0000'); return
        }
        authenticate(password, setup = false)
    }
    private fun authenticate(password: CharArray, setup: Boolean) {
        val ticket = ++serial
        mutableState.value = VaultState(VaultStage.UNLOCKING)
        attempt = scope.launch {
            var result: VaultAccess? = null
            try {
                withContext(Dispatchers.IO) {
                    gate.withLock {
                        var createAllowed = setup
                        var key: ByteArray? = null
                        var db: com.gem.keeppeer.data.local.AppDatabase? = null
                        try {
                            if (setup) {
                                check(!envelopes.exists())
                                check(!context.getDatabasePath(EncryptedDatabaseProvider.VAULT_NAME).exists())
                                check(!legacyExists())
                                key = crypto.randomKey()
                                // Commit recoverable metadata BEFORE creating the database.
                                // An interrupted first creation never silently regenerates the key.
                                envelopes.initializeStorageKey()
                                val wrapped = crypto.wrap(password, key)
                                try { envelopes.write(wrapped, creationPending = true) } finally { wrapped.fill(0) }
                            } else {
                                val record = envelopes.readRecord()
                                createAllowed = record.creationPending
                                try {
                                    check(record.creationPending || activeDatabaseExists()) { "Missing or truncated vault database" }
                                    key = crypto.unwrap(password, record.envelope)
                                } finally { record.envelope.fill(0) }
                            }
                            currentCoroutineContext().ensureActive()
                            db = EncryptedDatabaseProvider.open(context, key, allowCreate = createAllowed)
                            val record = envelopes.readRecord()
                            try { if (record.creationPending) envelopes.write(record.envelope) }
                            finally { record.envelope.fill(0) }
                            result = VaultAccess(ticket, db, key, passwordAuthenticated = true)
                            key = null
                            db = null
                        } finally { db?.close(); key?.fill(0); password.fill('\u0000') }
                    }
                }
                if (serial == ticket && inForeground && !deviceLocked()) {
                    mutableState.value = VaultState(VaultStage.UNLOCKED, result)
                    result = null
                } else if (serial == ticket) lock()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (serial == ticket) mutableState.value = VaultState(
                    if (setup && !envelopes.exists()) VaultStage.FIRST_RUN else VaultStage.RECOVERABLE_ERROR,
                    message = "Could not unlock. Check your password. Damaged or missing device keys or vault files also prevent access. Existing data was preserved.")
            } finally {
                password.fill('\u0000')
                result?.let { abandoned -> abandoned.revoke(); withContext(NonCancellable + Dispatchers.IO) { abandoned.close() } }
            }
        }
    }

    fun changePassword(current: CharArray, replacement: CharArray, confirmation: CharArray, done: (String) -> Unit) {
        val access = state.value.access
        if (access == null || replacement.size < 12 || !replacement.contentEquals(confirmation)) {
            current.fill('\u0000'); replacement.fill('\u0000'); confirmation.fill('\u0000')
            done("Use matching new passwords of at least 12 characters.")
            return
        }
        confirmation.fill('\u0000')
        manage {
            try {
                withContext(Dispatchers.IO) {
                    gate.withLock {
                        check(state.value.access === access)
                        val old = envelopes.read()
                        val key = try { crypto.unwrap(current, old) } finally { old.fill(0) }
                        try {
                            check(java.security.MessageDigest.isEqual(key, access.key))
                            val fresh = crypto.wrap(replacement, key)
                            try {
                                currentCoroutineContext().ensureActive()
                                check(state.value.access === access)
                                envelopes.write(fresh)
                            } finally { fresh.fill(0) }
                        } finally { key.fill(0) }
                    }
                }
                if (state.value.access === access) done("Master password changed. Existing entries and database key are unchanged.")
            } catch (_: Exception) {
                if (state.value.access === access) done("Password change failed. Your previous password envelope was retained.")
            } finally { current.fill('\u0000'); replacement.fill('\u0000') }
        }
    }

    fun prepareBiometricEnrollment(password: CharArray, done: (Cipher?) -> Unit) {
        val access = state.value.access
        manage {
            var cipher: Cipher? = null
            try {
                withContext(Dispatchers.IO) {
                    gate.withLock {
                        check(access != null && state.value.access === access)
                        val wrapped = envelopes.read()
                        val verified = try { crypto.unwrap(password, wrapped) } finally { wrapped.fill(0) }
                        try { check(java.security.MessageDigest.isEqual(verified, access.key)) }
                        finally { verified.fill(0) }
                        currentCoroutineContext().ensureActive()
                        check(state.value.access === access)
                        cipher = biometrics.encryptionCipher()
                    }
                }
                if (state.value.access === access && access != null && inForeground) {
                    biometricApproval = access.id
                    done(cipher)
                } else done(null)
            } catch (_: Exception) { done(null) }
            finally { password.fill('\u0000') }
        }
    }
    fun finishBiometricEnrollment(cipher: Cipher, done: (Boolean) -> Unit) {
        val access = state.value.access
        val approved = biometricApproval
        biometricApproval = null
        manage {
            try {
                withContext(Dispatchers.IO) {
                    gate.withLock {
                        check(access != null && approved == access.id && state.value.access === access && inForeground)
                        biometrics.save(cipher, access.key)
                    }
                }
                done(true)
            } catch (_: Exception) { done(false) }
        }
    }
    fun disableBiometric(done: (Boolean) -> Unit) {
        biometricApproval = null
        manage {
            val removed = try { withContext(Dispatchers.IO) { gate.withLock { biometrics.disable() } } }
                catch (_: Exception) { false }
            done(removed)
        }
    }
    fun unlockBiometric(cipher: Cipher) {
        if (state.value.stage !in listOf(VaultStage.LOCKED, VaultStage.RECOVERABLE_ERROR)) return
        val ticket = ++serial
        mutableState.value = VaultState(VaultStage.UNLOCKING)
        attempt = scope.launch {
            var result: VaultAccess? = null
            try {
                withContext(Dispatchers.IO) {
                    gate.withLock {
                        check(envelopes.exists() && activeDatabaseExists())
                        val key = biometrics.decrypt(cipher)
                        try {
                            val db = EncryptedDatabaseProvider.open(context, key)
                            result = VaultAccess(ticket, db, key, passwordAuthenticated = false)
                        } catch (failure: Exception) { key.fill(0); throw failure }
                    }
                }
                if (serial == ticket && inForeground && !deviceLocked()) {
                    mutableState.value = VaultState(VaultStage.UNLOCKED, result)
                    result = null
                } else if (serial == ticket) lock()
            } catch (_: Exception) {
                if (serial == ticket) mutableState.value = VaultState(VaultStage.RECOVERABLE_ERROR,
                    message = "Biometric unlock unavailable. Use your master password; re-enable biometrics in Security settings.")
            } finally {
                result?.let { abandoned -> abandoned.revoke(); withContext(NonCancellable + Dispatchers.IO) { abandoned.close() } }
            }
        }
    }
}
