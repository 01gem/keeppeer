package com.gem.keeppeer

import android.os.Looper
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.gem.keeppeer.security.*
import com.gem.keeppeer.ui.lock.VaultRoot
import com.gem.keeppeer.ui.theme.KeeppeerTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class VaultAvailabilityUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun checkingAndFailedInspectionHideCreationAndImpossibleUnlockControls() {
        val context = SyntheticVaultContext(InstrumentationRegistry.getInstrumentation().targetContext)
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val guard = FileVaultInitialization(context, context.namespace, aliasExists = {
            assertNotSame(Looper.getMainLooper(), Looper.myLooper())
            entered.countDown()
            check(release.await(30, TimeUnit.SECONDS))
            throw IOException("Synthetic unavailable Keystore")
        })
        val session = syntheticOnMain { VaultSession(context, SensitiveClipboard(context), context.namespace,
            deviceLocked = { false }, initialization = guard) }
        try {
            compose.runOnUiThread {
                session.foreground()
                compose.activity.setContent {
                    KeeppeerTheme { VaultRoot(session) { _, _, _ -> error("Unexpected authentication prompt") } }
                }
            }
            assertTrue(entered.await(10, TimeUnit.SECONDS))
            compose.onNodeWithText("Checking vault availability…").assertIsDisplayed()
            fun noAuthenticationControls() {
                compose.onNodeWithText("Create vault").assertDoesNotExist()
                compose.onNodeWithText("Unlock", useUnmergedTree = true).assertDoesNotExist()
                compose.onNodeWithText("Master password").assertDoesNotExist()
                compose.onNodeWithText("Unlock with biometrics").assertDoesNotExist()
            }
            noAuthenticationControls()
            val password = "synthetic guard master".toCharArray()
            val confirmation = password.copyOf()
            compose.runOnUiThread { session.setup(password, confirmation) }
            assertTrue(password.all { it == '\u0000' } && confirmation.all { it == '\u0000' })
            release.countDown()
            compose.waitUntil(30_000) { session.state.value.stage == VaultStage.UNAVAILABLE }
            compose.onNodeWithText("Vault unavailable").assertIsDisplayed()
            noAuthenticationControls()
            assertNull(session.state.value.access)
        } finally {
            release.countDown()
            compose.runOnUiThread { session.lock() }
        }
    }
}
