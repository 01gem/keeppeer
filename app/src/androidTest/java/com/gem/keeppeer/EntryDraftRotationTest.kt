package com.gem.keeppeer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EntryDraftRotationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun draftSurvivesActivityRecreationAndCancelDoesNotWrite() {
        val session = (compose.activity.application as KeeppeerApplication).container.session
        compose.runOnUiThread {
            if (session.state.value.stage == com.gem.keeppeer.security.VaultStage.FIRST_RUN)
                session.setup("synthetic test master".toCharArray(), "synthetic test master".toCharArray())
            else session.unlock("synthetic test master".toCharArray())
        }
        compose.waitUntil(120_000) { session.state.value.access != null }
        val repository = requireNotNull(session.state.value.access).repository
        val before = runBlocking { repository.observeAll().first().map { it.id } }
        compose.onNodeWithText("Add").performClick()
        compose.onNodeWithText("Title *").performTextInput("Synthetic rotation")
        compose.onNodeWithText("Password").performScrollTo().performTextInput(" synthetic rotation ")
        compose.onNodeWithText("Notes (optional)").performScrollTo().performTextInput("雲\nSecond line")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Title *").performScrollTo().assertTextContains("Synthetic rotation")
        compose.onNodeWithText("Notes (optional)").performScrollTo().assertTextContains("雲\nSecond line")
        compose.onNodeWithText("Show").performScrollTo().performClick()
        compose.onNodeWithText("Password").assertTextContains(" synthetic rotation ")
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        compose.onNodeWithText("Keeppeer").assertIsDisplayed()
        val after = runBlocking { repository.observeAll().first().map { it.id } }
        assertEquals(before, after)
    }
}
