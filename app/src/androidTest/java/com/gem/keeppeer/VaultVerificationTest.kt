package com.gem.keeppeer

import android.content.ClipboardManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import com.gem.keeppeer.domain.PasswordEntry
import com.gem.keeppeer.data.local.EncryptedDatabaseProvider
import com.gem.keeppeer.ui.lock.VaultRoot
import com.gem.keeppeer.ui.theme.KeeppeerTheme
import com.gem.keeppeer.security.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.security.KeyStore

/** Additional acceptance checks; all vault files and aliases are disposable synthetic fixtures. */
class VaultVerificationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val password get() = "synthetic test master".toCharArray()
    private fun main(block: () -> Unit) = instrumentation.runOnMainSync(block)
    private fun metric(value: String) = instrumentation.sendStatus(2, Bundle().apply {
        putString("stream", "\nKeeppeerVerification API=${Build.VERSION.SDK_INT} $value\n")
    })
    private suspend fun opened(session: VaultSession): VaultAccess = withTimeout(120_000) {
        session.state.first { it.stage == VaultStage.UNLOCKED || it.stage == VaultStage.RECOVERABLE_ERROR }
            .also { assertEquals(it.message, VaultStage.UNLOCKED, it.stage) }.access!!
    }
    private fun appSession(): VaultSession {
        val session = (compose.activity.application as KeeppeerApplication).container.session
        compose.runOnUiThread {
            session.foreground()
            if (session.state.value.stage == VaultStage.FIRST_RUN) session.setup(password, password)
            else if (session.state.value.access == null) session.unlock(password)
        }
        compose.waitUntil(120_000) { session.state.value.access != null }
        return session
    }

    @Test fun cancelLegacyResetPreservesDataAndSetupRequiresMatchingStrongPassword() {
        val context = SyntheticVaultContext(instrumentation.targetContext)
        val legacy = context.getDatabasePath(EncryptedDatabaseProvider.LEGACY_NAME)
        val contents = "disposable synthetic Phase 1 data"
        legacy.writeText(contents)
        val session = VaultSession(context, SensitiveClipboard(context), context.namespace, deviceLocked = { false })
        try {
            compose.runOnUiThread {
                session.foreground()
                compose.activity.setContent {
                    KeeppeerTheme { VaultRoot(session) { _, _, _ -> error("No biometric prompt expected") } }
                }
            }
            compose.onNodeWithText("Reset synthetic development data…").performClick()
            compose.onNodeWithText("Cancel").performClick()
            assertEquals(contents, legacy.readText())
            assertEquals(VaultStage.LEGACY_RESET, session.state.value.stage)
            assertFalse(context.getDatabasePath(EncryptedDatabaseProvider.VAULT_NAME).exists())
            compose.onNodeWithText("Reset synthetic development data…").performClick()
            compose.onNodeWithText("Delete synthetic data").performClick()
            compose.waitUntil(10_000) { session.state.value.stage == VaultStage.FIRST_RUN }
            assertFalse(legacy.exists())
            compose.onNodeWithText("Master password").performTextInput("short")
            compose.onNodeWithText("Confirm master password").performTextInput("short")
            compose.onNodeWithText("Create vault").performScrollTo().performClick()
            compose.onNodeWithText("Use at least 12 characters and matching passwords.")
                .performScrollTo().assertIsDisplayed()
            assertFalse(context.getDatabasePath(EncryptedDatabaseProvider.VAULT_NAME).exists())
            compose.onNodeWithText("Master password").performTextInput("synthetic test master")
            compose.onNodeWithText("Confirm master password").performTextInput("synthetic mismatch master")
            compose.onNodeWithText("Create vault").performScrollTo().performClick()
            assertFalse(context.getDatabasePath(EncryptedDatabaseProvider.VAULT_NAME).exists())
            compose.onNodeWithText("Master password").performTextInput("synthetic test master")
            compose.onNodeWithText("Confirm master password").performTextInput("synthetic test master")
            compose.onNodeWithText("Create vault").performScrollTo().performClick()
            compose.waitUntil(120_000) { session.state.value.access != null }
            assertTrue(context.getDatabasePath(EncryptedDatabaseProvider.VAULT_NAME).exists())
        } finally { main { session.lock() } }
    }

    @Test fun productionKdfAgreementAndMainLooperProgress() = runBlocking {
        val crypto = CryptoManager()
        val vector = crypto.derivePortable("password".toCharArray(), "salt".toByteArray(), 1)
        try {
            assertEquals("120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b",
                vector.joinToString("") { "%02x".format(it) })
        } finally { vector.fill(0) }
        var ticks = 0
        var largestGap = 0L
        var lastTick = SystemClock.elapsedRealtime()
        val handler = Handler(Looper.getMainLooper())
        val heartbeat = object : Runnable {
            override fun run() {
                val now = SystemClock.elapsedRealtime()
                largestGap = maxOf(largestGap, now - lastTick)
                lastTick = now
                ticks++
                handler.postDelayed(this, 16)
            }
        }
        val unicode = "Synthetic 雲🙂 spaced master ".toCharArray()
        val salt = ByteArray(16) { it.toByte() }
        var portable: ByteArray? = null
        var selected: ByteArray? = null
        main { handler.post(heartbeat) }
        try {
            val timings = withContext(Dispatchers.IO) {
                var start = SystemClock.elapsedRealtime()
                portable = crypto.derivePortable(unicode, salt, CryptoManager.ITERATIONS)
                val portableMs = SystemClock.elapsedRealtime() - start
                start = SystemClock.elapsedRealtime()
                selected = crypto.derive(unicode, salt, CryptoManager.ITERATIONS)
                portableMs to (SystemClock.elapsedRealtime() - start)
            }
            main {
                handler.removeCallbacks(heartbeat)
                assertTrue("Main looper must keep processing while the KDF runs off main", ticks > 1)
                metric("kdfIterations=${CryptoManager.ITERATIONS} portableMs=${timings.first} selectedMs=${timings.second} mainTicks=$ticks maxMainGapMs=$largestGap selectedBackend=${if (Build.VERSION.SDK_INT >= 26) "platform" else "portable"}")
            }
            assertArrayEquals(portable, selected)
        } finally {
            main { handler.removeCallbacks(heartbeat) }
            unicode.fill('\u0000'); portable?.fill(0); selected?.fill(0)
        }
    }

    @Test fun malformedEnvelopesAndMissingBiometricKeyPreservePasswordVault() = runBlocking {
        val context = SyntheticVaultContext(instrumentation.targetContext)
        val session = VaultSession(context, SensitiveClipboard(context), context.namespace, deviceLocked = { false })
        main { session.foreground(); session.setup(password, password) }
        val access = opened(session)
        val id = access.repository.create(PasswordEntry(title = "Synthetic preservation", password = "synthetic entry"))
        val outer = AtomicEnvelopeFile(File(context.noBackupFilesDir, "vault-envelope.bin"))
        val original = outer.read()
        val store = EnvelopeStore(context, context.namespace + ".storage.v1")
        val aliases = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val biometricAlias = context.namespace + ".biometric.v1"
        val biometricFile = AtomicEnvelopeFile(File(context.noBackupFilesDir, "biometric-envelope.bin"))
        // A correctly sized wrapper with no key must fail without creating a replacement key.
        biometricFile.write(ByteArray(61).also { it[0] = 1 })
        try {
            assertFalse(aliases.containsAlias(biometricAlias))
            var rejected = false
            try { session.biometrics.decryptionCipher() } catch (_: Exception) { rejected = true }
            assertTrue(rejected)
            assertFalse(aliases.containsAlias(biometricAlias))
            assertArrayEquals(original, outer.read())
            assertEquals("synthetic entry", access.repository.getById(id)?.password)
            main { session.lock() }
            // Malformed outer metadata/tag must preserve the exact stored bytes and deny access.
            for (damaged in listOf(original.copyOf(10),
                original.copyOf().also { it[3] = 2 },
                original.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() })) {
                outer.write(damaged)
                main { session.unlock(password) }
                withTimeout(120_000) { session.state.first { it.stage == VaultStage.RECOVERABLE_ERROR } }
                assertNull(session.state.value.access)
                assertArrayEquals(damaged, outer.read())
                assertTrue(aliases.containsAlias(context.namespace + ".storage.v1"))
            }
            outer.write(original)
            main { session.unlock(password) }
            assertEquals("synthetic entry", opened(session).repository.getById(id)?.password)
            assertArrayEquals(original, outer.read())
            assertFalse(store.readRecord().creationPending)
        } finally { main { session.lock() }; original.fill(0) }
    }

    @Test fun keyboardSelectionCopyExpiresAndBackgroundReturnClearsOwnedClip() = runBlocking {
        val session = appSession()
        val manager = compose.activity.getSystemService(ClipboardManager::class.java)
        compose.onNodeWithText("Add").performClick()
        val text = "synthetic selected clipboard"
        compose.onNodeWithText("Notes (optional)").performScrollTo().performTextInput(text)
        compose.onNodeWithText("Notes (optional)").performTextInputSelection(TextRange(0, text.length))
        compose.onNodeWithText("Notes (optional)").performKeyInput {
            keyDown(Key.CtrlLeft); pressKey(Key.C); keyUp(Key.CtrlLeft)
        }
        var label = ""
        main {
            label = manager.primaryClipDescription?.label?.toString().orEmpty()
            assertTrue("Keyboard selection copy must use the central clipboard boundary", label.startsWith("Keeppeer:"))
            assertTrue(manager.primaryClipDescription!!.extras!!.getBoolean("android.content.extra.IS_SENSITIVE"))
        }
        val copiedAt = SystemClock.elapsedRealtime()
        try {
            delay(1_000)
            main { assertEquals(label, manager.primaryClipDescription?.label?.toString()) }
            var owned = true
            withTimeout(35_000) {
                while (owned) {
                    delay(100)
                    main { owned = manager.primaryClipDescription?.label?.toString() == label }
                }
            }
            val elapsed = SystemClock.elapsedRealtime() - copiedAt
            assertTrue("Clip should expire at approximately 30 seconds", elapsed in 28_000..35_000)
            metric("selectionKeyboardCopyExpiryMs=$elapsed")
            main { assertTrue(session.clipboard.copy("synthetic background clip")) }
            compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            main {
                session.foreground()
                assertFalse(manager.primaryClipDescription?.label?.toString()?.startsWith("Keeppeer:") == true)
            }
            compose.onNodeWithText("Keeppeer locked").assertIsDisplayed()
        } finally { main { session.clipboard.clear(); session.lock() } }
        Unit
    }
}
