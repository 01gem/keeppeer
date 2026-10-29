package com.gem.keeppeer.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gem.keeppeer.data.repo.EntryRepository
import com.gem.keeppeer.domain.EntrySummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EntryListState(
    val query: String = "",
    val entries: List<EntrySummary> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

class EntryListViewModel(private val repository: EntryRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(EntryListState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null

    init { search("") }

    fun search(query: String) {
        observation?.cancel()
        mutableState.value = EntryListState(query = query)
        observation = viewModelScope.launch {
            try {
                repository.search(query).collect { entries ->
                    mutableState.update { it.copy(entries = entries, loading = false, error = null) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, error = "Could not load entries.") }
            }
        }
    }

    override fun onCleared() {
        mutableState.value = EntryListState()
        super.onCleared()
    }

    fun retry() = search(state.value.query)
}
