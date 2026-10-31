package com.gem.keeppeer.csv

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import com.gem.keeppeer.security.VaultAccess
import kotlinx.coroutines.*
import java.io.InputStream
import java.io.OutputStream
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

interface CsvDocuments {
    fun read(uri: Uri): InputStream?
    fun write(uri: Uri): OutputStream?
    fun deleteCreated(uri: Uri): Boolean
}

class SafCsvDocuments(private val resolver: ContentResolver) : CsvDocuments {
    override fun read(uri: Uri) = resolver.openInputStream(uri)
    override fun write(uri: Uri) = resolver.openOutputStream(uri, "wt")
    override fun deleteCreated(uri: Uri): Boolean {
        val flags = resolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_FLAGS), null, null, null)
            ?.use { if (it.moveToFirst()) it.getInt(0) else 0 } ?: 0
        if (flags and DocumentsContract.Document.FLAG_SUPPORTS_DELETE == 0) return false
        return DocumentsContract.deleteDocument(resolver, uri)
    }
}

/** Every stream and decrypted read belongs to the same revocable VaultAccess work lifetime. */
class CsvTransfer(private val documents: CsvDocuments) {
    suspend fun read(access: VaultAccess, uri: Uri) = access.work {
        require(uri.scheme == "content") { "SAF document required" }
        val stream = documents.read(uri) ?: throw java.io.IOException("Input unavailable")
        useRevocable(stream) { VaultCsv.parse(stream) }
    }

    suspend fun export(access: VaultAccess, uri: Uri) {
        require(uri.scheme == "content") { "SAF document required" }
        var complete = false
        try {
            access.work {
                val entries = access.repository.exportCsv()
                currentCoroutineContext().ensureActive()
                val stream = documents.write(uri) ?: throw java.io.IOException("Output unavailable")
                useRevocable(stream) { VaultCsv.write(stream, entries); stream.flush() }
                currentCoroutineContext().ensureActive()
            }
            complete = true
        } finally {
            if (!complete) discardCreated(uri)
        }
    }

    /** Cancellation also asks the provider to close a blocked stream on a separate IO worker. */
    private suspend fun <T> useRevocable(stream: Closeable, block: suspend () -> T): T = coroutineScope {
        val closed = AtomicBoolean(false)
        val watcher = launch(Dispatchers.IO, start = CoroutineStart.UNDISPATCHED) {
            try { awaitCancellation() }
            finally {
                if (closed.compareAndSet(false, true)) {
                    try { stream.close() } catch (_: Exception) { /* cancelled; cannot report success */ }
                }
            }
        }
        try { block() }
        finally {
            try { if (closed.compareAndSet(false, true)) stream.close() }
            finally { withContext(NonCancellable) { watcher.cancelAndJoin() } }
        }
    }

    /** Only called with a URI returned by our CREATE_DOCUMENT, never an import source. */
    suspend fun discardCreated(uri: Uri): Boolean = withContext(NonCancellable + Dispatchers.IO) {
        if (uri.scheme != "content") false
        else try { documents.deleteCreated(uri) } catch (_: Exception) { false }
    }
}
