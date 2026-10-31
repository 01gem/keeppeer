package com.gem.keeppeer

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.gem.keeppeer.ui.settings.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [28], application = android.app.Application::class, manifest = org.robolectric.annotation.Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class ThemeSettingsTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun realDataStorePersistsAcrossClosedStoreAndNewReader() = runBlocking {
        val file = temp.newFolder().resolve("appearance.preferences_pb")
        for (mode in ThemeMode.entries) {
            val owner = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val store = PreferenceDataStoreFactory.create(scope = owner) { file }
            val settings = ThemeSettings(store, owner)
            withTimeout(10_000) { settings.state.first { !it.loading } }
            settings.select(ThemeMode.DARK)
            withTimeout(10_000) { settings.state.first { !it.saving && it.mode == ThemeMode.DARK } }
            settings.select(mode)
            withTimeout(10_000) { settings.state.first { !it.saving && it.mode == mode } }
            assertFalse(settings.state.value.writeFailed)
            // Reading the real store verifies disk-backed update completion before shutdown.
            assertEquals(mode.name, store.data.first()[stringPreferencesKey("theme")])
            owner.coroutineContext[Job]!!.cancelAndJoin()
            val readerOwner = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val reader = PreferenceDataStoreFactory.create(scope = readerOwner) { file }
            assertEquals(mode.name, reader.data.first()[stringPreferencesKey("theme")])
            val reopened = ThemeSettings(reader, readerOwner)
            val readState = withTimeout(10_000) { reopened.state.first { !it.loading } }
            assertFalse("Reopened real DataStore read must succeed", readState.readFailed)
            assertEquals(mode, readState.mode)
            assertEquals(setOf("theme"), reader.data.first().asMap().keys.map { it.name }.toSet())
            readerOwner.coroutineContext[Job]!!.cancelAndJoin()
        }
    }
    @Test fun systemFollowsActualInputAndExplicitModesOverrideIt() {
        assertFalse(ThemeMode.SYSTEM.isDark(false)); assertTrue(ThemeMode.SYSTEM.isDark(true))
        for (system in listOf(false, true)) {
            assertFalse(ThemeMode.LIGHT.isDark(system)); assertTrue(ThemeMode.DARK.isDark(system))
        }
    }
    @Test fun readFailureUsesSafeDefaultBlocksWriteAndCanRetry() = runTest {
        var fail = true
        var writes = 0
        val store = object : DataStore<Preferences> {
            override val data: Flow<Preferences> get() = flow {
                if (fail) throw IOException("synthetic private detail")
                emit(preferencesOf(stringPreferencesKey("theme") to "DARK"))
            }
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
                writes++; return transform(emptyPreferences())
            }
        }
        val settings = ThemeSettings(store, backgroundScope)
        runCurrent()
        assertTrue(settings.state.value.readFailed)
        assertEquals(ThemeMode.SYSTEM, settings.state.value.mode)
        settings.select(ThemeMode.DARK); runCurrent(); assertEquals(0, writes)
        fail = false; settings.retry(); runCurrent()
        assertEquals(ThemeMode.DARK, settings.state.value.mode)
        assertFalse(settings.state.value.readFailed)
    }
    @Test fun failedWriteNeverClaimsOrAppliesRequestedSelection() = runTest {
        val store = object : DataStore<Preferences> {
            override val data = flowOf(preferencesOf(stringPreferencesKey("theme") to "LIGHT"))
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                throw IOException("synthetic")
        }
        val settings = ThemeSettings(store, backgroundScope)
        runCurrent()
        settings.select(ThemeMode.DARK); runCurrent()
        assertEquals(ThemeMode.LIGHT, settings.state.value.mode)
        assertTrue(settings.state.value.writeFailed)
        assertFalse(settings.state.value.saving)
    }
}
