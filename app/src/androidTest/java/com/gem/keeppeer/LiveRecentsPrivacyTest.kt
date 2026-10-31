package com.gem.keeppeer

import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import com.gem.keeppeer.domain.PasswordEntry
import com.gem.keeppeer.security.LockTimeout
import com.gem.keeppeer.security.VaultStage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/**
 * Actual OS Recents, not a simulated onStop. Pixel inspection remains a separate live
 * observation: window metadata alone cannot prove what a provider composites.
 */
class LiveRecentsPrivacyTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private fun shell(command: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
            .bufferedReader().use { it.readText().trim() }

    private fun deviceAction(command: String) {
        // Refuse a physical device, even if someone invokes this class outside the handoff.
        assertEquals("1", shell("getprop ro.kernel.qemu"))
        assertTrue("Only a dedicated synthetic Guard AVD is allowed",
            shell("getprop ro.boot.qemu.avd_name").startsWith("Keeppeer_Guard_"))
        val action = when (command) {
            "input keyevent KEYCODE_APP_SWITCH" -> android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS
            "input keyevent KEYCODE_BACK" -> android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
            else -> error("Unsupported fixture action")
        }
        assertTrue("Actual OS navigation action must be accepted",
            instrumentation.uiAutomation.performGlobalAction(action))
    }

    private fun awaitVisibility(visible: Boolean) {
        compose.waitUntil(15_000) {
            var matches = false
            compose.runOnUiThread {
                matches = compose.activity.window.attributes.alpha == (if (visible) 1f else 0f)
            }
            matches
        }
    }

    @Test fun actualRecentsRedactsDraftAndOwnedDialogWithoutExpiringTheSession() {
        val session = (compose.activity.application as KeeppeerApplication).container.session
        val originalTimeout = session.timeout.value
        try {
            compose.waitUntil(30_000) { session.state.value.stage != VaultStage.CHECKING }
            compose.runOnUiThread {
                session.setTimeout(LockTimeout.MINUTE)
                if (session.state.value.stage == VaultStage.FIRST_RUN)
                    session.setup("synthetic test master".toCharArray(), "synthetic test master".toCharArray())
                else session.unlock("synthetic test master".toCharArray())
            }
            compose.waitUntil(120_000) { session.state.value.access != null }
            val access = requireNotNull(session.state.value.access)
            val title = "Synthetic live Recents draft"
            compose.onNodeWithText("Add").performClick()
            compose.onNodeWithText("Title *").performTextInput(title)
            compose.onNodeWithText("Password").performScrollTo().performTextInput("synthetic recents value")
            compose.onNodeWithText(compose.activity.getString(R.string.show_password)).performClick()
            awaitVisibility(true)
            compose.runOnUiThread {
                assertTrue("The revealed draft must own focus before actual Recents",
                    compose.activity.hasWindowFocus())
            }

            deviceAction("input keyevent KEYCODE_APP_SWITCH")
            awaitVisibility(false)
            compose.runOnUiThread {
                assertFalse(compose.activity.hasWindowFocus())
                if (Build.VERSION.SDK_INT == 31) assertEquals(
                    "The reproduced API31 live-card case must remain RESUMED",
                    Lifecycle.State.RESUMED, compose.activity.lifecycle.currentState)
                assertSame("Visual privacy must not revoke an otherwise valid session",
                    access, session.state.value.access)
            }
            deviceAction("input keyevent KEYCODE_BACK")
            awaitVisibility(true)
            compose.onNodeWithText("Password").assertTextContains("synthetic recents value")
            compose.onNodeWithText("Title *").performScrollTo().assertTextContains(title)
            compose.onNodeWithText("Cancel").performScrollTo().performClick()

            // Use the real app-owned delete confirmation without deleting its synthetic entry.
            val id = runBlocking { access.repository.create(PasswordEntry(
                title = "Synthetic dialog Recents", password = "synthetic dialog value")) }
            compose.onNodeWithText("Synthetic dialog Recents").performClick()
            compose.onNodeWithText("Delete", useUnmergedTree = true).performScrollTo().performClick()
            compose.onNodeWithText("Delete entry?").assertIsDisplayed()
            awaitVisibility(true) // Dialog focus must restore the parent without locking/cancelling.
            deviceAction("input keyevent KEYCODE_APP_SWITCH")
            awaitVisibility(false)
            deviceAction("input keyevent KEYCODE_BACK")
            awaitVisibility(true)
            compose.onNodeWithText("Delete entry?").assertIsDisplayed()
            compose.onNodeWithText("Cancel", useUnmergedTree = true).performClick()
            assertNotNull(runBlocking { access.repository.getById(id) })
        } finally {
            compose.runOnUiThread { session.setTimeout(originalTimeout); session.lock() }
        }
    }
}
