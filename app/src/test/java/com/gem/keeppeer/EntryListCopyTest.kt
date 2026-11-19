package com.gem.keeppeer

import com.gem.keeppeer.data.repo.EntryRepository
import com.gem.keeppeer.domain.*
import com.gem.keeppeer.ui.list.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

/** Synthetic credentials only. Same dispatcher/fake-repository pattern as Phase4ViewModelTest. */
@OptIn(ExperimentalCoroutinesApi::class)
class EntryListCopyTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo = CopyRepository()
    private val copied = mutableListOf<String>()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    private fun entry(id: Long, password: String) = PasswordEntry(id = id, title = "Synthetic $id", password = password)
    private fun seed(vararg entries: PasswordEntry, stored: Boolean = true) {
        repo.entries = if (stored) entries.associateBy { it.id } else emptyMap()
        repo.metadata.value = entries.map { EntrySummary(it.id, it.title, it.username, it.url, it.category, it.isFavorite) }
    }
    private fun TestScope.listVm() = EntryListViewModel(repo).also { runCurrent() }

    @Test fun copiesExactPasswordOnceAndReportsCopied() = runTest(dispatcher) {
        seed(entry(1, SYNTHETIC))
        val vm = listVm()
        vm.copyPassword(1) { copied += it; true }; runCurrent()
        assertEquals(listOf(SYNTHETIC), copied)
        assertEquals(CopyStatus.COPIED, vm.state.value.copyStatus)
        assertEquals(1L, vm.state.value.copyStatusId)
        assertTrue(vm.state.value.copying.isEmpty())
        vm.search("x")
        assertNull(vm.state.value.copyStatus)
        assertNull(vm.state.value.copyStatusId)
    }
    @Test fun copiedMessageExpiresWithClipboard() = runTest(dispatcher) {
        seed(entry(1, SYNTHETIC))
        val vm = listVm()
        vm.copyPassword(1) { copied += it; true }; runCurrent()
        assertEquals(CopyStatus.COPIED, vm.state.value.copyStatus)
        advanceTimeBy(30_001); runCurrent()
        assertNull(vm.state.value.copyStatus)
        assertNull(vm.state.value.copyStatusId)
    }
    @Test fun clipboardRefusalReportsFailed() = runTest(dispatcher) {
        seed(entry(1, SYNTHETIC))
        val vm = listVm()
        vm.copyPassword(1) { copied += it; false }; runCurrent()
        assertEquals(1, copied.size)
        assertEquals(CopyStatus.FAILED, vm.state.value.copyStatus)
        assertTrue(vm.state.value.copying.isEmpty())
    }
    @Test fun unknownIdIsNoOpAndDeletedEntryReportsMissing() = runTest(dispatcher) {
        seed(entry(1, SYNTHETIC), stored = false)
        val vm = listVm()
        vm.copyPassword(99) { copied += it; true }; runCurrent()
        assertEquals(0, repo.reads)
        assertNull(vm.state.value.copyStatus)
        vm.copyPassword(1) { copied += it; true }; runCurrent()
        assertTrue(copied.isEmpty())
        assertEquals(CopyStatus.MISSING, vm.state.value.copyStatus)
        assertTrue(vm.state.value.copying.isEmpty())
    }
    @Test fun emptyPasswordIsNotCopied() = runTest(dispatcher) {
        seed(entry(1, ""))
        val vm = listVm()
        vm.copyPassword(1) { copied += it; true }; runCurrent()
        assertTrue(copied.isEmpty())
        assertEquals(CopyStatus.EMPTY, vm.state.value.copyStatus)
    }
    @Test fun secondTapWhileInFlightIsIgnored() = runTest(dispatcher) {
        seed(entry(1, SYNTHETIC))
        val vm = listVm()
        repo.gate = CompletableDeferred()
        vm.copyPassword(1) { copied += it; true }; vm.copyPassword(1) { copied += it; true }; runCurrent()
        assertEquals(1, repo.reads)
        assertEquals(setOf(1L), vm.state.value.copying)
        repo.gate!!.complete(Unit); runCurrent()
        assertEquals(1, copied.size)
        assertTrue(vm.state.value.copying.isEmpty())
    }
    @Test fun stateNeverHoldsCopiedPassword() = runTest(dispatcher) {
        seed(entry(1, SYNTHETIC))
        val vm = listVm()
        vm.copyPassword(1) { copied += it; true }; runCurrent()
        assertEquals(CopyStatus.COPIED, vm.state.value.copyStatus)
        val dump = vm.state.value.toString()
        assertFalse(dump.contains(SYNTHETIC))
        assertFalse(dump.contains(SYNTHETIC.trim()))
    }

    private class CopyRepository : EntryRepository {
        val metadata = MutableStateFlow<List<EntrySummary>>(emptyList())
        var entries: Map<Long, PasswordEntry> = emptyMap()
        var gate: CompletableDeferred<Unit>? = null
        var reads = 0
        override fun observeAll() = metadata
        override fun search(query: String) = metadata
        override suspend fun getById(id: Long): PasswordEntry? { reads++; gate?.await(); return entries[id] }
        override fun observeById(id: Long): Flow<PasswordEntry?> = flowOf(null)
        override suspend fun create(entry: PasswordEntry): Long = error("Unused")
        override suspend fun update(entry: PasswordEntry) = false
        override suspend fun delete(id: Long) = false
        override suspend fun setFavorite(id: Long, favorite: Boolean) = false
        override suspend fun exportCsv(): List<PasswordEntry> = error("Unused")
        override suspend fun previewCsv(rows: List<PasswordEntry>): com.gem.keeppeer.csv.ImportPreview = error("Unused")
        override suspend fun importCsv(rows: List<PasswordEntry>, expected: com.gem.keeppeer.csv.ImportPreview,
            policy: com.gem.keeppeer.csv.DuplicatePolicy): com.gem.keeppeer.csv.ImportResult = error("Unused")
    }

    private companion object {
        // Synthetic only: leading/trailing spaces, accented Latin, CJK and a supplementary-plane emoji.
        const val SYNTHETIC = "  syn théé 密码 🔑 pw  "
    }
}
