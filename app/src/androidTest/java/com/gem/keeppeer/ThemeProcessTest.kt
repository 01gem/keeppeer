package com.gem.keeppeer

import androidx.test.platform.app.InstrumentationRegistry
import com.gem.keeppeer.ui.settings.ThemeMode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.Assert.*

/** Run these methods in separate native instrumentation processes on the guarded synthetic AVD. */
class ThemeProcessTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as KeeppeerApplication
    @Test fun persistDarkForNextProcess() = runBlocking {
        val settings = app.container.appearance
        withTimeout(10_000) { settings.state.first { !it.loading } }
        syntheticOnMain { settings.select(ThemeMode.LIGHT) }
        val light = withTimeout(10_000) { settings.state.first { !it.saving && it.mode == ThemeMode.LIGHT } }
        assertFalse(light.writeFailed)
        syntheticOnMain { settings.select(ThemeMode.DARK) }
        val state = withTimeout(10_000) { settings.state.first { !it.saving && it.mode == ThemeMode.DARK } }
        assertFalse(state.readFailed); assertFalse(state.writeFailed)
    }
    @Test fun newProcessReadsDarkThenRestoreSystem() = runBlocking {
        val settings = app.container.appearance
        val state = withTimeout(10_000) { settings.state.first { !it.loading } }
        assertFalse(state.readFailed)
        assertEquals(ThemeMode.DARK, state.mode)
        syntheticOnMain { settings.select(ThemeMode.SYSTEM) }
        val restored = withTimeout(10_000) { settings.state.first { !it.saving && it.mode == ThemeMode.SYSTEM } }
        assertFalse(restored.writeFailed)
    }
}
