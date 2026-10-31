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
    val generatorOpen: Boolean = false,
    val generatorOptions: com.gem.keeppeer.domain.GeneratorOptions = com.gem.keeppeer.domain.GeneratorOptions(),
    val candidate: String? = null,
    val generationFailed: Boolean = false,
)

class EntryEditViewModel(
    private val repository: EntryRepository,
    private val entryId: Long?,
    private val generator: () -> com.gem.keeppeer.domain.PasswordGenerator = { com.gem.keeppeer.domain.PasswordGenerator() },
) : ViewModel() {
    private val mutableState = MutableStateFlow(EntryEditState(loading = entryId != null))
    val state = mutableState.asStateFlow()

    private var cleared = false
    private fun editable() = !cleared && !state.value.loading && !state.value.loadFailed &&
        !state.value.saving && !state.value.missing && state.value.savedId == null
    init { if (entryId != null) load() }

    fun openGenerator() {
        if (!editable()) return
        mutableState.update { it.copy(generatorOpen = true) }
        regenerate()
    }
    fun dismissGenerator() {
        mutableState.update { it.copy(generatorOpen = false, candidate = null, generationFailed = false) }
    }
    fun generatorOptions(options: com.gem.keeppeer.domain.GeneratorOptions) {
        if (!editable() || !state.value.generatorOpen) return
        mutableState.update { it.copy(generatorOptions = options, candidate = null, generationFailed = false) }
    }
    fun regenerate() {
        if (!editable() || !state.value.generatorOpen) return
        try {
            val candidate = generator().generate(state.value.generatorOptions)
            mutableState.update { it.copy(candidate = candidate, generationFailed = false) }
        } catch (_: Exception) {
            mutableState.update { it.copy(candidate = null, generationFailed = true) }
        }
    }
    fun useGenerated() {
        if (!editable() || !state.value.generatorOpen) return
        val candidate = state.value.candidate ?: return
        change { it.copy(password = candidate) }
        dismissGenerator()
    }

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
        if (!editable()) return
        mutableState.update { it.copy(draft = transform(it.draft), titleError = false, error = null) }
    }

    override fun onCleared() {
        cleared = true
        mutableState.value = EntryEditState()
        super.onCleared()
    }

    fun save() {
        if (cleared) return
        val current = state.value
        if (current.loading || current.loadFailed || current.saving || current.missing || current.savedId != null) return
        if (current.draft.title.isBlank()) {
            mutableState.update { it.copy(titleError = true) }
            return
        }
        if (cleared) return
        mutableState.update { it.copy(saving = true, error = null, generatorOpen = false, candidate = null) }
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
