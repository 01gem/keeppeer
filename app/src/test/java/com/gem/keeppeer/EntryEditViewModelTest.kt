package com.gem.keeppeer

import com.gem.keeppeer.data.repo.EntryRepository
import com.gem.keeppeer.domain.EntrySummary
import com.gem.keeppeer.domain.PasswordEntry
import com.gem.keeppeer.ui.edit.EntryEditViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EntryEditViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeRepository()

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun validatesTitleAndPreventsDuplicateSavesWhileKeepingExactDraft() = runTest(dispatcher) {
        val vm = EntryEditViewModel(repository, null)
        vm.save()
        assertTrue(vm.state.value.titleError)
        assertEquals(0, repository.creates)
        vm.change { it.copy(title = "Synthetic", password = "  synthetic  ", notes = "\n雲\n") }
        repository.saveGate = CompletableDeferred()
        vm.save()
        vm.save()
        runCurrent()
        assertEquals(1, repository.creates)
        assertTrue(vm.state.value.saving)
        vm.change { it.copy(title = "Unexpected") }
        assertEquals("Synthetic", vm.state.value.draft.title)
        repository.saveGate?.complete(Unit)
        advanceUntilIdle()
        assertEquals("  synthetic  ", repository.lastWrite?.password)
        assertEquals("\n雲\n", repository.lastWrite?.notes)
        assertEquals(42L, vm.state.value.savedId)
        assertEquals("", vm.state.value.draft.password)
        vm.save()
        assertEquals(1, repository.creates)
    }

    @Test fun draftChangesDoNotWriteAndNewViewModelRestoresStoredEntryOnly() = runTest(dispatcher) {
        repository.entry = PasswordEntry(id = 7, title = "Saved", password = "synthetic-saved")
        val vm = EntryEditViewModel(repository, 7)
        advanceUntilIdle()
        vm.change { it.copy(password = "synthetic-unsaved", notes = "draft") }
        assertEquals(0, repository.updates)
        assertEquals("synthetic-unsaved", vm.state.value.draft.password)
        // Process recreation creates a new ViewModel; no saved draft is supplied.
        val recreated = EntryEditViewModel(repository, 7)
        advanceUntilIdle()
        assertEquals("synthetic-saved", recreated.state.value.draft.password)
        assertEquals("", recreated.state.value.draft.notes)
    }

    @Test fun missingAndDeletedEntriesCannotBeSavedAsNew() = runTest(dispatcher) {
        val missing = EntryEditViewModel(repository, 7)
        advanceUntilIdle()
        assertTrue(missing.state.value.missing)
        missing.save()
        assertEquals(0, repository.creates)
        repository.entry = PasswordEntry(id = 7, title = "Saved", password = "")
        val editing = EntryEditViewModel(repository, 7)
        advanceUntilIdle()
        repository.entry = null
        editing.save()
        advanceUntilIdle()
        assertTrue(editing.state.value.missing)
        assertFalse(editing.state.value.saving)
        assertEquals(0, repository.creates)
    }

    @Test fun loadAndSaveErrorsAreRecoverableWithoutLeakingExceptionText() = runTest(dispatcher) {
        repository.fail = true
        val vm = EntryEditViewModel(repository, 7)
        advanceUntilIdle()
        assertTrue(vm.state.value.loadFailed)
        vm.save()
        assertEquals(0, repository.updates)
        repository.fail = false
        repository.entry = PasswordEntry(id = 7, title = "Saved", password = "synthetic")
        vm.load()
        advanceUntilIdle()
        vm.change { it.copy(title = "Edited") }
        repository.fail = true
        vm.save()
        advanceUntilIdle()
        assertFalse(vm.state.value.saving)
        assertNotNull(vm.state.value.error)
        assertFalse(vm.state.value.error.orEmpty().contains("private exception"))
        assertEquals("Edited", vm.state.value.draft.title)
        repository.fail = false
        vm.save()
        advanceUntilIdle()
        assertEquals(7L, vm.state.value.savedId)
    }

    private class FakeRepository : EntryRepository {
        var entry: PasswordEntry? = null
        var creates = 0
        var updates = 0
        var fail = false
        var lastWrite: PasswordEntry? = null
        var saveGate: CompletableDeferred<Unit>? = null
        private fun checkFailure() { if (fail) error("private exception") }
        override fun observeAll(): Flow<List<EntrySummary>> = flowOf(emptyList())
        override fun search(query: String) = observeAll()
        override fun observeById(id: Long) = flowOf(entry)
        override suspend fun getById(id: Long): PasswordEntry? { checkFailure(); return entry }
        override suspend fun create(entry: PasswordEntry): Long {
            creates++
            checkFailure()
            saveGate?.await()
            lastWrite = entry
            return 42
        }
        override suspend fun update(entry: PasswordEntry): Boolean {
            updates++
            checkFailure()
            if (this.entry == null) return false
            lastWrite = entry
            return true
        }
        override suspend fun delete(id: Long): Boolean = false
    }
}
