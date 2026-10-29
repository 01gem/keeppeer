package com.gem.keeppeer

import android.content.Intent
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.gem.keeppeer.security.VaultStage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Produces only synthetic-app and generic Display Settings captures for separate visual review. */
class SecureCaptureTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private fun shell(command: String) {
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
            .use { it.readBytes() }
    }

    @Test fun syntheticCaptureArtifactsForVisualReview() {
        // Fail closed if accidentally invoked through a runner that can access the normal vault.
        assertTrue(compose.activity.application is SyntheticTestApplication)
        val session = (compose.activity.application as SyntheticTestApplication).container.session
        org.junit.Assume.assumeTrue("Enrolled device required for this dialog-capture journey",
            session.biometrics.available())
        val automation = instrumentation.uiAutomation
        val directory = "/data/local/tmp/keeppeer-phase2-capture-${SystemClock.elapsedRealtime()}"
        shell("mkdir -p $directory")
        fun capture(name: String) {
            shell("screencap -p $directory/$name.png")
            shell("screenrecord --size 540x960 --time-limit 5 $directory/$name.mp4")
        }
        try {
            // Generic system settings supplies an unprotected recorder control; never show
            // personal vault screens, notification shade, or other apps' recent-task cards.
            instrumentation.targetContext.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            compose.waitUntil(10_000) {
                automation.rootInActiveWindow?.packageName?.toString() == "com.android.settings"
            }
            capture("control-display-settings")
            instrumentation.targetContext.startActivity(Intent(instrumentation.targetContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            compose.waitUntil(10_000) {
                compose.activity.lifecycle.currentState == androidx.lifecycle.Lifecycle.State.RESUMED &&
                    compose.activity.hasWindowFocus()
            }
            compose.runOnIdle {
                session.foreground()
                val password = "synthetic test master".toCharArray()
                if (session.state.value.stage == VaultStage.FIRST_RUN) session.setup(password, password)
                else session.unlock(password)
            }
            compose.waitUntil(120_000) { session.state.value.stage != VaultStage.UNLOCKING }
            assertEquals(VaultStage.UNLOCKED, session.state.value.stage)
            compose.onNodeWithText("Add").performClick()
            compose.onNodeWithText("Title *").performTextReplacement("Synthetic capture fixture")
            compose.onNodeWithText("Password").performScrollTo().performTextReplacement("synthetic capture only")
            compose.onNodeWithText("Show").performScrollTo().performClick()
            compose.onNodeWithText("Password").assertTextContains("synthetic capture only")
            compose.runOnUiThread {
                assertTrue(compose.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
            }
            capture("revealed-synthetic-draft")
            compose.onNodeWithText("Cancel").performScrollTo().performClick()
            compose.onNodeWithText("Security").performClick()
            compose.onNodeWithText("Enable biometrics").performScrollTo().performClick()
            compose.onNodeWithText("Current master password for biometrics")
                .performTextReplacement("synthetic test master")
            compose.onNodeWithText("Confirm biometric enrollment").assertIsDisplayed()
            capture("synthetic-confirmation-dialog")
            compose.onNodeWithText("Cancel", useUnmergedTree = true).performClick()
            assertFalse(session.biometrics.enabled())
            assertNotNull(session.state.value.access)
        } finally {
            instrumentation.runOnMainSync { session.lock() }
            instrumentation.sendStatus(2, Bundle().apply {
                putString("stream", "\nKeeppeerCapture artifacts=$directory visualReviewRequired=true\n")
            })
        }
    }
}
