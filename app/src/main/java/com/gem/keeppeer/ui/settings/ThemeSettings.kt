package com.gem.keeppeer.ui.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class ThemeMode {
    SYSTEM, LIGHT, DARK;
    fun isDark(systemDark: Boolean) = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }
}

// One instance per application/file. No vault data ever enters this DataStore.
private val Context.themeDataStore by preferencesDataStore(name = "appearance")
data class ThemeState(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val readFailed: Boolean = false,
    val writeFailed: Boolean = false,
)

class ThemeSettings(
    private val store: DataStore<Preferences>,
    private val scope: CoroutineScope,
) {
    constructor(context: Context) : this(context.applicationContext.themeDataStore,
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate))
    private val key = stringPreferencesKey("theme")
    private val mutable = MutableStateFlow(ThemeState())
    val state = mutable.asStateFlow()
    private val writes = Mutex()
    private var reader: Job? = null
    init { retry() }
    fun retry() {
        reader?.cancel()
        mutable.update { it.copy(loading = true, readFailed = false) }
        reader = scope.launch {
            try {
                store.data.collect { preferences ->
                    val mode = ThemeMode.entries.firstOrNull { it.name == preferences[key] } ?: ThemeMode.SYSTEM
                    mutable.update {
                        it.copy(mode = if (it.saving || it.writeFailed) it.mode else mode,
                            loading = false, readFailed = false)
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutable.update { it.copy(loading = false, readFailed = true) } }
        }
    }
    fun select(mode: ThemeMode) {
        if (state.value.loading || state.value.saving || state.value.readFailed) return
        val applied = state.value.mode
        mutable.update { it.copy(saving = true, writeFailed = false) }
        scope.launch {
            writes.withLock {
                try {
                    store.edit { it[key] = mode.name }
                    mutable.update { it.copy(mode = mode, saving = false, writeFailed = false) }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { mutable.update { it.copy(mode = applied, saving = false, writeFailed = true) } }
            }
        }
    }
}
