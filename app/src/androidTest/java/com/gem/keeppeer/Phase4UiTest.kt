package com.gem.keeppeer

import android.content.ClipboardManager
import android.view.WindowManager
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.Lifecycle
import com.gem.keeppeer.domain.PasswordEntry
import com.gem.keeppeer.security.*
import com.gem.keeppeer.ui.settings.ThemeMode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*

class Phase4UiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app get() = compose.activity.application as KeeppeerApplication
    private val session get() = app.container.session
    private fun text(id: Int) = compose.activity.getString(id)
    private fun node(id: Int): SemanticsNodeInteraction {
        val dialog = hasText(text(id)) and hasAnyAncestor(isDialog())
        return if (compose.onAllNodes(dialog).fetchSemanticsNodes().isNotEmpty()) compose.onNode(dialog)
            else compose.onNodeWithText(text(id))
    }
    private fun click(id: Int) { node(id).performClick(); compose.waitForIdle() }
    private fun scrollClick(id: Int) { node(id).performScrollTo().performClick(); compose.waitForIdle() }
    private fun unlock() {
        compose.waitUntil(30_000) { session.state.value.stage !in listOf(VaultStage.CHECKING, VaultStage.UNLOCKING) }
        compose.runOnUiThread {
            if (session.state.value.stage == VaultStage.FIRST_RUN) session.setup(MASTER.toCharArray(), MASTER.toCharArray())
            else if (session.state.value.access == null) session.unlock(MASTER.toCharArray())
        }
        compose.waitUntil(120_000) { session.state.value.access != null }
    }
    private fun value(id: Int) = node(id).fetchSemanticsNode().config[SemanticsProperties.InputText].text
    private fun rendering(id: Int, raw: String, revealed: Boolean) {
        val layouts = mutableListOf<TextLayoutResult>()
        node(id).performScrollTo().assertIsDisplayed()
        node(id).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> assertTrue(action(layouts)) }
        val expected = if (revealed) raw else PasswordVisualTransformation().filter(AnnotatedString(raw)).text.text
        assertTrue("Password rendering matches requested visibility", layouts.single().layoutInput.text.text == expected)
    }
    private fun secureDialogs() {
        compose.runOnIdle {
            val field = compose.activity.privacyWindows.javaClass.getDeclaredField("windows").apply { isAccessible = true }
            val windows = field.get(compose.activity.privacyWindows) as Map<*, *>
            assertTrue("Owned dialog registered", windows.size >= 2)
            windows.keys.forEach { window ->
                assertTrue((window as android.view.Window).attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
            }
        }
    }

    @Test fun generatorDraftThemeRotationCopyAndLockJourney() = runBlocking { generatorJourney(checkClipboard = true) }

    /** Independent state/lifecycle coverage; the full copy assertion above remains mandatory and unchanged. */
    @Test fun generatorCancelUseThemeAndLockWithoutClipboard() = runBlocking { generatorJourney(checkClipboard = false) }

    private suspend fun generatorJourney(checkClipboard: Boolean) {
        try {
        unlock()
        val access = session.state.value.access!!
        click(R.string.add)
        node(R.string.title_required).performTextInput("Phase4 synthetic")
        compose.runOnIdle {
            val info = android.view.inputmethod.EditorInfo()
            val connection = compose.activity.currentFocus?.onCreateInputConnection(info)
            assertNotNull("Focused editor provides an input connection", connection)
            assertTrue(info.imeOptions and android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0)
            assertTrue(info.inputType and android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS != 0)
            assertEquals(0, info.inputType and android.text.InputType.TYPE_TEXT_FLAG_AUTO_CORRECT)
            connection?.closeConnection()
        }
        node(R.string.password).performScrollTo().performTextInput("synthetic original")
        scrollClick(R.string.generate)
        secureDialogs()
        val first = value(R.string.generator_candidate)
        assertTrue(first.length == 20)
        rendering(R.string.generator_candidate, first, false)
        scrollClick(R.string.show_password)
        node(R.string.hide_password).assertExists()
        rendering(R.string.generator_candidate, first, true)
        compose.activityRule.scenario.recreate()
        val retained = value(R.string.generator_candidate)
        assertTrue(retained == first)
        rendering(R.string.generator_candidate, retained, false)
        scrollClick(R.string.generator_regenerate)
        val second = value(R.string.generator_candidate)
        assertTrue("Explicit regeneration replaces candidate", second != first)
        if (checkClipboard) {
            scrollClick(R.string.copy_password)
            node(R.string.clipboard_copied).assertExists()
            compose.runOnIdle {
                val manager = compose.activity.getSystemService(ClipboardManager::class.java)
                val description = manager.primaryClipDescription
                val label = description?.label?.toString()
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendStatus(2,
                    android.os.Bundle().apply {
                        putString("phase4_clip_label_classification", when {
                            label?.startsWith("Keeppeer:") == true -> "owned"
                            label?.equals("host clipboard", ignoreCase = true) == true -> "host_clipboard"
                            label == null -> "absent"
                            else -> "other"
                        })
                        val owned = label?.startsWith("Keeppeer:") == true
                        putBoolean("phase4_clip_payload_checked", owned)
                        if (owned) putBoolean("phase4_clip_matches_candidate", manager.primaryClip?.getItemAt(0)?.text?.toString() == second)
                        putBoolean("phase4_clip_sensitive", description?.extras?.getBoolean("android.content.extra.IS_SENSITIVE") == true)
                    })
                assertTrue("Copy routed through ownership-labelled service",
                    manager.primaryClipDescription?.label?.toString()?.startsWith("Keeppeer:") == true)
                assertTrue("Copied candidate is exact", manager.primaryClip?.getItemAt(0)?.text?.toString() == second)
            }
        }
        click(R.string.cancel)
        node(R.string.password).performScrollTo()
        assertTrue(value(R.string.password) == "synthetic original")
        scrollClick(R.string.generate)
        val chosen = value(R.string.generator_candidate)
        click(R.string.generator_use)
        assertTrue(value(R.string.password) == chosen)
        assertTrue(access.repository.observeAll().first().none { it.title == "Phase4 synthetic" })
        click(R.string.settings)
        secureDialogs()
        scrollClick(R.string.theme_dark)
        compose.waitUntil(10_000) { app.container.appearance.state.value.mode == ThemeMode.DARK && !app.container.appearance.state.value.saving }
        click(R.string.back)
        assertTrue(value(R.string.password) == chosen)
        rendering(R.string.password, chosen, false)
        scrollClick(R.string.save)
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text(R.string.entry_details)).fetchSemanticsNodes().isNotEmpty() }
        val stored = access.repository.exportCsv().single { it.title == "Phase4 synthetic" }
        assertTrue(stored.password == chosen)
        click(R.string.back)
        compose.onNodeWithText("Phase4 synthetic").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text(R.string.show_password)).fetchSemanticsNodes().isNotEmpty() }
        node(R.string.show_password).assertExists()
        scrollClick(R.string.edit)
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text(R.string.generate)).fetchSemanticsNodes().isNotEmpty() }
        scrollClick(R.string.generate)
        val revokedCandidate = value(R.string.generator_candidate)
        secureDialogs()
        compose.runOnUiThread { session.lock() }
        compose.waitUntil(30_000) { session.state.value.stage == VaultStage.LOCKED }
        node(R.string.generator_title).assertDoesNotExist()
        val retainedSecret = compose.onAllNodes(SemanticsMatcher("Check revoked candidate presence") { node ->
            node.config.getOrNull(SemanticsProperties.InputText)?.text == revokedCandidate ||
                node.config.getOrNull(SemanticsProperties.EditableText)?.text == revokedCandidate ||
                node.config.getOrNull(SemanticsProperties.Text)?.any { it.text == revokedCandidate } == true
        }).fetchSemanticsNodes().isNotEmpty()
        assertFalse("Generated value must be absent after revocation", retainedSecret)
        unlock()
        node(R.string.generator_title).assertDoesNotExist()
        node(R.string.add_entry).assertDoesNotExist()
        assertTrue(session.state.value.access!!.repository.getById(stored.id)!!.password == chosen)
        // Background uses the unchanged immediate policy and cannot recover the prior draft.
        click(R.string.add)
        node(R.string.title_required).performTextInput("discarded")
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        assertNull(session.state.value.access)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        node(R.string.locked_title).assertIsDisplayed()
        compose.onNodeWithText("discarded").assertDoesNotExist()
        } catch (failure: Throwable) {
            // Compose's default failed-node dumps can include EditableText.
            // Preserve the failure and source location without publishing a generated value.
            throw AssertionError("Generator journey failed; sensitive UI diagnostics omitted.").apply {
                stackTrace = failure.stackTrace
            }
        }
    }

    @Test fun favoritesCategoryAndLiteralSearchStayReactive() = runBlocking {
        unlock()
        val repo = session.state.value.access!!.repository
        val first = repo.create(PasswordEntry(title = "École 100%_\\ favorite", password = "synthetic", category = "All"))
        repo.create(PasswordEntry(title = "École other", password = "synthetic", category = "Work"))
        repo.create(PasswordEntry(title = "Blank", password = "synthetic", category = " "))
        compose.waitUntil(10_000) { compose.onAllNodesWithText("École 100%_\\ favorite").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("favorite-$first").performScrollTo().performClick()
        compose.waitUntil(10_000) { runBlocking { repo.getById(first)?.isFavorite == true } }
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text(R.string.favorites)).fetchSemanticsNodes().isNotEmpty() }
        node(R.string.search).performScrollTo().performTextInput("école 100%_\\")
        click(R.string.filters)
        secureDialogs()
        compose.onNodeWithText(compose.activity.getString(R.string.named_category, "All")).performScrollTo().performClick()
        scrollClick(R.string.favorites_only)
        click(R.string.csv_continue)
        compose.onNodeWithText("École 100%_\\ favorite").assertExists()
        compose.onNodeWithText("École other").assertDoesNotExist()
        repo.delete(first)
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text(R.string.no_results)).fetchSemanticsNodes().isNotEmpty() }
        click(R.string.filters)
        node(R.string.active_category_removed).performScrollTo().assertIsDisplayed()
        click(R.string.clear_filters)
        click(R.string.csv_continue)
        compose.onNodeWithText("École other").assertExists()
        compose.onNodeWithText("Blank").assertExists()
        Unit
    }
    companion object { private const val MASTER = "synthetic test master" }
}
