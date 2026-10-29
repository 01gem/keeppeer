package com.gem.keeppeer

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import com.gem.keeppeer.security.VaultStage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EntryDraftRotationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun assertPasswordRendering(revealed: Boolean) {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("Password").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
            assertTrue(action(layouts))
        }
        val synthetic = " synthetic rotation "
        val expected = if (revealed) synthetic
            else PasswordVisualTransformation().filter(AnnotatedString(synthetic)).text.text
        assertEquals(expected, layouts.single().layoutInput.text.text)
    }

    @Test fun draftSurvivesActivityRecreationAndCancelDoesNotWrite() {
        val session = (compose.activity.application as KeeppeerApplication).container.session
        compose.runOnIdle {
            assertEquals("Authentication fixture requires a resumed Activity",
                Lifecycle.State.RESUMED, compose.activity.lifecycle.currentState)
            if (session.state.value.stage == VaultStage.FIRST_RUN)
                session.setup("synthetic test master".toCharArray(), "synthetic test master".toCharArray())
            else session.unlock("synthetic test master".toCharArray())
        }
        compose.waitUntil(120_000) { session.state.value.stage != VaultStage.UNLOCKING }
        val opened = session.state.value
        assertEquals("Synthetic rotation setup: ${opened.message}; lifecycle=${compose.activity.lifecycle.currentState}",
            VaultStage.UNLOCKED, opened.stage)
        val repository = requireNotNull(session.state.value.access).repository
        val before = runBlocking { repository.observeAll().first().map { it.id } }
        compose.onNodeWithText("Add").performClick()
        compose.onNodeWithText("Title *").performTextInput("Synthetic rotation")
        compose.onNodeWithText("Password").performScrollTo().performTextInput(" synthetic rotation ")
        compose.onNodeWithText("Notes (optional)").performScrollTo().performTextInput("雲\nSecond line")
        compose.onNodeWithText("Show").performScrollTo().performClick()
        assertPasswordRendering(revealed = true)
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Password").performScrollTo()
        assertPasswordRendering(revealed = false)
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
