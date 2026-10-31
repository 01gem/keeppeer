package com.gem.keeppeer

import android.app.ActivityManager
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Real DialogFragment windows use MainActivity's production lifecycle registrations. */
class WindowRegistrationTest {
    @get:Rule val activityRule = ActivityScenarioRule(MainActivity::class.java)
    private lateinit var host: MainActivity

    private fun main(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block()
        else InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    }

    private fun shell(command: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        ).bufferedReader().use { it.readText().trim() }

    private fun deviceAction(key: String) {
        assertEquals("1", shell("getprop ro.kernel.qemu"))
        val avd = shell("getprop ro.boot.qemu.avd_name").ifEmpty {
            shell("getprop ro.kernel.qemu.avd_name").ifEmpty { shell("getprop qemu.avd_name") }
        }
        assertTrue("Dedicated synthetic Guard emulator required", avd.startsWith("Keeppeer_Guard_"))
        assertTrue(shell("getprop ro.build.version.sdk") in listOf("25", "31"))
        assertEquals("x86_64", shell("getprop ro.product.cpu.abi"))
        val action = when (key) {
            "KEYCODE_APP_SWITCH" -> android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS
            "KEYCODE_HOME" -> android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
            "KEYCODE_BACK" -> android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
            else -> error("Unsupported fixture action")
        }
        assertTrue("The actual OS action must be accepted",
            InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(action))
    }

    private fun step(value: String) =
        InstrumentationRegistry.getInstrumentation().sendStatus(2, Bundle().apply {
            putString("stream", "\nKeeppeerWindowStep=$value\n")
        })

    private fun dialogWindow(): Window {
        var result: Window? = null
        main { result = (host.supportFragmentManager.findFragmentByTag(TAG) as OpacityDialog)
            .requireDialog().window }
        return requireNotNull(result)
    }

    private fun waitFor(condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 15_000
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return
            SystemClock.sleep(25)
        }
        var state = "unknown"
        main { state = host.lifecycle.currentState.name }
        fail("Window condition timed out; Activity lifecycle=$state")
    }

    private fun awaitWindow(window: Window, focused: Boolean, alpha: Float) {
        waitFor {
            var matches = false
            main { matches = window.decorView.hasWindowFocus() == focused &&
                window.attributes.alpha == alpha }
            matches
        }
        main { assertTrue(window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0) }
    }

    @Suppress("DEPRECATION")
    private fun returnToOwnActivity() {
        // Bring back this Scenario's task.
        val intent = android.content.Intent(host, MainActivity::class.java).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                     android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        InstrumentationRegistry.getInstrumentation().targetContext.startActivity(intent)
        // A showing Dialog owns focus instead of its Activity window on API31.
        // The caller still verifies the exact original Window and its restored alpha.
        waitFor {
            var focused = false
            main {
                val dialog = host.supportFragmentManager.findFragmentByTag(TAG) as? OpacityDialog
                val dialogFocused = dialog?.dialog?.window?.decorView?.hasWindowFocus() == true
                val hostFocused = host.hasWindowFocus()
                focused = hostFocused || dialogFocused
                if (focused) step("RETURN_FOCUS_HOST_${hostFocused}_DIALOG_${dialogFocused}")
            }
            focused
        }
    }

    @Test fun hiddenDialogStopStartRestoresOriginalAlphaAndClosingStaysRedacted() {
        activityRule.scenario.onActivity { host = it }
        try {
            main { OpacityDialog().showNow(host.supportFragmentManager, TAG) }
            val original = dialogWindow()
            awaitWindow(original, true, ORIGINAL_ALPHA)
            step("ORIGINAL_VISIBLE")
            deviceAction("KEYCODE_APP_SWITCH")
            awaitWindow(original, false, 0f)
            step("RECENTS_REDACTED")

            // Ordinary Home stops the Activity and removes the production dialog registration.
            deviceAction("KEYCODE_HOME")
            waitFor {
                var stopped = false
                main { stopped = host.lifecycle.currentState == Lifecycle.State.CREATED }
                stopped
            }
            main {
                assertSame("Stop must retain this actual Window", original, dialogWindow())
                assertEquals("Unregister must not reveal an unfocused closing window",
                    0f, original.attributes.alpha, 0f)
            }
            step("SAME_WINDOW_STOPPED")
            returnToOwnActivity()
            assertSame(original, dialogWindow())
            awaitWindow(original, true, ORIGINAL_ALPHA)
            step("ORIGINAL_ALPHA_RESTORED")

            deviceAction("KEYCODE_APP_SWITCH")
            awaitWindow(original, false, 0f)
            main {
                // API25 saves Fragment state while Recents backgrounds this fixture. The
                // disposable dialog must still close there; process that removal before
                // checking that disposal never restores its hidden Window opacity.
                (host.supportFragmentManager.findFragmentByTag(TAG) as OpacityDialog)
                    .dismissAllowingStateLoss()
                host.supportFragmentManager.executePendingTransactions()
                assertEquals("Disposal must not restore a hidden dialog into Recents",
                    0f, original.attributes.alpha, 0f)
                assertEquals(0f, host.window.attributes.alpha, 0f)
            }
            step("CLOSING_STAYS_REDACTED")
            returnToOwnActivity()
            waitFor {
                var visible = false
                main { visible = host.hasWindowFocus() && host.window.attributes.alpha == 1f }
                visible
            }
            step("PARENT_RETURNED")
            main { OpacityDialog().showNow(host.supportFragmentManager, TAG) }
            val beforeRotation = dialogWindow()
            awaitWindow(beforeRotation, true, ORIGINAL_ALPHA)
            step("BEFORE_RECREATE")
            activityRule.scenario.recreate()
            activityRule.scenario.onActivity { host = it }
            step("AFTER_RECREATE")
            val recreated = dialogWindow()
            assertNotSame("Recreation must use a new Window", beforeRotation, recreated)
            awaitWindow(recreated, true, ORIGINAL_ALPHA)
            step("RECREATED_VISIBLE")
            main { (host.supportFragmentManager.findFragmentByTag(TAG) as OpacityDialog).dismissNow() }
            waitFor {
                var visible = false
                main { visible = host.hasWindowFocus() && host.window.attributes.alpha == 1f }
                visible
            }
            step("DISMISSED_PARENT_VISIBLE")
        } catch (failure: Throwable) {
            step("BODY_FAILURE_" + failure.javaClass.simpleName)
            throw failure
        } finally {
            // Dispose the fixture without launching a different task during Scenario teardown.
            main { (host.supportFragmentManager.findFragmentByTag(TAG) as? OpacityDialog)
                ?.dismissAllowingStateLoss() }
        }
    }

    companion object {
        private const val TAG = "synthetic-opacity-dialog"
        const val ORIGINAL_ALPHA = 0.8f
    }
}

class OpacityDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        Dialog(requireContext()).apply {
            setTitle("Synthetic window lifecycle")
            setContentView(TextView(context).apply {
                text = "Synthetic dialog opacity fixture"
                setPadding(32, 32, 32, 32)
            })
            window!!.attributes = window!!.attributes.apply {
                alpha = WindowRegistrationTest.ORIGINAL_ALPHA
            }
        }
}
