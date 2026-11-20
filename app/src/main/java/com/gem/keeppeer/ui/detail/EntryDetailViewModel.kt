package com.gem.keeppeer.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gem.keeppeer.data.repo.EntryRepository
import com.gem.keeppeer.domain.PasswordEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EntryDetailState(
    val entry: PasswordEntry? = null,
    val loading: Boolean = true,
    val deleting: Boolean = false,
    val deleted: Boolean = false,
    val error: String? = null,
    val favoriteUpdating: Boolean = false,
    val favoriteError: Boolean = false,
)

class EntryDetailViewModel(
    private val repository: EntryRepository,
    private val entryId: Long,
) : ViewModel() {
    private val mutableState = MutableStateFlow(EntryDetailState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null

    init { retry() }

    fun retry() {
        observation?.cancel()
        mutableState.value = EntryDetailState()
        observation = viewModelScope.launch {
            try {
                repository.observeById(entryId).collect { entry ->
                    mutableState.update { it.copy(entry = entry, loading = false, error = null) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, error = "Could not load entry.") }
            }
        }
    }

    override fun onCleared() {
        mutableState.value = EntryDetailState()
        super.onCleared()
    }

    // Mirrors the list's favorite(): one field-only write at a time; repeated taps do not queue.
    fun favorite(value: Boolean) {
        val current = state.value
        if (current.loading || current.deleting || current.deleted || current.entry == null || current.favoriteUpdating) return
        mutableState.update { it.copy(favoriteUpdating = true, favoriteError = false) }
        viewModelScope.launch {
            try {
                val found = repository.setFavorite(entryId, value)
                mutableState.update { it.copy(favoriteError = !found) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.update { it.copy(favoriteError = true) } }
            finally { mutableState.update { it.copy(favoriteUpdating = false) } }
        }
    }

    fun delete() {
        if (state.value.loading || state.value.deleting || state.value.deleted || state.value.entry == null) return
        mutableState.update { it.copy(deleting = true, error = null) }
        viewModelScope.launch {
            try {
                repository.delete(entryId) // Already deleted also reaches the safe list destination.
                observation?.cancel()
                mutableState.value = EntryDetailState(loading = false, deleted = true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(deleting = false, error = "Could not delete entry. Please retry.") }
            }
        }
    }
}
