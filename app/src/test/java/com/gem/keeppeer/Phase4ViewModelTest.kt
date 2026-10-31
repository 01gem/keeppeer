package com.gem.keeppeer

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.gem.keeppeer.data.repo.EntryRepository
import com.gem.keeppeer.domain.*
import com.gem.keeppeer.ui.edit.*
import com.gem.keeppeer.ui.list.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class Phase4ViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo = MetadataRepository()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }
    private fun row(id: Long, category: String?, favorite: Boolean = false) =
        EntrySummary(id, "École 100%_\\", null, null, category, favorite)

    @Test fun liveFiltersKeepAllCategoryChoicesAndOrphanIdentityUntilClear() = runTest(dispatcher) {
        repo.metadata.value = listOf(row(4, "All", true), row(3, "Uncategorized"), row(2, "Work"), row(1, null))
        val vm = EntryListViewModel(repo)
        runCurrent()
        vm.category(CategoryFilter.Named("All")); vm.search("école")
        assertEquals(listOf(4L), vm.state.value.entries.map { it.id })
        assertEquals(listOf("All", "Uncategorized", "Work"), vm.state.value.categories)
        repo.metadata.value = listOf(row(3, "Uncategorized"), row(2, "Work"), row(1, null))
        runCurrent()
        assertEquals(CategoryFilter.Named("All"), vm.state.value.category)
        assertTrue(vm.state.value.entries.isEmpty())
        assertFalse(vm.state.value.vaultEmpty)
        vm.clearFilters()
        assertEquals(3, vm.state.value.entries.size)
        assertFalse(vm.state.value.filtered)
        repo.metadata.value = emptyList(); runCurrent()
        assertTrue(vm.state.value.vaultEmpty)
    }
    @Test fun favoriteTapsAreSerializedPerRowAndFailuresStaySafe() = runTest(dispatcher) {
        repo.metadata.value = listOf(row(1, "Work"))
        val vm = EntryListViewModel(repo); runCurrent()
        repo.gate = CompletableDeferred()
        vm.favorite(1, true); vm.favorite(1, true); runCurrent()
        assertEquals(1, repo.favoriteWrites)
        assertEquals(setOf(1L), vm.state.value.updating)
        repo.gate!!.complete(Unit); runCurrent()
        assertTrue(vm.state.value.sections.favorites.single().isFavorite)
        assertTrue(vm.state.value.sections.remaining.isEmpty())
        repo.found = false
        vm.favorite(1, false); runCurrent()
        assertTrue(vm.state.value.favoriteError)
        assertTrue(vm.state.value.updating.isEmpty())
    }
    @Test fun generatorCancelRegenerateAndExplicitUseDoNotSave() = runTest(dispatcher) {
        var draw = 0
        val vm = EntryEditViewModel(repo, null) { PasswordGenerator.forTest { bound -> (draw++ % bound) } }
        vm.change { it.copy(title = "Synthetic", password = "original") }
        vm.openGenerator()
        val candidate = vm.state.value.candidate
        assertNotNull(candidate); assertEquals("original", vm.state.value.draft.password)
        vm.regenerate()
        assertNotNull(vm.state.value.candidate)
        assertEquals("original", vm.state.value.draft.password)
        vm.dismissGenerator()
        assertNull(vm.state.value.candidate)
        assertEquals("original", vm.state.value.draft.password)
        vm.openGenerator()
        val selected = vm.state.value.candidate
        vm.useGenerated()
        assertTrue(vm.state.value.draft.password == selected)
        assertNull(vm.state.value.candidate)
        assertEquals(0, repo.creates)
        vm.save(); runCurrent()
        assertEquals(1, repo.creates)
    }
    @Test fun invalidOptionsAndSecureFailureNeverReplaceOriginalDraft() = runTest(dispatcher) {
        val vm = EntryEditViewModel(repo, null) { PasswordGenerator.forTest { throw java.security.ProviderException() } }
        vm.change { it.copy(password = "original") }
        vm.openGenerator()
        assertTrue(vm.state.value.generationFailed); assertNull(vm.state.value.candidate)
        vm.useGenerated()
        assertEquals("original", vm.state.value.draft.password)
        vm.generatorOptions(GeneratorOptions(lowercase = false, uppercase = false, digits = false, symbols = false))
        vm.regenerate()
        assertTrue(vm.state.value.generationFailed)
        assertEquals("original", vm.state.value.draft.password)
    }
    @Test fun storeClearDropsSecretsAndRejectsStaleUseOrChanges() = runTest(dispatcher) {
        val store = ViewModelStore()
        val vm = EntryEditViewModel(repo, null)
        store.put("edit", vm)
        vm.change { it.copy(title = "Synthetic", password = "original") }
        vm.openGenerator()
        store.clear()
        assertNull(vm.state.value.candidate)
        assertEquals("", vm.state.value.draft.password)
        vm.useGenerated(); vm.openGenerator(); vm.change { it.copy(password = "stale") }; vm.save()
        assertNull(vm.state.value.candidate)
        assertEquals("", vm.state.value.draft.password)
        assertEquals(0, repo.creates)
    }

    private class MetadataRepository : EntryRepository {
        val metadata = MutableStateFlow<List<EntrySummary>>(emptyList())
        var gate: CompletableDeferred<Unit>? = null
        var favoriteWrites = 0
        var creates = 0
        var found = true
        override fun observeAll() = metadata
        override fun search(query: String) = metadata
        override suspend fun setFavorite(id: Long, favorite: Boolean): Boolean {
            favoriteWrites++; gate?.await()
            if (found) metadata.value = metadata.value.map { if (it.id == id) it.copy(isFavorite = favorite) else it }
            return found
        }
        override suspend fun create(entry: PasswordEntry): Long { creates++; return 1 }
        override suspend fun getById(id: Long): PasswordEntry? = null
        override fun observeById(id: Long): Flow<PasswordEntry?> = flowOf(null)
        override suspend fun update(entry: PasswordEntry) = false
        override suspend fun delete(id: Long) = false
        override suspend fun exportCsv(): List<PasswordEntry> = error("Unused")
        override suspend fun previewCsv(rows: List<PasswordEntry>): com.gem.keeppeer.csv.ImportPreview = error("Unused")
        override suspend fun importCsv(rows: List<PasswordEntry>, expected: com.gem.keeppeer.csv.ImportPreview,
            policy: com.gem.keeppeer.csv.DuplicatePolicy): com.gem.keeppeer.csv.ImportResult = error("Unused")
    }
}
