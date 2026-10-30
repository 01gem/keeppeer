package com.gem.keeppeer

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.gem.keeppeer.security.*
import com.gem.keeppeer.ui.lock.VaultRoot
import com.gem.keeppeer.ui.theme.KeeppeerTheme
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Honors FLAG_SECURE; never removes protection or captures recents/other applications' cards. */
class VaultScreenshotPolicyTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test fun actualScreenshotApiUsesReadableControlAndProtectedSyntheticDraftAndDialog() {
        assertTrue(compose.activity.application is SyntheticTestApplication)
        val session = (compose.activity.application as SyntheticTestApplication).container.session
        var dialogSession: VaultSession? = null
        // Only synthetic screenshots/control images are saved, in app-scoped artifact storage.
        val directory = File(checkNotNull(instrumentation.targetContext.getExternalFilesDir(null)),
            "private-captures/" + UUID.randomUUID())
        check(directory.mkdirs())
        fun metric(text: String) = instrumentation.sendStatus(2, Bundle().apply {
            putString("stream", "\nKeeppeerScreenshot $text path=${directory.path}\n")
        })
        fun capture(name: String): Bitmap? {
            val bitmap = instrumentation.uiAutomation.takeScreenshot()
            if (bitmap == null) {
                metric("$name=UNAVAILABLE_no_bitmap")
                return null
            }
            FileOutputStream(File(directory, "$name.png")).use {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
            return bitmap
        }
        fun blackFraction(bitmap: Bitmap): Double {
            var black = 0
            var sampled = 0
            for (y in bitmap.height * 15 / 100 until bitmap.height * 60 / 100 step 3) {
                for (x in bitmap.width / 5 until bitmap.width * 4 / 5 step 3) {
                    val pixel = bitmap.getPixel(x, y)
                    if ((pixel and 0x00ffffff) == 0) black++
                    sampled++
                }
            }
            return black.toDouble() / sampled
        }
        try {
            val displayIntent = Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val displayComponent = checkNotNull(displayIntent.resolveActivity(instrumentation.targetContext.packageManager))
                .flattenToShortString()
            instrumentation.targetContext.startActivity(displayIntent)
            // A single accessibility root is not reliable for the foreground Settings window on
            // this provider. Verify the resolved foreground Activity, without reading UI contents.
            compose.waitUntil(10_000) {
                android.os.ParcelFileDescriptor.AutoCloseInputStream(
                    instrumentation.uiAutomation.executeShellCommand(
                        "dumpsys activity activities | grep mResumedActivity")
                ).use { it.readBytes().toString(Charsets.UTF_8).contains(displayComponent) }
            }
            val control = capture("control-display-settings")
            assumeTrue("Screenshot API returned no valid control; no protection conclusion", control != null)
            val controlBlack = blackFraction(control!!)
            control.recycle()
            metric("controlBlackFraction=$controlBlack")
            assertTrue("Control must contain readable nonblack content", controlBlack < 0.99)
            instrumentation.targetContext.startActivity(Intent(instrumentation.targetContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            compose.waitUntil(30_000) {
                compose.activity.hasWindowFocus() && session.state.value.stage != VaultStage.CHECKING
            }
            compose.runOnUiThread {
                session.foreground()
                if (session.state.value.stage == VaultStage.FIRST_RUN)
                    session.setup("synthetic test master".toCharArray(), "synthetic test master".toCharArray())
                else if (session.state.value.access == null) session.unlock("synthetic test master".toCharArray())
            }
            compose.waitUntil(120_000) { session.state.value.access != null }
            compose.onNodeWithText("Add").performClick()
            compose.onNodeWithText("Title *").performTextReplacement("Synthetic screenshot fixture")
            compose.onNodeWithText("Password").performScrollTo().performTextReplacement("synthetic screenshot only")
            compose.onNodeWithText("Show").performScrollTo().performClick()
            compose.onNodeWithText("Password").assertTextContains("synthetic screenshot only")
            compose.runOnUiThread {
                assertTrue(compose.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
            }
            val draft = capture("protected-synthetic-draft")
            assumeTrue("Protected API returned no bitmap; not positive screenshot-policy evidence", draft != null)
            val draftBlack = blackFraction(draft!!)
            draft.recycle()
            metric("draftBlackFraction=$draftBlack")
            assertEquals("Protected app sample must be black", 1.0, draftBlack, 0.0)
            compose.onNodeWithText("Cancel").performScrollTo().performClick()
            // Use the real legacy confirmation dialog with a new synthetic context; cancel only.
            // This path has no biometric enrollment or native authentication operation.
            val legacyContext = SyntheticVaultContext(instrumentation.targetContext)
            val legacy = legacyContext.getDatabasePath(com.gem.keeppeer.data.local.EncryptedDatabaseProvider.LEGACY_NAME)
            legacy.writeText("Synthetic screenshot dialog fixture")
            val confirmation = syntheticOnMain {
                VaultSession(legacyContext, SensitiveClipboard(legacyContext), legacyContext.namespace,
                    deviceLocked = { false })
            }
            dialogSession = confirmation
            compose.runOnUiThread {
                session.lock()
                confirmation.foreground()
                compose.activity.setContent {
                    KeeppeerTheme { VaultRoot(confirmation) { _, _, _ -> error("No authentication expected") } }
                }
            }
            compose.waitUntil(30_000) { confirmation.state.value.stage == VaultStage.LEGACY_RESET }
            compose.onNodeWithText("Reset synthetic development data…").performClick()
            compose.onNodeWithText("Permanently delete synthetic Phase 1 data?").assertIsDisplayed()
            val dialog = capture("protected-synthetic-dialog")
            assumeTrue("Protected dialog API returned no bitmap; no dialog-policy conclusion", dialog != null)
            val dialogBlack = blackFraction(dialog!!)
            dialog.recycle()
            metric("dialogBlackFraction=$dialogBlack visualReviewRequired=true")
            assertEquals("Protected dialog sample must be black", 1.0, dialogBlack, 0.0)
            compose.onNodeWithText("Cancel", useUnmergedTree = true).performClick()
            assertEquals("Synthetic screenshot dialog fixture", legacy.readText())
        } finally {
            compose.runOnUiThread { session.lock(); dialogSession?.lock() }
        }
    }
}
