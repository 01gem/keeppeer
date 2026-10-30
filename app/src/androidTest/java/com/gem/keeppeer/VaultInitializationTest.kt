package com.gem.keeppeer

import android.os.Looper
import androidx.test.platform.app.InstrumentationRegistry
import com.gem.keeppeer.data.local.EncryptedDatabaseProvider
import com.gem.keeppeer.domain.PasswordEntry
import com.gem.keeppeer.security.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.IOException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Actual Android files/AtomicFile/Keystore/SQLCipher; all contexts and aliases are new synthetic UUIDs. */
class VaultInitializationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val password get() = "synthetic guard master".toCharArray()
    private fun context() = SyntheticVaultContext(instrumentation.targetContext)
    private fun aliases() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private fun alias(context: SyntheticVaultContext) = context.namespace + ".storage.v1"
    private fun envelope(context: SyntheticVaultContext) = File(context.noBackupFilesDir, "vault-envelope.bin")
    private fun marker(context: SyntheticVaultContext) = File(context.noBackupFilesDir, FileVaultInitialization.MARKER_NAME)
    private fun session(context: SyntheticVaultContext, guard: VaultInitialization = FileVaultInitialization(context, context.namespace)) =
        syntheticOnMain { VaultSession(context, SensitiveClipboard(context), context.namespace,
            deviceLocked = { false }, initialization = guard) }
    private fun main(block: () -> Unit) = instrumentation.runOnMainSync(block)
    private suspend fun terminal(session: VaultSession): VaultState = withTimeout(120_000) {
        session.state.first { it.stage !in listOf(VaultStage.CHECKING, VaultStage.UNLOCKING) }
    }
    private suspend fun setup(session: VaultSession): VaultAccess {
        assertEquals(VaultStage.FIRST_RUN, awaitVaultInspection(session).stage)
        main { session.foreground(); session.setup(password, password) }
        val opened = terminal(session)
        assertEquals(opened.message, VaultStage.UNLOCKED, opened.stage)
        return requireNotNull(opened.access)
    }
    private suspend fun lock(session: VaultSession, access: VaultAccess? = session.state.value.access) {
        main { session.lock() }
        awaitVaultInspection(session)
        if (access != null) withTimeout(10_000) { while (access.database.isOpen) delay(20) }
    }
    private fun rejectSetup(session: VaultSession) {
        val input = password
        val confirm = password
        main { session.setup(input, confirm) }
        assertTrue(input.all { it == '\u0000' })
        assertTrue(confirm.all { it == '\u0000' })
        assertNull(session.state.value.access)
    }

    @Test fun freshSetupRestartAndMarkerlessAuthenticatedUpgradePreserveKeyAndEntry() = runBlocking {
        val context = context()
        val session = session(context)
        val access = setup(session)
        val key = access.key.copyOf()
        val id = access.repository.create(PasswordEntry(title = "Synthetic guard", password = "synthetic retained"))
        val outer = envelope(context).readBytes()
        assertTrue(marker(context).readText().endsWith("COMPLETE\n"))
        lock(session, access)
        // Model a pre-guard installation by removing only this synthetic marker.
        assertTrue(marker(context).delete())
        val restarted = session(context)
        assertEquals(VaultStage.LOCKED, awaitVaultInspection(restarted).stage)
        assertFalse(marker(context).exists()) // Inspection must not provision/backfill anything.
        main { restarted.foreground(); restarted.unlock(password) }
        val opened = terminal(restarted).access!!
        assertArrayEquals(key, opened.key)
        assertEquals("synthetic retained", opened.repository.getById(id)?.password)
        assertArrayEquals(outer, envelope(context).readBytes())
        assertTrue(marker(context).readText().endsWith("COMPLETE\n"))
        lock(restarted, opened)
        val cold = session(context)
        assertEquals(VaultStage.LOCKED, awaitVaultInspection(cold).stage)
        main { cold.foreground(); cold.unlock(password) }
        assertEquals("synthetic retained", terminal(cold).access!!.repository.getById(id)?.password)
        lock(cold)
        key.fill(0)
    }

    @Test fun lossOfBothNormalFilesRefusesColdAndDirectSetupWithoutReplacingExistingKey() = runBlocking {
        val context = context()
        val session = session(context)
        val access = setup(session)
        lock(session, access)
        val retainedMarker = marker(context).readBytes()
        // A challenge proves the existing non-exportable storage key was not replaced.
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, aliases().getKey(alias(context), null) as SecretKey)
        }
        val challenge = cipher.doFinal(byteArrayOf(7, 8, 9))
        assertTrue(context.deleteDatabase(EncryptedDatabaseProvider.VAULT_NAME))
        assertTrue(envelope(context).delete())
        val cold = session(context)
        assertEquals(VaultStage.UNAVAILABLE, awaitVaultInspection(cold).stage)
        rejectSetup(cold)
        assertFalse(envelope(context).exists())
        assertFalse(context.getDatabasePath(EncryptedDatabaseProvider.VAULT_NAME).exists())
        assertArrayEquals(retainedMarker, marker(context).readBytes())
        val decrypted = Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.DECRYPT_MODE, aliases().getKey(alias(context), null) as SecretKey,
                GCMParameterSpec(128, cipher.iv))
            doFinal(challenge)
        }
        assertArrayEquals(byteArrayOf(7, 8, 9), decrypted)
        lock(cold)
    }

    @Test fun existingAliasAloneBlocksSetupAndOtherNamespacesRemainFresh() = runBlocking {
        val old = context()
        EnvelopeStore(old, alias(old)).initializeStorageKey()
        val oldSession = session(old)
        assertEquals(VaultStage.UNAVAILABLE, awaitVaultInspection(oldSession).stage)
        rejectSetup(oldSession)
        assertTrue(aliases().containsAlias(alias(old)))
        assertFalse(marker(old).exists())
        val fresh = context()
        val freshSession = session(fresh)
        assertEquals(VaultStage.FIRST_RUN, awaitVaultInspection(freshSession).stage)
        assertFalse(aliases().containsAlias(alias(fresh)))
        lock(oldSession); lock(freshSession)
    }

    @Test fun residualSidecarsAtomicSiblingsAndBiometricMaterialAreNeverFresh() = runBlocking {
        for (name in listOf("vault-envelope.bin.new", "biometric-envelope.bin.bak",
                FileVaultInitialization.MARKER_NAME + ".new", "vault-envelope.bin.bak")) {
            val context = context()
            val residual = File(context.noBackupFilesDir, name)
            val bytes = byteArrayOf(3, 1, 4)
            residual.writeBytes(bytes)
            val session = session(context)
            val stage = awaitVaultInspection(session).stage
            // A .bak password envelope retains the password path; other residuals cannot unlock.
            assertEquals(if (name == "vault-envelope.bin.bak") VaultStage.LOCKED else VaultStage.UNAVAILABLE, stage)
            rejectSetup(session)
            assertArrayEquals(bytes, residual.readBytes())
            assertFalse(aliases().containsAlias(alias(context)))
            lock(session)
        }
        val context = context()
        val sidecar = File(context.getDatabasePath(EncryptedDatabaseProvider.VAULT_NAME).path + "-wal")
        sidecar.writeBytes(byteArrayOf(2, 7))
        val session = session(context)
        assertEquals(VaultStage.UNAVAILABLE, awaitVaultInspection(session).stage)
        rejectSetup(session)
        assertArrayEquals(byteArrayOf(2, 7), sidecar.readBytes())
        assertFalse(aliases().containsAlias(alias(context)))
        lock(session)
    }

    @Test fun unreadableEvidenceAndKeystoreInspectionFailureAreUnavailableNotAbsent() = runBlocking {
        val context = context()
        val unreadable = File(context.noBackupFilesDir, FileVaultInitialization.MARKER_NAME)
        unreadable.writeText("synthetic unreadable evidence")
        assertTrue(unreadable.setReadable(false, false))
        try {
            val session = session(context)
            assertEquals(VaultStage.UNAVAILABLE, awaitVaultInspection(session).stage)
            rejectSetup(session)
            assertFalse(aliases().containsAlias(alias(context)))
            lock(session)
        } finally { assertTrue(unreadable.setReadable(true, true)) }
        val other = context()
        val session = session(other, FileVaultInitialization(other, other.namespace,
            aliasExists = { throw IOException("Synthetic evidence inspection failure") }))
        assertEquals(VaultStage.UNAVAILABLE, awaitVaultInspection(session).stage)
        rejectSetup(session)
        assertFalse(aliases().containsAlias(alias(other)))
        assertFalse(envelope(other).exists())
        lock(session)
    }

    @Test fun initialGuardWriteFailureCannotProvisionKeysEnvelopeOrDatabase() = runBlocking {
        val context = context()
        val guard = FileVaultInitialization(context, context.namespace, beforeCommit = {
            assertNotSame(Looper.getMainLooper(), Looper.myLooper())
            if (it == InitializationPhase.STARTED) throw IOException("Synthetic start-write failure")
        })
        val session = session(context, guard)
        assertEquals(VaultStage.FIRST_RUN, awaitVaultInspection(session).stage)
        val secret = password
        val confirmation = password
        main { session.foreground(); session.setup(secret, confirmation) }
        assertEquals(VaultStage.UNAVAILABLE, terminal(session).stage)
        assertTrue(secret.all { it == '\u0000' } && confirmation.all { it == '\u0000' })
        assertFalse(aliases().containsAlias(alias(context)))
        assertFalse(envelope(context).exists())
        assertFalse(context.getDatabasePath(EncryptedDatabaseProvider.VAULT_NAME).exists())
        assertFalse(marker(context).exists())
        lock(session)
    }

    @Test fun startedIntentSurvivesInterruptedProvisioningAndNeverRegenerates() = runBlocking {
        for (provisionKey in listOf(false, true)) {
            val context = context()
            FileVaultInitialization(context, context.namespace).start()
            if (provisionKey) EnvelopeStore(context, alias(context)).initializeStorageKey()
            val bytes = marker(context).readBytes()
            val session = session(context)
            assertEquals(VaultStage.UNAVAILABLE, awaitVaultInspection(session).stage)
            rejectSetup(session)
            assertArrayEquals(bytes, marker(context).readBytes())
            assertEquals(provisionKey, aliases().containsAlias(alias(context)))
            assertFalse(envelope(context).exists())
            assertFalse(context.getDatabasePath(EncryptedDatabaseProvider.VAULT_NAME).exists())
            lock(session)
        }
    }

    @Test fun startedIntentWithUsablePendingEnvelopeResumesOriginalKey() = runBlocking {
        val context = context()
        FileVaultInitialization(context, context.namespace).start()
        val key = CryptoManager().randomKey()
        val wrapped = CryptoManager().wrap(password, key)
        val store = EnvelopeStore(context, alias(context))
        store.initializeStorageKey()
        try { store.write(wrapped, creationPending = true) } finally { wrapped.fill(0) }
        val session = session(context)
        assertEquals(VaultStage.LOCKED, awaitVaultInspection(session).stage)
        main { session.foreground(); session.unlock(password) }
        val access = terminal(session).access!!
        assertArrayEquals(key, access.key)
        assertFalse(store.readRecord().creationPending)
        assertTrue(marker(context).readText().endsWith("COMPLETE\n"))
        lock(session, access)
        key.fill(0)
    }

    @Test fun finalMarkerWriteFailureRetainsVaultAndRequiresAuthenticatedRetry() = runBlocking {
        val context = context()
        val guard = FileVaultInitialization(context, context.namespace, beforeCommit = {
            if (it == InitializationPhase.COMPLETE) throw IOException("Synthetic completion-write failure")
        })
        val session = session(context, guard)
        assertEquals(VaultStage.FIRST_RUN, awaitVaultInspection(session).stage)
        main { session.foreground(); session.setup(password, password) }
        assertEquals(VaultStage.RECOVERABLE_ERROR, terminal(session).stage)
        assertNull(session.state.value.access)
        assertTrue(marker(context).readText().endsWith("STARTED\n"))
        assertTrue(context.getDatabasePath(EncryptedDatabaseProvider.VAULT_NAME).length() >= 4096)
        val outer = envelope(context).readBytes()
        val store = EnvelopeStore(context, alias(context))
        val wrapped = store.read()
        val originalKey = try { CryptoManager().unwrap(password, wrapped) } finally { wrapped.fill(0) }
        lock(session)
        val retry = session(context)
        assertEquals(VaultStage.LOCKED, awaitVaultInspection(retry).stage)
        main { retry.foreground(); retry.unlock(password) }
        val access = terminal(retry).access!!
        assertArrayEquals(originalKey, access.key)
        assertArrayEquals(outer, envelope(context).readBytes())
        assertTrue(marker(context).readText().endsWith("COMPLETE\n"))
        lock(retry, access)
        originalKey.fill(0)
    }

    @Test fun staleFreshUiEligibilityIsRecheckedBeforeAnyProvisioning() = runBlocking {
        val context = context()
        val session = session(context)
        assertEquals(VaultStage.FIRST_RUN, awaitVaultInspection(session).stage)
        val residual = File(context.getDatabasePath(EncryptedDatabaseProvider.VAULT_NAME).path + "-journal")
        residual.writeBytes(byteArrayOf(6, 2, 6))
        val secret = password
        val confirmation = password
        main { session.foreground(); session.setup(secret, confirmation) }
        assertEquals(VaultStage.UNAVAILABLE, terminal(session).stage)
        assertTrue(secret.all { it == '\u0000' } && confirmation.all { it == '\u0000' })
        assertFalse(aliases().containsAlias(alias(context)))
        assertFalse(envelope(context).exists())
        assertFalse(marker(context).exists())
        assertArrayEquals(byteArrayOf(6, 2, 6), residual.readBytes())
        rejectSetup(session)
        lock(session)
    }
}
