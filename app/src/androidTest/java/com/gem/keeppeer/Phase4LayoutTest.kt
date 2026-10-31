package com.gem.keeppeer

import android.content.pm.ActivityInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gem.keeppeer.security.*
import com.gem.keeppeer.ui.lock.VaultRoot
import com.gem.keeppeer.ui.settings.ThemeMode
import com.gem.keeppeer.ui.theme.*
import org.junit.*
import org.junit.Assert.*

/** Bounds/semantics checks at 2x font in real portrait/landscape; not a manual TalkBack session. */
class Phase4LayoutTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app get() = compose.activity.application as KeeppeerApplication
    private fun text(id: Int) = compose.activity.getString(id)
    private fun click(id: Int, scroll: Boolean = false) {
        val dialog = hasText(text(id)) and hasAnyAncestor(isDialog())
        val node = if (compose.onAllNodes(dialog).fetchSemanticsNodes().isNotEmpty()) compose.onNode(dialog)
            else compose.onNodeWithText(text(id))
        if (scroll) node.performScrollTo()
        node.performClick()
    }
    @Test fun portraitLandscapeTwoTimesFontsAndAllThemesKeepActionsReachable() {
        val session = app.container.session
        compose.waitUntil(30_000) { session.state.value.stage != VaultStage.CHECKING }
        compose.runOnUiThread {
            if (session.state.value.stage == VaultStage.FIRST_RUN)
                session.setup("synthetic test master".toCharArray(), "synthetic test master".toCharArray())
            else session.unlock("synthetic test master".toCharArray())
        }
        compose.waitUntil(120_000) { session.state.value.access != null }
        for (orientation in listOf(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)) {
            compose.runOnUiThread { compose.activity.requestedOrientation = orientation }
            compose.waitUntil(15_000) {
                val c = compose.activity.resources.configuration
                if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) c.screenWidthDp > c.screenHeightDp
                else c.screenHeightDp > c.screenWidthDp
            }
            compose.runOnUiThread {
                compose.activity.setContent {
                    val theme by app.container.appearance.state.collectAsStateWithLifecycle()
                    val density = LocalDensity.current.density
                    CompositionLocalProvider(LocalDensity provides Density(density, 2f), LocalThemeMode provides theme.mode) {
                        KeeppeerTheme(darkTheme = theme.mode.isDark(isSystemInDarkTheme())) {
                            androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.widthIn(max = 320.dp)) {
                                VaultRoot(session) { _, _, _ -> error("No biometric enrollment in layout fixture") }
                            }
                        }
                    }
                }
            }
            compose.waitForIdle()
            click(R.string.add)
            click(R.string.generate, scroll = true)
            compose.onNodeWithText(text(R.string.generator_use)).assertIsDisplayed()
            compose.onNodeWithText(text(R.string.generator_lower)).performScrollTo()
                .assertIsToggleable().assertIsOn()
            compose.onNodeWithText(text(R.string.generator_regenerate)).performScrollTo().assertIsDisplayed()
            click(R.string.cancel)
            compose.onNodeWithText(text(R.string.save)).performScrollTo().assertIsDisplayed()
            click(R.string.settings)
            for (mode in ThemeMode.entries) {
                val id = when (mode) { ThemeMode.SYSTEM -> R.string.theme_system; ThemeMode.LIGHT -> R.string.theme_light; ThemeMode.DARK -> R.string.theme_dark }
                click(id, scroll = true)
                compose.waitUntil(10_000) { !app.container.appearance.state.value.saving && app.container.appearance.state.value.mode == mode }
                compose.onNodeWithText(text(id)).assertIsSelected()
            }
            click(R.string.about, scroll = true)
            compose.onNodeWithText(text(R.string.about_export)).performScrollTo().assertIsDisplayed()
            click(R.string.csv_continue)
            click(R.string.back)
            click(R.string.cancel, scroll = true)
        }
        compose.runOnUiThread { compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
}
