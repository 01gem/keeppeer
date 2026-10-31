package com.gem.keeppeer

import android.app.Application
import android.net.Uri
import androidx.room.Room
import androidx.room.withTransaction
import com.gem.keeppeer.csv.*
import com.gem.keeppeer.data.local.AppDatabase
import com.gem.keeppeer.domain.PasswordEntry
import com.gem.keeppeer.security.VaultAccess
import com.gem.keeppeer.ui.export.*
import java.io.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, manifest = Config.NONE)
class CsvTransferTest {
    private lateinit var db: AppDatabase
    private lateinit var access: VaultAccess
    private val uri = Uri.parse("content://synthetic/document/new")
    private class Documents : CsvDocuments {
        var input: InputStream? = null
        var output: OutputStream? = null
        var reads = 0
        var writes = 0
        var deletes = 0
        var deletionFails = false
        var openFails = false
        override fun read(uri: Uri): InputStream? { reads++; if (openFails) throw SecurityException(); return input }
        override fun write(uri: Uri): OutputStream? { writes++; if (openFails) throw IOException(); return output }
        override fun deleteCreated(uri: Uri): Boolean { deletes++; if (deletionFails) throw IOException(); return true }
    }
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java).build()
        access = VaultAccess(1, db, ByteArray(32), true)
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }
    @After fun close() = runBlocking {
        access.revoke()
        access.close()
        Dispatchers.resetMain()
    }
    private suspend fun failed(block: suspend () -> Unit) {
        var failed = false
        try { block() } catch (_: Exception) { failed = true }
        assertTrue("Expected safe failure", failed)
    }
    @Test fun inputCloseFailureDiscardsParsedRowsAndPreservesSource() = runBlocking {
        val docs = Documents()
        docs.input = object : ByteArrayInputStream((VaultCsv.header.joinToString(",") + "\nT,,,,,,false,0,0").toByteArray()) {
            override fun close() { throw IOException("Synthetic close failure") }
        }
        failed { CsvTransfer(docs).read(access, uri) }
        assertTrue(docs.deletes == 0 && access.repository.exportCsv().isEmpty())
    }
    @Test fun revocationBeforeImportTransactionCommitRollsBackAllRows() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val work = async {
            try {
                access.work {
                    db.withTransaction {
                        val rows = listOf(PasswordEntry(title = "Synthetic", password = "", createdAt = 1, updatedAt = 1))
                        val preview = db.entryDao().previewCsv(rows)
                        db.entryDao().importCsv(rows, preview, DuplicatePolicy.SKIP, 10)
                        entered.complete(Unit)
                        awaitCancellation()
                    }
                }
                false
            } catch (_: CancellationException) { true }
        }
        withTimeout(10_000) { entered.await() }
        access.revoke()
        assertTrue(work.await())
        assertTrue(db.entryDao().csvCount() == 0L)
    }
    @Test fun nullAndDeniedInputNeverDeleteImportSource() = runBlocking {
        val docs = Documents()
        val transfer = CsvTransfer(docs)
        failed { transfer.read(access, uri) }
        docs.openFails = true
        failed { transfer.read(access, uri) }
        assertTrue(docs.deletes == 0 && docs.reads == 2)
    }
    @Test fun nullAndOpenOutputFailuresAttemptOnlyNewDocumentCleanup() = runBlocking {
        val docs = Documents()
        val transfer = CsvTransfer(docs)
        failed { transfer.export(access, uri) }
        docs.openFails = true
        docs.deletionFails = true
        failed { transfer.export(access, uri) }
        assertTrue(docs.deletes == 2)
    }
    @Test fun writeFlushAndCloseFailuresAreNeverExportSuccess() = runBlocking {
        for (kind in 0..2) {
            val docs = Documents()
            var closed = false
            docs.output = object : OutputStream() {
                override fun write(b: Int) { if (kind == 0) throw IOException() }
                override fun flush() { if (kind == 1) throw IOException() }
                override fun close() { closed = true; if (kind == 2) throw IOException() }
            }
            failed { CsvTransfer(docs).export(access, uri) }
            assertTrue(closed && docs.deletes == 1)
        }
    }
    @Test fun successfulExportClosesAndDoesNotDelete() = runBlocking {
        val docs = Documents()
        var closed = false
        val stream = object : ByteArrayOutputStream() { override fun close() { closed = true; super.close() } }
        docs.output = stream
        access.repository.create(PasswordEntry(title = "Synthetic", password = "  exact  "))
        CsvTransfer(docs).export(access, uri)
        assertTrue(closed && docs.deletes == 0)
        assertTrue(VaultCsv.parse(stream.toByteArray().inputStream()).single().password == "  exact  ")
    }
    @Test fun inFlightExportRevocationStopsChunksClosesAndCleansNewDocument() = runBlocking {
        val entered = CountDownLatch(1)
        val resume = CountDownLatch(1)
        var closed = false
        val docs = Documents()
        docs.output = object : OutputStream() {
            override fun write(b: Int) { }
            override fun write(b: ByteArray, off: Int, len: Int) {
                entered.countDown()
                check(resume.await(10, TimeUnit.SECONDS))
            }
            override fun close() { closed = true }
        }
        access.repository.create(PasswordEntry(title = "Synthetic", password = "synthetic".repeat(2000)))
        val work = async { failed { CsvTransfer(docs).export(access, uri) } }
        withContext(Dispatchers.IO) { check(entered.await(10, TimeUnit.SECONDS)) }
        access.revoke()
        resume.countDown()
        work.await()
        assertTrue(closed && docs.deletes == 1)
        failed { access.repository.exportCsv() }
    }
    @Test fun inFlightImportRevocationClosesReadAndDoesNotWriteOrDeleteSource() = runBlocking {
        val entered = CountDownLatch(1)
        val resume = CountDownLatch(1)
        var closed = false
        val docs = Documents()
        docs.input = object : ByteArrayInputStream((VaultCsv.header.joinToString(",") + "\nT,,,,,,false,0,0").toByteArray()) {
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                entered.countDown()
                check(resume.await(10, TimeUnit.SECONDS))
                return super.read(b, off, len)
            }
            override fun close() { closed = true }
        }
        val work = async { failed { CsvTransfer(docs).read(access, uri) } }
        withContext(Dispatchers.IO) { check(entered.await(10, TimeUnit.SECONDS)) }
        access.revoke()
        resume.countDown()
        work.await()
        assertTrue(closed && docs.deletes == 0 && docs.writes == 0)
    }
    @Test fun pickerCancellationStaleResultAndProcessStateLossDoNotStartIo() {
        val picker = CsvPickerModel()
        picker.request(CsvAction.IMPORT); picker.launched(); picker.result(null)
        assertTrue(picker.state.value == CsvPickerState())
        picker.request(CsvAction.EXPORT); picker.launched()
        access.revoke()
        picker.result(uri)
        assertTrue(picker.state.value.selection?.action == CsvAction.EXPORT)
        // No callback starts I/O or contains decrypted entries; a fresh session must confirm.
        assertTrue(picker.state.value.launching == null && picker.state.value.awaiting == null)
        val invalid = CsvPickerModel()
        invalid.request(CsvAction.EXPORT); invalid.launched()
        invalid.result(Uri.parse("file:///synthetic-private-file"))
        assertTrue(invalid.state.value.notice && invalid.state.value.selection == null)
        val recreatedProcess = CsvPickerModel()
        recreatedProcess.result(uri)
        assertTrue(recreatedProcess.state.value.notice && recreatedProcess.state.value.selection == null)
    }
    @Test fun viewModelPreventsDoubleSubmitAndDropsRowsAtRevoke() = runBlocking {
        val docs = Documents()
        docs.input = (VaultCsv.header.joinToString(",") + "\nT,,,,,,false,0,0").byteInputStream()
        val vm = CsvViewModel(access, CsvTransfer(docs))
        access.viewModelStore.put("csv-test", vm)
        withContext(Dispatchers.Main) { vm.selected(CsvSelection(CsvAction.IMPORT, uri)) {} }
        withTimeout(10_000) { while (vm.state.value.preview == null) delay(10) }
        withContext(Dispatchers.Main) { vm.submit(); vm.submit() }
        withTimeout(10_000) { while (vm.state.value.busy) delay(10) }
        assertTrue(vm.state.value.counts == ImportCounts(1, 0, 0))
        assertTrue(access.repository.exportCsv().size == 1)
        access.revoke()
        assertTrue(access.viewModelStore.keys().isEmpty())
    }
}
