package com.gem.keeppeer.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gem.keeppeer.data.repo.EntryRepository
import com.gem.keeppeer.domain.PasswordEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Memory only: never place drafts in SavedStateHandle or Compose saveable state.
data class EntryDraft(
    val title: String = "",
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val notes: String = "",
    val category: String = "",
    val isFavorite: Boolean = false,
)

data class EntryEditState(
    val draft: EntryDraft = EntryDraft(),
    val loading: Boolean = false,
    val loadFailed: Boolean = false,
    val saving: Boolean = false,
    val missing: Boolean = false,
    val titleError: Boolean = false,
    val error: String? = null,
    val savedId: Long? = null,
)

class EntryEditViewModel(
    private val repository: EntryRepository,
    private val entryId: Long?,
) : ViewModel() {
    private val mutableState = MutableStateFlow(EntryEditState(loading = entryId != null))
    val state = mutableState.asStateFlow()

    init { if (entryId != null) load() }

    fun load() {
        if (state.value.saving) return
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val entry = repository.getById(requireNotNull(entryId))
                mutableState.value = if (entry == null) EntryEditState(missing = true)
                else EntryEditState(draft = EntryDraft(
                    entry.title, entry.username.orEmpty(), entry.password, entry.url.orEmpty(),
                    entry.notes.orEmpty(), entry.category.orEmpty(), entry.isFavorite,
                ))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, loadFailed = true, error = "Could not load entry.") }
            }
        }
    }

    fun change(transform: (EntryDraft) -> EntryDraft) {
        if (state.value.loading || state.value.saving || state.value.savedId != null || state.value.missing) return
        mutableState.update { it.copy(draft = transform(it.draft), titleError = false, error = null) }
    }

    override fun onCleared() {
        mutableState.value = EntryEditState()
        super.onCleared()
    }

    fun save() {
        val current = state.value
        if (current.loading || current.loadFailed || current.saving || current.missing || current.savedId != null) return
        if (current.draft.title.isBlank()) {
            mutableState.update { it.copy(titleError = true) }
            return
        }
        mutableState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                val draft = current.draft
                val entry = PasswordEntry(
                    id = entryId ?: 0L, title = draft.title,
                    username = draft.username.ifEmpty { null }, password = draft.password,
                    url = draft.url.ifEmpty { null }, notes = draft.notes,
                    category = draft.category.ifEmpty { null }, isFavorite = draft.isFavorite,
                )
                val id = if (entryId == null) repository.create(entry) else {
                    if (!repository.update(entry)) {
                        mutableState.update { it.copy(saving = false, missing = true, draft = EntryDraft()) }
                        return@launch
                    }
                    entryId
                }
                // Clear the draft immediately after a successful write.
                mutableState.value = EntryEditState(savedId = id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(saving = false, error = "Could not save entry. Please retry.") }
            }
        }
    }
}
