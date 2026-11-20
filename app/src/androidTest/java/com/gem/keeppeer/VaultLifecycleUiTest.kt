package com.gem.keeppeer

import android.view.WindowManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import com.gem.keeppeer.security.VaultStage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class VaultLifecycleUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun backgroundAndManualLockRemoveDraftsAndRevokeRepository() {
        val session = (compose.activity.application as KeeppeerApplication).container.session
        compose.waitUntil(30_000) { session.state.value.stage != VaultStage.CHECKING }
        compose.runOnUiThread {
            if (session.state.value.stage == VaultStage.FIRST_RUN)
                session.setup("synthetic test master".toCharArray(), "synthetic test master".toCharArray())
            else session.unlock("synthetic test master".toCharArray())
        }
        compose.waitUntil(120_000) { session.state.value.access != null }
        val repository = requireNotNull(session.state.value.access).repository
        compose.runOnUiThread {
            assertTrue(compose.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
        }
        compose.onNodeWithText(str(R.string.add)).performClick()
        compose.onNodeWithText(str(R.string.title_required)).performTextInput("Synthetic discarded on lock")
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        assertNull(session.state.value.access)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.onNodeWithText(str(R.string.locked_title)).assertIsDisplayed()
        compose.onNodeWithText("Synthetic discarded on lock").assertDoesNotExist()
        try { runBlocking { repository.getById(1) }; fail("Revoked repository must fail") }
        catch (_: IllegalStateException) {}
        compose.waitUntil(30_000) { session.state.value.stage == VaultStage.LOCKED }
        compose.onNodeWithText(str(R.string.master_password)).performTextInput("synthetic test master")
        compose.onNodeWithText(str(R.string.unlock), useUnmergedTree = true).performClick()
        compose.waitUntil(120_000) { session.state.value.access != null }
        compose.onNodeWithContentDescription(str(R.string.settings)).performClick()
        compose.onNodeWithText(str(R.string.lock_now)).performClick()
        compose.onNodeWithText(str(R.string.locked_title)).assertIsDisplayed()
    }
}

private fun str(id: Int): String =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
