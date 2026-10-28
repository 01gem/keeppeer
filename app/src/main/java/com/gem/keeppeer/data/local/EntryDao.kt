package com.gem.keeppeer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gem.keeppeer.domain.EntrySummary
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: EntryEntity): Long

    // A single statement preserves createdAt and advances updatedAt, even if the
    // wall clock moves backwards or two edits happen in the same millisecond.
    @Query("""
        UPDATE entries SET title = :title, username = :username, password = :password,
        url = :url, notes = :notes, category = :category, is_favorite = :isFavorite,
        updated_at = MAX(:now, updated_at + 1) WHERE id = :id
    """)
    suspend fun update(
        id: Long, title: String, username: String?, password: String,
        url: String?, notes: String?, category: String?, isFavorite: Boolean, now: Long,
    ): Int

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun getById(id: Long): EntryEntity?

    @Query("SELECT * FROM entries WHERE id = :id")
    fun observeById(id: Long): Flow<EntryEntity?>

    @Query("""
        SELECT id, title, username, url, category FROM entries
        ORDER BY updated_at DESC, id DESC
    """)
    fun observeSummaries(): Flow<List<EntrySummary>>
}
