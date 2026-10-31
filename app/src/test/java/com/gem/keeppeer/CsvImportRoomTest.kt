package com.gem.keeppeer

import android.app.Application
import androidx.room.Room
import androidx.room.withTransaction
import com.gem.keeppeer.csv.*
import com.gem.keeppeer.data.local.AppDatabase
import com.gem.keeppeer.data.repo.EntryRepositoryImpl
import com.gem.keeppeer.domain.PasswordEntry
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, manifest = Config.NONE)
class CsvImportRoomTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: EntryRepositoryImpl
    private fun row(title: String = "Synthetic", username: String? = null, updated: Long = 20) =
        PasswordEntry(title = title, username = username, password = "  synthetic, \"\n雲  ", notes = "line1\r\nline2",
            createdAt = 10, updatedAt = updated)
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java).build()
        repo = EntryRepositoryImpl(db.entryDao(), clock = { 100 })
    }
    @After fun close() { db.close() }
    private suspend fun apply(rows: List<PasswordEntry>, policy: DuplicatePolicy): ImportCounts =
        (repo.importCsv(rows, repo.previewCsv(rows), policy) as ImportResult.Applied).counts

    @Test fun skipIsIdempotentIncludingDuplicatesWithinOneFile() = runBlocking {
        val input = listOf(row(), row(), row("Other"))
        assertTrue(repo.previewCsv(input).let { it.total == 3 && it.new == 2 && it.conflicting == 1 })
        assertTrue(apply(input, DuplicatePolicy.SKIP) == ImportCounts(2, 0, 1))
        assertTrue(apply(input, DuplicatePolicy.SKIP) == ImportCounts(0, 0, 3))
        val stored = repo.exportCsv()
        assertTrue(stored.size == 2 && stored.all { it.createdAt == 10L && it.updatedAt == 20L })
        assertTrue(stored.first().password == input.first().password)
    }
    @Test fun keepBothAlwaysCreatesDistinctLocalIdsAndPreservesDuplicateRows() = runBlocking {
        repo.create(row())
        assertTrue(apply(listOf(row(), row()), DuplicatePolicy.KEEP_BOTH) == ImportCounts(2, 0, 0))
        val entries = repo.exportCsv()
        assertTrue(entries.size == 3 && entries.map { it.id }.distinct().size == 3)
    }
    @Test fun updateTargetsLowestIdPreservesCreationAndUsesFileOrder() = runBlocking {
        val low = repo.create(row())
        val other = repo.create(row())
        val before = requireNotNull(repo.getById(low))
        val rows = listOf(row(updated = 200).copy(password = "first"), row(updated = 150).copy(password = "last"))
        assertTrue(apply(rows, DuplicatePolicy.UPDATE) == ImportCounts(0, 2, 0))
        val updated = requireNotNull(repo.getById(low))
        assertTrue(updated.id == low && updated.createdAt == before.createdAt)
        assertTrue(updated.password == "last" && updated.updatedAt == 201L)
        assertTrue(repo.getById(other)?.updatedAt == 100L)
    }
    @Test fun updateSameFileNewRowKeepsImportedCreationAndAdvancesEvenBackwardClock() = runBlocking {
        assertTrue(apply(listOf(row(updated = 500), row(updated = 30)), DuplicatePolicy.UPDATE) == ImportCounts(1, 1, 0))
        assertTrue(repo.exportCsv().single().let { it.createdAt == 10L && it.updatedAt == 501L })
    }
    @Test fun matchingIsCaseAndWhitespaceSensitiveWithNullEmptyUsernameNormalization() = runBlocking {
        repo.create(row(username = ""))
        val rows = listOf(row(), row("synthetic"), row(" Synthetic"), row(username = " "))
        val preview = repo.previewCsv(rows)
        assertTrue(preview.new == 3 && preview.conflicting == 1)
        assertTrue(apply(rows, DuplicatePolicy.SKIP) == ImportCounts(3, 0, 1))
    }
    @Test fun changedDatabaseRequiresRefreshedConfirmationEvenWhenCountsDoNotChange() = runBlocking {
        val id = repo.create(row())
        val input = listOf(row())
        val before = repo.previewCsv(input)
        repo.update(requireNotNull(repo.getById(id)).copy(password = "changed"))
        val result = repo.importCsv(input, before, DuplicatePolicy.UPDATE)
        assertTrue(result is ImportResult.Changed)
        assertTrue(repo.getById(id)?.password == "changed")
        val refreshed = (result as ImportResult.Changed).preview
        assertTrue(refreshed.conflicting == before.conflicting && refreshed.revision != before.revision)
        assertTrue(repo.importCsv(input, refreshed, DuplicatePolicy.UPDATE) is ImportResult.Applied)
    }
    @Test fun newExistingMatchAfterPreviewDoesNotSilentlyChangeTargets() = runBlocking {
        val input = listOf(row())
        val preview = repo.previewCsv(input)
        repo.create(row())
        val result = repo.importCsv(input, preview, DuplicatePolicy.KEEP_BOTH)
        assertTrue(result is ImportResult.Changed && result.preview.conflicting == 1)
        assertTrue(repo.exportCsv().size == 1)
    }
    @Test fun injectedSqlWriteFailureRollsBackEarlierRows() = runBlocking {
        db.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER synthetic_failure BEFORE INSERT ON entries WHEN NEW.title = 'Fail' BEGIN SELECT RAISE(ABORT, 'synthetic write failure'); END")
        var failed = false
        try { apply(listOf(row("First"), row("Fail")), DuplicatePolicy.KEEP_BOTH) } catch (_: Exception) { failed = true }
        assertTrue(failed)
        assertTrue(repo.exportCsv().isEmpty())
    }
    @Test fun cancellationBeforeOuterCommitRollsBackCompletedNestedImport() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val work = launch(Dispatchers.IO) {
            db.withTransaction {
                apply(listOf(row("Before cancellation")), DuplicatePolicy.SKIP)
                entered.complete(Unit)
                awaitCancellation()
            }
        }
        entered.await()
        work.cancelAndJoin()
        assertTrue(repo.exportCsv().isEmpty())
    }
    @Test fun overflowFailsWholeImportAndOrdinaryUpdateDoesNotCoerceToReal() = runBlocking {
        apply(listOf(row(updated = Long.MAX_VALUE)), DuplicatePolicy.SKIP)
        val before = repo.exportCsv().single()
        var failed = false
        try { apply(listOf(row("New"), row()), DuplicatePolicy.UPDATE) } catch (_: ArithmeticException) { failed = true }
        assertTrue(failed)
        assertTrue(repo.exportCsv().size == 1 && repo.exportCsv().single() == before)
        failed = false
        try { repo.update(before) } catch (_: ArithmeticException) { failed = true }
        assertTrue(failed && repo.getById(before.id) == before)
    }
}
