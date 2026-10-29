package com.gem.keeppeer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.gem.keeppeer.security.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.GCMParameterSpec
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/** SyntheticTestRunner isolates every file and alias from the installed user's vault. */
class BiometricEnrollmentTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val password get() = "synthetic test master".toCharArray()
    private fun setup(): VaultSession {
        val session = (compose.activity.application as KeeppeerApplication).container.session
        compose.runOnUiThread {
            session.foreground()
            if (session.state.value.stage == VaultStage.FIRST_RUN) session.setup(password, password)
            else if (session.state.value.access == null) session.unlock(password)
        }
        compose.waitUntil(120_000) { session.state.value.access != null }
        return session
    }
    @Test fun emptyWrongAndUnapprovedEnrollmentPreservePasswordAccess() = runBlocking {
        val session = setup()
        val originalKey = requireNotNull(session.state.value.access).key.copyOf()
        suspend fun prepare(input: CharArray): BiometricResult {
            val result = CompletableDeferred<BiometricResult>()
            compose.runOnUiThread {
                session.prepareBiometricEnrollment(input) { cipher, outcome ->
                    assertNull(cipher)
                    result.complete(outcome)
                }
            }
            return withTimeout(120_000) { result.await() }
        }
        assertEquals(BiometricOutcome.EMPTY_PASSWORD, prepare(charArrayOf()).outcome)
        val wrong = "synthetic wrong password".toCharArray()
        assertEquals(BiometricOutcome.WRONG_PASSWORD, prepare(wrong).outcome)
        compose.runOnIdle { assertTrue(wrong.all { it == '\u0000' }) }
        val rejected = CompletableDeferred<BiometricResult>()
        compose.runOnUiThread {
            session.finishBiometricEnrollment(Cipher.getInstance("AES/GCM/NoPadding")) { rejected.complete(it) }
        }
        assertEquals(BiometricOutcome.SESSION_EXPIRED, withTimeout(120_000) { rejected.await() }.outcome)
        assertFalse(session.biometrics.enabled())
        compose.runOnUiThread { session.lock(); session.unlock(password) }
        compose.waitUntil(120_000) { session.state.value.access != null }
        try { assertArrayEquals(originalKey, requireNotNull(session.state.value.access).key) }
        finally { originalKey.fill(0) }
    }
    /** Real AES-GCM format regression; does not simulate biometric authorization. */
    @Test fun postAuthenticationAadPreservesSingleApplicationAndV1Compatibility() {
        val context = SyntheticVaultContext(InstrumentationRegistry.getInstrumentation().targetContext)
        val vault = BiometricVault(context, context.namespace + ".biometric.v1")
        val wrapping = ByteArray(32).also(java.security.SecureRandom()::nextBytes)
        val key = ByteArray(32).also(java.security.SecureRandom()::nextBytes)
        val secret = SecretKeySpec(wrapping, "AES")
        val aad = "Keeppeer biometric vault key v1".toByteArray(Charsets.UTF_8)
        val file = AtomicEnvelopeFile(File(context.noBackupFilesDir, "biometric-envelope.bin"))
        fun decryptor(bytes: ByteArray) = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, secret, GCMParameterSpec(128, bytes.copyOfRange(1, 13)))
        }
        try {
            val encrypt = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, secret) }
            vault.save(encrypt, key) // AAD belongs in the finalization path, exactly once.
            val bytes = file.read()
            assertEquals(61, bytes.size)
            assertEquals(1.toByte(), bytes[0])
            val reference = decryptor(bytes).apply { updateAAD(aad) }.doFinal(bytes, 13, 48)
            assertArrayEquals(key, reference)
            reference.fill(0)

            // A wrapper produced with the original v1 AAD remains readable.
            val legacy = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.ENCRYPT_MODE, secret)
                updateAAD(aad)
            }
            file.write(byteArrayOf(1) + legacy.iv + legacy.doFinal(key))
            val opened = vault.decrypt(decryptor(file.read()))
            assertArrayEquals(key, opened)
            opened.fill(0)
            val tampered = file.read().also { it[60] = (it[60].toInt() xor 1).toByte() }
            file.write(tampered)
            try { vault.decrypt(decryptor(tampered)); fail("Tampered biometric wrapper must fail") }
            catch (failure: BiometricFailure) { assertEquals(BiometricStage.DECRYPT_FINAL, failure.stage) }
        } finally { file.delete(); wrapping.fill(0); key.fill(0) }
    }
    @Test fun enrollmentDialogRejectsBlankAndDoesNotUsePasswordChangeFields() {
        val session = setup()
        if (android.os.Build.VERSION.SDK_INT in 24..27) {
            // The platform service is absent from compileSdk 37; inspect only its public legacy
            // capability methods on the old device. No key/template data or auth callback is read.
            val native = compose.activity.getSystemService("fingerprint")
            val enrolled = native != null &&
                native.javaClass.getMethod("isHardwareDetected").invoke(native) == true &&
                native.javaClass.getMethod("hasEnrolledFingerprints").invoke(native) == true
            if (enrolled) assertTrue("Enrolled legacy fingerprint must remain available through Biometric compatibility",
                session.biometrics.available())
        }
        org.junit.Assume.assumeTrue("Strong biometric enrollment required for the settings button", session.biometrics.available())
        compose.onNodeWithText("Security").performClick()
        compose.onNodeWithText("Enable biometrics").performScrollTo().performClick()
        compose.onNodeWithText("Continue to fingerprint").performClick()
        compose.onNodeWithText("Enter your current master password to enable biometrics.").assertIsDisplayed()
        compose.onNodeWithText("Current master password for biometrics").performTextInput("synthetic wrong password")
        compose.onNodeWithText("Continue to fingerprint").performClick()
        compose.waitUntil(120_000) {
            compose.onAllNodesWithText("Current master password is incorrect. Try again.").fetchSemanticsNodes().isNotEmpty()
        }
        assertFalse(session.biometrics.enabled())
        compose.onNodeWithText("Enable biometrics").performScrollTo().performClick()
        compose.onNodeWithText("Current master password for biometrics").assertTextEquals("Current master password for biometrics", "")
        compose.onNodeWithText("Cancel", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Confirm biometric enrollment").assertDoesNotExist()
        assertNotNull(session.state.value.access)

        // Open the actual platform/compat fingerprint window: checking availability alone
        // misses legacy dialog theme crashes. Cancel without fabricating biometric success.
        compose.onNodeWithText("Enable biometrics").performScrollTo().performClick()
        compose.onNodeWithText("Current master password for biometrics")
            .performTextReplacement("synthetic test master")
        compose.onNodeWithText("Continue to fingerprint")
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) { assertTrue(it()) }
        compose.waitUntil(30_000) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow
                ?.findAccessibilityNodeInfosByText("Enable biometric unlock")?.isNotEmpty() == true
        }
        if (android.os.Build.VERSION.SDK_INT < 28) compose.runOnUiThread {
            fun secureDialogCount(manager: androidx.fragment.app.FragmentManager): Int =
                manager.fragments.sumOf { fragment ->
                    val window = (fragment as? androidx.fragment.app.DialogFragment)
                        ?.dialog?.takeIf { it.isShowing }?.window
                    if (window != null) assertTrue("App-owned fingerprint dialog must be FLAG_SECURE",
                        window.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_SECURE != 0)
                    (if (window != null) 1 else 0) + secureDialogCount(fragment.childFragmentManager)
                }
            assertTrue("Actual compat fingerprint dialog must exist",
                secureDialogCount(compose.activity.supportFragmentManager) > 0)
        }
        // API 28+ owns the prompt in System UI, outside Espresso's app-window root.
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
            automation.executeShellCommand("input keyevent 4")
        ).use { it.readBytes() }
        // Verify the observable cancellation; command completion alone is not evidence.
        compose.waitUntil(10_000) {
            automation.rootInActiveWindow?.findAccessibilityNodeInfosByText("Enable biometric unlock")
                ?.isEmpty() == true
        }
        compose.waitForIdle()
        assertFalse("Cancelled native prompt must not write a wrapper", session.biometrics.enabled())
        assertNotNull("Cancelling enrollment must preserve password access", session.state.value.access)
    }
}
