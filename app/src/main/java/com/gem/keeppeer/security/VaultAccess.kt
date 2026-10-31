package com.gem.keeppeer.security

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.gem.keeppeer.data.local.AppDatabase
import com.gem.keeppeer.data.repo.EntryRepository
import com.gem.keeppeer.data.repo.EntryRepositoryImpl
import com.gem.keeppeer.domain.EntrySummary
import com.gem.keeppeer.domain.PasswordEntry
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

/** Each unlock owns a new repository, work tree and UI store. Revocation is irreversible. */
class VaultAccess internal constructor(
    val id: Long, internal val database: AppDatabase, internal val key: ByteArray,
    val passwordAuthenticated: Boolean,
) : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.IO)
    @Volatile private var revoked = false
    private val delegate = EntryRepositoryImpl(database.entryDao())
    suspend fun <T> work(block: suspend () -> T): T {
        check(!revoked) { "Vault locked" }
        val work = scope.async { check(!revoked); block() }
        return try { work.await() } finally {
            withContext(NonCancellable) { work.cancelAndJoin() }
        }
    }
    private fun <T> observe(source: () -> Flow<T>): Flow<T> = channelFlow {
        check(!revoked) { "Vault locked" }
        val work = scope.launch {
            try { source().collect { send(it) } }
            catch (failure: Exception) { close(failure) }
            finally { close() }
        }
        work.invokeOnCompletion { close(it) }
        awaitClose { work.cancel() }
    }.buffer(0)
    val repository: EntryRepository = object : EntryRepository {
        override fun observeAll(): Flow<List<EntrySummary>> = observe { delegate.observeAll() }
        override fun search(query: String): Flow<List<EntrySummary>> = observe { delegate.search(query) }
        override suspend fun getById(id: Long) = work { delegate.getById(id) }
        override fun observeById(id: Long) = observe { delegate.observeById(id) }
        override suspend fun create(entry: PasswordEntry) = work { delegate.create(entry) }
        override suspend fun update(entry: PasswordEntry) = work { delegate.update(entry) }
        override suspend fun delete(id: Long) = work { delegate.delete(id) }
        override suspend fun setFavorite(id: Long, favorite: Boolean) = work { delegate.setFavorite(id, favorite) }
        override suspend fun exportCsv() = work { delegate.exportCsv() }
        override suspend fun previewCsv(rows: List<PasswordEntry>) = work { delegate.previewCsv(rows) }
        override suspend fun importCsv(rows: List<PasswordEntry>, expected: com.gem.keeppeer.csv.ImportPreview,
            policy: com.gem.keeppeer.csv.DuplicatePolicy) = work { delegate.importCsv(rows, expected, policy) }
    }
    /** Called on main before publishing locked; clears navigation and all destination VMs. */
    internal fun revoke() {
        revoked = true
        job.cancel()
        viewModelStore.clear()
    }
    internal suspend fun close() {
        job.join()
        try { database.close() } finally { key.fill(0) }
    }
}
