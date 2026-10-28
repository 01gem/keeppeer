package com.gem.keeppeer.data.repo

import com.gem.keeppeer.domain.EntrySummary
import com.gem.keeppeer.domain.PasswordEntry
import kotlinx.coroutines.flow.Flow

interface EntryRepository {
    fun observeAll(): Flow<List<EntrySummary>>
    /** Unicode-aware, case-insensitive literal substring search in metadata only. */
    fun search(query: String): Flow<List<EntrySummary>>
    suspend fun getById(id: Long): PasswordEntry?
    fun observeById(id: Long): Flow<PasswordEntry?>
    suspend fun create(entry: PasswordEntry): Long
    /** Preserves stored createdAt and strictly advances updatedAt. */
    suspend fun update(entry: PasswordEntry): Boolean
    suspend fun delete(id: Long): Boolean
}
