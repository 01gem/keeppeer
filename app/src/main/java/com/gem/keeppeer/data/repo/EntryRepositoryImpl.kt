package com.gem.keeppeer.data.repo

import com.gem.keeppeer.data.local.EntryDao
import com.gem.keeppeer.domain.EntrySummary
import com.gem.keeppeer.domain.PasswordEntry
import com.gem.keeppeer.domain.toDomain
import com.gem.keeppeer.domain.toEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class EntryRepositoryImpl(
    private val entryDao: EntryDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis,
) : EntryRepository {
    override fun observeAll(): Flow<List<EntrySummary>> =
        entryDao.observeSummaries().flowOn(ioDispatcher)

    // SQLite's built-in LIKE/NOCASE only folds ASCII. Filter the metadata-only
    // Room projection in Kotlin for Unicode case matching and literal %, _ and \.
    // This is intentional for a small offline vault; no secrets enter list state.
    override fun search(query: String): Flow<List<EntrySummary>> =
        entryDao.observeSummaries().map { entries ->
            val term = query.trim()
            entries.filter { entry ->
                term.isEmpty() || listOf(entry.title, entry.username, entry.url, entry.category)
                    .any { it?.contains(term, ignoreCase = true) == true }
            }
        }.flowOn(ioDispatcher)

    override suspend fun getById(id: Long): PasswordEntry? = withContext(ioDispatcher) {
        entryDao.getById(id)?.toDomain()
    }

    override fun observeById(id: Long): Flow<PasswordEntry?> =
        entryDao.observeById(id).map { it?.toDomain() }.flowOn(ioDispatcher)

    override suspend fun create(entry: PasswordEntry): Long = withContext(ioDispatcher) {
        require(entry.title.isNotBlank()) { "Title cannot be blank" }
        val now = clock()
        entryDao.insert(entry.copy(id = 0, createdAt = now, updatedAt = now).toEntity())
    }

    override suspend fun update(entry: PasswordEntry): Boolean = withContext(ioDispatcher) {
        require(entry.id > 0 && entry.title.isNotBlank()) { "Invalid entry" }
        entryDao.update(
            entry.id, entry.title, entry.username, entry.password, entry.url,
            entry.notes, entry.category, entry.isFavorite, clock(),
        ) == 1
    }

    override suspend fun delete(id: Long): Boolean = withContext(ioDispatcher) {
        entryDao.deleteById(id) == 1
    }
}
