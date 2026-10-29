package com.gem.keeppeer

import android.content.ClipData
import android.content.ClipboardManager
import android.security.keystore.KeyInfo
import androidx.test.platform.app.InstrumentationRegistry
import com.gem.keeppeer.data.local.EncryptedDatabaseProvider
import com.gem.keeppeer.domain.PasswordEntry
import com.gem.keeppeer.security.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.KeyStore
import javax.crypto.SecretKeyFactory

class VaultSecurityTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun main(block: () -> Unit) = instrumentation.runOnMainSync(block)
    private val password get() = "synthetic master password".toCharArray()
    private suspend fun unlocked(session: VaultSession) = withTimeout(120_000) {
        session.state.first { it.stage != VaultStage.UNLOCKING && it.stage != VaultStage.FIRST_RUN }
            .also { assertEquals(it.message, VaultStage.UNLOCKED, it.stage) }.access!!
    }
    private fun fixture(): Pair<SyntheticVaultContext, VaultSession> {
        val context = SyntheticVaultContext(instrumentation.targetContext)
        return context to syntheticOnMain { VaultSession(context, SensitiveClipboard(context), context.namespace, deviceLocked = { false }) }
    }
    private suspend fun setup(session: VaultSession): VaultAccess {
        main { session.foreground(); session.setup(password, password) }
        return unlocked(session)
    }

    @Test fun platformAndPortableKdfAgreeForUnicodeAndProductionCost() {
        val crypto = CryptoManager()
        val synthetic = "Synthetic 雲🙂 spaced master ".toCharArray()
        val salt = ByteArray(16) { it.toByte() }
        val portable = crypto.derivePortable(synthetic, salt, CryptoManager.ITERATIONS)
        val platform = crypto.derive(synthetic, salt, CryptoManager.ITERATIONS)
        assertArrayEquals(portable, platform)
        portable.fill(0); platform.fill(0); synthetic.fill('\u0000')
    }

    @Test fun keystoreEnvelopeIsDeviceBoundAndFailedReplacementPreservesOldPassword() {
        val context = SyntheticVaultContext(instrumentation.targetContext)
        val alias = context.namespace + ".storage.v1"
        val store = EnvelopeStore(context, alias)
        val crypto = CryptoManager()
        val key = crypto.randomKey()
        val old = crypto.wrap(password, key)
        try {
            store.initializeStorageKey()
            store.write(old)
            assertArrayEquals(key, crypto.unwrap(password, store.read()))
            val new = crypto.wrap("synthetic replacement".toCharArray(), key)
            try { store.write(new) { error("Synthetic failed-write injection") }; fail() }
            catch (_: IllegalStateException) {}
            assertArrayEquals(old, store.read())
            val raw = File(context.noBackupFilesDir, "vault-envelope.bin").readBytes()
            assertFalse(raw.toString(Charsets.ISO_8859_1).contains("synthetic master"))
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val info = SecretKeyFactory.getInstance("AES", "AndroidKeyStore")
                .getKeySpec(ks.getKey(alias, null) as javax.crypto.SecretKey, KeyInfo::class.java) as KeyInfo
            assertFalse(info.isUserAuthenticationRequired)
            // Removing this synthetic key cannot trigger key regeneration during read.
            ks.deleteEntry(alias)
            try { store.read(); fail() } catch (_: Exception) {}
            assertArrayEquals(raw, File(context.noBackupFilesDir, "vault-envelope.bin").readBytes())
            assertFalse(ks.containsAlias(alias))
        } finally { key.fill(0); old.fill(0) }
    }

    @Test fun passwordChangeRestartWrongPasswordAndStaleRepositoryRevocation() = runBlocking {
        val (context, session) = fixture()
        val access = setup(session)
        val id = access.repository.create(PasswordEntry(title = "Synthetic session", password = "synthetic entry"))
        val store = EnvelopeStore(context, context.namespace + ".storage.v1")
        val crypto = CryptoManager()
        val beforeEnvelope = store.read()
        val beforeKey = crypto.unwrap(password, beforeEnvelope)
        val changed = CompletableDeferred<String>()
        main { session.changePassword(password, "synthetic changed master".toCharArray(),
            "synthetic changed master".toCharArray()) { changed.complete(it) } }
        assertTrue(withTimeout(120_000) { changed.await() }.startsWith("Master password changed"))
        val after = store.read()
        assertFalse(beforeEnvelope.contentEquals(after))
        assertArrayEquals(beforeKey, crypto.unwrap("synthetic changed master".toCharArray(), after))
        main { session.lock() }
        assertNull(session.state.value.access)
        try { access.repository.getById(id); fail("Stale repository must be revoked") } catch (_: IllegalStateException) {}
        try { access.repository.create(PasswordEntry(title = "Synthetic stale write", password = "synthetic")); fail("Stale writes must be revoked") }
        catch (_: IllegalStateException) {}
        val restarted = syntheticOnMain { VaultSession(context, SensitiveClipboard(context), context.namespace, deviceLocked = { false }) }
        assertEquals(VaultStage.LOCKED, restarted.state.value.stage)
        main { restarted.foreground(); restarted.unlock(password) }
        withTimeout(120_000) { restarted.state.first { it.stage == VaultStage.RECOVERABLE_ERROR } }
        assertNull(restarted.state.value.access)
        main { restarted.unlock("synthetic changed master".toCharArray()) }
        assertEquals("synthetic entry", unlocked(restarted).repository.getById(id)?.password)
        main { restarted.lock() }
        beforeKey.fill(0)
    }

    @Test fun pendingCreationResumesButMissingActiveDatabaseNeverRecreates() = runBlocking {
        val (context, session) = fixture()
        val crypto = CryptoManager()
        val key = crypto.randomKey()
        val store = EnvelopeStore(context, context.namespace + ".storage.v1")
        store.initializeStorageKey()
        store.write(crypto.wrap(password, key), creationPending = true)
        val resumed = syntheticOnMain { VaultSession(context, SensitiveClipboard(context), context.namespace, deviceLocked = { false }) }
        main { resumed.foreground(); resumed.unlock(password) }
        unlocked(resumed)
        val resumedKey = crypto.unwrap(password, store.read())
        try { assertArrayEquals("Pending creation must retain its original generated key", key, resumedKey) }
        finally { resumedKey.fill(0) }
        assertFalse(store.readRecord().creationPending)
        main { resumed.lock() }
        // Wait until the revoked database has closed before simulating file loss.
        delay(500)
        assertTrue(context.deleteDatabase(EncryptedDatabaseProvider.VAULT_NAME))
        val missing = syntheticOnMain { VaultSession(context, SensitiveClipboard(context), context.namespace, deviceLocked = { false }) }
        main { missing.foreground(); missing.unlock(password) }
        withTimeout(120_000) { missing.state.first { it.stage == VaultStage.RECOVERABLE_ERROR } }
        assertFalse(context.getDatabasePath(EncryptedDatabaseProvider.VAULT_NAME).exists())
        assertTrue(store.exists())
        val truncated = context.getDatabasePath(EncryptedDatabaseProvider.VAULT_NAME)
        truncated.writeBytes(byteArrayOf())
        main { missing.unlock(password) }
        withTimeout(120_000) { missing.state.first { it.stage == VaultStage.RECOVERABLE_ERROR } }
        assertTrue(truncated.exists())
        assertEquals(0L, truncated.length())
        main { session.lock(); missing.lock() }
        key.fill(0)
    }

    @Test fun monotonicReturnEnforcesAllTimeoutOptionsAndDeviceLock() = runBlocking {
        val context = SyntheticVaultContext(instrumentation.targetContext)
        var elapsed = 10L
        var locked = false
        val session = syntheticOnMain {
            VaultSession(context, SensitiveClipboard(context), context.namespace,
                elapsedTime = { elapsed }, deviceLocked = { locked })
        }
        setup(session)
        main { session.background() }
        assertNull(session.state.value.access)
        for (timeout in listOf(LockTimeout.MINUTE, LockTimeout.FIVE_MINUTES)) {
            main { session.foreground(); session.unlock(password) }
            val access = unlocked(session)
            main { session.setTimeout(timeout); session.background() }
            elapsed += timeout.milliseconds - 1
            main { session.foreground() }
            assertSame(access, session.state.value.access)
            main { session.background() }
            elapsed += timeout.milliseconds
            main { session.foreground() }
            assertNull(session.state.value.access)
        }
        main { session.unlock(password) }
        unlocked(session)
        locked = true
        main { session.foreground() }
        assertNull(session.state.value.access)
        main { session.lock() }
    }

    @Test fun legacyPrototypeRequiresExplicitReset() = runBlocking {
        val context = SyntheticVaultContext(instrumentation.targetContext)
        val legacy = context.getDatabasePath(EncryptedDatabaseProvider.LEGACY_NAME)
        legacy.writeText("synthetic plaintext prototype")
        val session = syntheticOnMain { VaultSession(context, SensitiveClipboard(context), context.namespace, deviceLocked = { false }) }
        assertEquals(VaultStage.LEGACY_RESET, session.state.value.stage)
        assertTrue(legacy.exists())
        main { session.foreground(); session.setup(password, password) }
        assertTrue(legacy.exists())
        main { session.resetLegacy() }
        withTimeout(10_000) { session.state.first { it.stage == VaultStage.FIRST_RUN } }
        assertFalse(legacy.exists())
        main { session.lock() }
    }

    @Test fun clipboardReplacementDoesNotEraseUnrelatedContent() {
        val context = instrumentation.targetContext
        val (manager, clipboard) = syntheticOnMain {
            context.getSystemService(ClipboardManager::class.java) to SensitiveClipboard(context)
        }
        androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java).use {
            instrumentation.waitForIdleSync()
            main {
                assertTrue(clipboard.copy("synthetic clipboard secret"))
                assertTrue(manager.primaryClipDescription!!.extras!!.getBoolean("android.content.extra.IS_SENSITIVE"))
                manager.setPrimaryClip(ClipData.newPlainText("unrelated synthetic", "keep this synthetic text"))
                clipboard.clear()
                assertEquals("keep this synthetic text", manager.primaryClip!!.getItemAt(0).text.toString())
                if (android.os.Build.VERSION.SDK_INT >= 28) manager.clearPrimaryClip()
                else manager.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }
    }
}
