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

enum class VaultStage { CHECKING, FIRST_RUN, LEGACY_RESET, LOCKED, UNLOCKING, UNLOCKED, RECOVERABLE_ERROR, UNAVAILABLE }
data class VaultState(
    val stage: VaultStage = VaultStage.CHECKING,
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
    private val initialization: VaultInitialization = FileVaultInitialization(context, namespace),
) {
    private val crypto = CryptoManager()
    private val envelopes = EnvelopeStore(context, "$namespace.storage.v1")
    val biometrics = BiometricVault(context, "$namespace.biometric.v1")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val gate = Mutex()
    private val preferences = context.getSharedPreferences("lock-settings", Context.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(VaultState())
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
    private var biometricApproval: Pair<Long, Cipher>? = null
    fun cancelBiometricEnrollment(cipher: Cipher) {
        if (biometricApproval?.second === cipher) biometricApproval = null
    }
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
    init { refreshState() }

    /** Only called under the gate on IO; absence and an inspection failure are different results. */
    private fun inspectedState(): VaultState {
        val evidence = initialization.inspect()
        return when {
            evidence.passwordEnvelope -> VaultState(VaultStage.LOCKED)
            evidence.encryptedEvidence -> unavailable()
            evidence.legacy -> VaultState(VaultStage.LEGACY_RESET)
            else -> VaultState(VaultStage.FIRST_RUN)
        }
    }
    private fun unavailable() = VaultState(VaultStage.UNAVAILABLE, message =
        "Vault unavailable. Prior initialization or uncertain storage was detected, but required vault material is missing or could not be inspected. Existing material was preserved. Do not clear app data or create another vault.")
    private fun refreshState() {
        val ticket = serial
        mutableState.value = VaultState(VaultStage.CHECKING, generation = ticket)
        attempt = scope.launch {
            val checked = withContext(Dispatchers.IO) {
                gate.withLock {
                    try { inspectedState() }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { unavailable() }
                }
            }
            if (serial == ticket) mutableState.value = checked.copy(generation = ticket)
        }
    }
    private suspend fun authenticationError(message: String): VaultState = withContext(Dispatchers.IO) {
        gate.withLock {
            try {
                if (initialization.inspect().passwordEnvelope)
                    VaultState(VaultStage.RECOVERABLE_ERROR, message = message)
                else unavailable()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { unavailable() }
        }
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
        BiometricDiagnostics.record(BiometricStage.SESSION_BACKGROUND)
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
        BiometricDiagnostics.record(BiometricStage.SESSION_GENERATION)
        attempt?.cancel()
        attempt = null
        managementJobs.toList().forEach { it.cancel() }
        managementJobs.clear()
        biometricApproval = null
        clipboard.clear()
        timer?.cancel()
        val access = state.value.access
        access?.revoke()
        if (access != null) scope.launch(Dispatchers.IO) { gate.withLock { access.close() } }
        refreshState()
    }

    /** User must explicitly confirm the development reset in the UI. Never touches encrypted files. */
    fun resetLegacy() {
        if (state.value.stage != VaultStage.LEGACY_RESET || attempt?.isActive == true) return
        attempt = scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    gate.withLock {
                        val evidence = initialization.inspect()
                        check(evidence.legacy && !evidence.encryptedEvidence && !evidence.passwordEnvelope)
                        context.deleteDatabase(EncryptedDatabaseProvider.LEGACY_NAME)
                        check(!legacyExists())
                    }
                }
                refreshState()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                mutableState.value = unavailable().copy(generation = serial)
            }
        }
    }

    fun setup(password: CharArray, confirmation: CharArray) {
        if (state.value.stage != VaultStage.FIRST_RUN || attempt?.isActive == true) {
            password.fill('\u0000'); confirmation.fill('\u0000'); return
        }
        if (password.size < 12 || !password.contentEquals(confirmation)) {
            password.fill('\u0000'); confirmation.fill('\u0000')
            mutableState.value = VaultState(VaultStage.FIRST_RUN, message = "Use at least 12 characters and matching passwords.")
            return
        }
        confirmation.fill('\u0000')
        authenticate(password, setup = true)
    }
    fun unlock(password: CharArray) {
        if (state.value.stage !in listOf(VaultStage.LOCKED, VaultStage.RECOVERABLE_ERROR)) {
            password.fill('\u0000'); return
        }
        authenticate(password, setup = false)
    }
    private fun authenticate(password: CharArray, setup: Boolean) {
        val ticket = ++serial
        BiometricDiagnostics.record(BiometricStage.SESSION_GENERATION)
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
                                currentCoroutineContext().ensureActive()
                                check(initialization.inspect().fresh) { "Setup eligibility changed" }
                                // Durable intent precedes every key/envelope/database provisioning step.
                                initialization.start()
                                currentCoroutineContext().ensureActive()
                                key = crypto.randomKey()
                                // Commit recoverable metadata BEFORE creating the database.
                                // An interrupted first creation never silently regenerates the key.
                                envelopes.initializeStorageKey()
                                val wrapped = crypto.wrap(password, key)
                                try { envelopes.write(wrapped, creationPending = true) } finally { wrapped.fill(0) }
                            } else {
                                check(initialization.inspect().passwordEnvelope)
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
                            initialization.complete()
                            currentCoroutineContext().ensureActive()
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
                val failed = authenticationError(
                    "Could not unlock. Check your password. Damaged or missing device keys or vault files also prevent access. Existing data was preserved.")
                if (serial == ticket) mutableState.value = failed.copy(generation = ticket)
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

    fun prepareBiometricEnrollment(password: CharArray, done: (Cipher?, BiometricResult) -> Unit) {
        BiometricDiagnostics.record(BiometricStage.PASSWORD_INPUT)
        if (password.isEmpty()) {
            done(null, BiometricResult(BiometricOutcome.EMPTY_PASSWORD, BiometricStage.PASSWORD_INPUT))
            return
        }
        val access = state.value.access
        if (access == null || !inForeground) {
            password.fill('\u0000')
            done(null, BiometricResult(BiometricOutcome.SESSION_EXPIRED, BiometricStage.SESSION_CHECK))
            return
        }
        biometricApproval = null
        manage {
            var stage = BiometricStage.SESSION_CHECK
            try {
                val cipher = withContext(Dispatchers.IO) {
                    gate.withLock {
                        check(state.value.access === access)
                        stage = BiometricStage.PASSWORD_ENVELOPE_READ
                        val wrapped = envelopes.read()
                        stage = BiometricStage.PASSWORD_VERIFY
                        BiometricDiagnostics.record(stage)
                        val verified = try { crypto.unwrap(password, wrapped) } finally { wrapped.fill(0) }
                        try { check(java.security.MessageDigest.isEqual(verified, access.key)) }
                        finally { verified.fill(0) }
                        currentCoroutineContext().ensureActive()
                        stage = BiometricStage.SESSION_CHECK
                        check(state.value.access === access)
                        stage = BiometricStage.AVAILABILITY
                        val code = biometrics.availabilityCode()
                        BiometricDiagnostics.record(stage, code = code)
                        if (code != androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS)
                            return@withLock null
                        stage = BiometricStage.KEY_GENERATE
                        biometrics.encryptionCipher()
                    }
                }
                if (state.value.access !== access || !inForeground) {
                    done(null, BiometricResult(BiometricOutcome.SESSION_EXPIRED, BiometricStage.SESSION_CHECK))
                } else if (cipher == null) {
                    done(null, BiometricResult(BiometricOutcome.UNAVAILABLE, BiometricStage.AVAILABILITY))
                } else {
                    biometricApproval = access.id to cipher
                    done(cipher, BiometricResult.ok())
                }
            } catch (error: Exception) {
                val result = if (stage == BiometricStage.SESSION_CHECK || error is CancellationException) {
                    BiometricDiagnostics.record(BiometricStage.SESSION_CANCELLED, error)
                    BiometricResult(BiometricOutcome.SESSION_EXPIRED, BiometricStage.SESSION_CHECK)
                } else BiometricDiagnostics.failure(stage, error)
                done(null, result)
                if (error is CancellationException) throw error
            } finally { password.fill('\u0000') }
        }
    }
    fun finishBiometricEnrollment(cipher: Cipher, done: (BiometricResult) -> Unit) {
        val access = state.value.access
        val approved = biometricApproval
        biometricApproval = null
        manage {
            var stage = BiometricStage.APPROVAL_CHECK
            try {
                withContext(Dispatchers.IO) {
                    gate.withLock {
                        BiometricDiagnostics.record(stage)
                        check(access != null && approved?.first == access.id && approved.second === cipher)
                        stage = BiometricStage.SESSION_CHECK
                        BiometricDiagnostics.record(stage)
                        check(state.value.access === access)
                        stage = BiometricStage.FOREGROUND_CHECK
                        BiometricDiagnostics.record(stage)
                        check(inForeground && !deviceLocked())
                        currentCoroutineContext().ensureActive()
                        stage = BiometricStage.ENCRYPT_FINAL
                        biometrics.save(cipher, access.key)
                    }
                }
                if (state.value.access === access && inForeground) {
                    BiometricDiagnostics.record(BiometricStage.COMPLETE)
                    done(BiometricResult.ok())
                } else done(BiometricResult(BiometricOutcome.SESSION_EXPIRED, BiometricStage.SESSION_CHECK))
            } catch (error: Exception) {
                val result = if (stage in listOf(BiometricStage.APPROVAL_CHECK, BiometricStage.SESSION_CHECK,
                        BiometricStage.FOREGROUND_CHECK) || error is CancellationException) {
                    BiometricDiagnostics.record(stage, error)
                    BiometricResult(BiometricOutcome.SESSION_EXPIRED, stage)
                } else BiometricDiagnostics.failure(stage, error)
                done(result)
                if (error is CancellationException) throw error
            }
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
    fun unlockBiometric(cipher: Cipher, done: (BiometricResult) -> Unit = {}) {
        if (state.value.stage !in listOf(VaultStage.LOCKED, VaultStage.RECOVERABLE_ERROR)) {
            done(BiometricResult(BiometricOutcome.SESSION_EXPIRED, BiometricStage.SESSION_CHECK)); return
        }
        val ticket = ++serial
        BiometricDiagnostics.record(BiometricStage.SESSION_GENERATION)
        mutableState.value = VaultState(VaultStage.UNLOCKING)
        attempt = scope.launch {
            var result: VaultAccess? = null
            try {
                withContext(Dispatchers.IO) {
                    gate.withLock {
                        check(initialization.inspect().passwordEnvelope && activeDatabaseExists())
                        val key = biometrics.decrypt(cipher)
                        try {
                            val db = BiometricDiagnostics.at(BiometricStage.DATABASE_OPEN) {
                                EncryptedDatabaseProvider.open(context, key)
                            }
                            try {
                                initialization.complete()
                                currentCoroutineContext().ensureActive()
                                result = VaultAccess(ticket, db, key, passwordAuthenticated = false)
                            } catch (failure: Exception) { db.close(); throw failure }
                        } catch (failure: Exception) { key.fill(0); throw failure }
                    }
                }
                if (serial == ticket && inForeground && !deviceLocked()) {
                    mutableState.value = VaultState(VaultStage.UNLOCKED, result)
                    result = null
                    BiometricDiagnostics.record(BiometricStage.COMPLETE)
                    done(BiometricResult.ok())
                } else {
                    if (serial == ticket) lock()
                    done(BiometricResult(BiometricOutcome.SESSION_EXPIRED, BiometricStage.SESSION_CHECK))
                }
            } catch (error: Exception) {
                val failure = BiometricDiagnostics.failure(BiometricStage.DECRYPT_FINAL, error)
                if (error !is CancellationException) {
                    val failed = authenticationError(failure.message())
                    if (serial == ticket) mutableState.value = failed.copy(generation = ticket)
                }
                done(failure)
                if (error is CancellationException) throw error
            } finally {
                result?.let { abandoned -> abandoned.revoke(); withContext(NonCancellable + Dispatchers.IO) { abandoned.close() } }
            }
        }
    }
}
