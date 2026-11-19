package com.gem.keeppeer.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gem.keeppeer.data.repo.EntryRepository
import com.gem.keeppeer.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class CopyStatus { COPIED, FAILED, MISSING, EMPTY }

data class EntryListState(
    val query: String = "",
    val category: CategoryFilter = CategoryFilter.All,
    val favoritesOnly: Boolean = false,
    val categories: List<String> = emptyList(),
    val sections: EntrySections = EntrySections(emptyList(), emptyList()),
    val vaultEmpty: Boolean = true,
    val loading: Boolean = true,
    val error: String? = null,
    val updating: Set<Long> = emptySet(),
    val favoriteError: Boolean = false,
    val copying: Set<Long> = emptySet(),
    val copyStatus: CopyStatus? = null,
    val copyStatusId: Long? = null,
) {
    val entries get() = sections.entries
    val filtered get() = query.isNotEmpty() || category != CategoryFilter.All || favoritesOnly
}

class EntryListViewModel(private val repository: EntryRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(EntryListState())
    val state = mutableState.asStateFlow()
    private var metadata: List<EntrySummary> = emptyList()
    private var observation: Job? = null
    private var cleared = false
    private var copiedExpiry: Job? = null

    init { retry() }

    private fun applyFilters() {
        mutableState.update { it.copy(
            categories = EntryFilters.categories(metadata),
            sections = EntryFilters.sections(metadata, it.query, it.category, it.favoritesOnly),
            vaultEmpty = metadata.isEmpty(),
        ) }
    }
    fun search(query: String) { if (!cleared) { mutableState.update { it.copy(query = query, copyStatus = null, copyStatusId = null) }; applyFilters() } }
    fun category(value: CategoryFilter) { if (!cleared) { mutableState.update { it.copy(category = value, copyStatus = null, copyStatusId = null) }; applyFilters() } }
    fun favoritesOnly(value: Boolean) { if (!cleared) { mutableState.update { it.copy(favoritesOnly = value, copyStatus = null, copyStatusId = null) }; applyFilters() } }
    fun clearFilters() {
        if (cleared) return
        mutableState.update { it.copy(query = "", category = CategoryFilter.All, favoritesOnly = false,
            copyStatus = null, copyStatusId = null) }
        applyFilters()
    }
    fun retry() {
        if (cleared) return
        observation?.cancel()
        mutableState.update { it.copy(loading = true, error = null) }
        observation = viewModelScope.launch {
            try {
                repository.observeAll().collect { entries ->
                    metadata = entries
                    mutableState.update { it.copy(loading = false, error = null) }
                    applyFilters()
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.update { it.copy(loading = false, error = "load_entries") } }
        }
    }
    // Disable each row until its field-only write completes; repeated taps do not queue stale toggles.
    fun favorite(id: Long, value: Boolean) {
        if (cleared || id in state.value.updating || metadata.none { it.id == id }) return
        mutableState.update { it.copy(updating = it.updating + id, favoriteError = false,
            copyStatus = null, copyStatusId = null) }
        viewModelScope.launch {
            try {
                val found = repository.setFavorite(id, value)
                mutableState.update { it.copy(favoriteError = !found) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.update { it.copy(favoriteError = true) } }
            finally { mutableState.update { it.copy(updating = it.updating - id) } }
        }
    }
    // Password is read only on tap and handed straight to the clipboard lambda; it never enters state.
    fun copyPassword(id: Long, copy: (String) -> Boolean) {
        if (cleared || id in state.value.copying || metadata.none { it.id == id }) return
        mutableState.update { it.copy(copying = it.copying + id, copyStatus = null, copyStatusId = null) }
        viewModelScope.launch {
            try {
                val entry = repository.getById(id)
                val status = when {
                    entry == null -> CopyStatus.MISSING
                    entry.password.isEmpty() -> CopyStatus.EMPTY
                    copy(entry.password) -> CopyStatus.COPIED
                    else -> CopyStatus.FAILED
                }
                mutableState.update { it.copy(copyStatus = status, copyStatusId = id) }
                if (status == CopyStatus.COPIED) expireCopiedMessage(id)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.update { it.copy(copyStatus = CopyStatus.FAILED, copyStatusId = id) } }
            finally { mutableState.update { it.copy(copying = it.copying - id) } }
        }
    }
    // SensitiveClipboard clears its clip after ~30 s; drop the "Copied" message at the same time.
    private fun expireCopiedMessage(id: Long) {
        copiedExpiry?.cancel()
        copiedExpiry = viewModelScope.launch {
            delay(30_000)
            mutableState.update {
                if (it.copyStatusId == id && it.copyStatus == CopyStatus.COPIED) it.copy(copyStatus = null, copyStatusId = null) else it
            }
        }
    }
    override fun onCleared() {
        cleared = true
        metadata = emptyList()
        mutableState.value = EntryListState()
        super.onCleared()
    }
}
