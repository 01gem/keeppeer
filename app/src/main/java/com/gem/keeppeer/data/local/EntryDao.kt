package com.gem.keeppeer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gem.keeppeer.domain.EntrySummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.ensureActive

@Dao
interface EntryDao {
    /** Bound full-entry allocation before loading decrypted fields, in the same transaction. */
    @Query("""SELECT COUNT(*) FROM entries""")
    suspend fun csvCount(): Long

    @Query("""SELECT COALESCE(SUM(
        length(CAST(title AS BLOB)) + length(CAST(password AS BLOB)) +
        COALESCE(length(CAST(username AS BLOB)),0) + COALESCE(length(CAST(url AS BLOB)),0) +
        COALESCE(length(CAST(notes AS BLOB)),0) + COALESCE(length(CAST(category AS BLOB)),0)),0)
        FROM entries""")
    suspend fun csvBytes(): Long

    @Query("SELECT * FROM entries ORDER BY id ASC")
    suspend fun csvEntries(): List<EntryEntity>

    @androidx.room.Transaction
    suspend fun exportCsv(): List<com.gem.keeppeer.domain.PasswordEntry> {
        val limits = com.gem.keeppeer.csv.VaultCsv.limits
        check(csvCount() <= limits.rows && csvBytes() <= limits.bytes) { "CSV vault limit" }
        return csvEntries().map { com.gem.keeppeer.domain.PasswordEntry(
            it.id, it.title, it.username, it.password, it.url, it.notes, it.category,
            it.isFavorite, it.createdAt, it.updatedAt) }
    }

    @androidx.room.Transaction
    suspend fun previewCsv(rows: List<com.gem.keeppeer.domain.PasswordEntry>) =
        com.gem.keeppeer.csv.CsvImport.preview(exportCsv(), rows)

    @androidx.room.Update
    suspend fun csvReplace(entry: EntryEntity): Int

    @androidx.room.Transaction
    suspend fun importCsv(
        rows: List<com.gem.keeppeer.domain.PasswordEntry>,
        expected: com.gem.keeppeer.csv.ImportPreview,
        policy: com.gem.keeppeer.csv.DuplicatePolicy,
        now: Long,
    ): com.gem.keeppeer.csv.ImportResult {
        val existing = exportCsv()
        val refreshed = com.gem.keeppeer.csv.CsvImport.preview(existing, rows)
        if (refreshed != expected) return com.gem.keeppeer.csv.ImportResult.Changed(refreshed)
        // Lowest local ID is the explicit target; other pre-existing matches are untouched.
        val targets = existing.sortedBy { it.id }.groupBy { com.gem.keeppeer.csv.CsvImport.key(it) }
            .mapValues { it.value.first() }.toMutableMap()
        var inserted = 0; var updated = 0; var skipped = 0
        for (row in rows) {
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            val key = com.gem.keeppeer.csv.CsvImport.key(row)
            val target = targets[key]
            if (target == null || policy == com.gem.keeppeer.csv.DuplicatePolicy.KEEP_BOTH) {
                val id = insert(EntryEntity(title = row.title, username = row.username, password = row.password,
                    url = row.url, notes = row.notes, category = row.category, isFavorite = row.isFavorite,
                    createdAt = row.createdAt, updatedAt = row.updatedAt))
                if (target == null) targets[key] = row.copy(id = id)
                inserted++
            } else if (policy == com.gem.keeppeer.csv.DuplicatePolicy.SKIP) skipped++
            else {
                val timestamp = com.gem.keeppeer.csv.CsvImport.nextTimestamp(target.updatedAt, row.updatedAt, now)
                check(csvReplace(EntryEntity(target.id, row.title, row.username, row.password,
                    row.url, row.notes, row.category, row.isFavorite, target.createdAt, timestamp)) == 1)
                targets[key] = row.copy(id = target.id, createdAt = target.createdAt, updatedAt = timestamp)
                updated++
            }
        }
        check(csvCount() <= com.gem.keeppeer.csv.VaultCsv.limits.rows &&
            csvBytes() <= com.gem.keeppeer.csv.VaultCsv.limits.bytes) { "CSV vault limit" }
        kotlinx.coroutines.currentCoroutineContext().ensureActive()
        return com.gem.keeppeer.csv.ImportResult.Applied(com.gem.keeppeer.csv.ImportCounts(inserted, updated, skipped))
    }

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: EntryEntity): Long

    // A single statement preserves createdAt and advances updatedAt, even if the
    // wall clock moves backwards or two edits happen in the same millisecond.
    @Query("""
        UPDATE entries SET title = :title, username = :username, password = :password,
        url = :url, notes = :notes, category = :category, is_favorite = :isFavorite,
        updated_at = MAX(:now, updated_at + 1) WHERE id = :id AND updated_at < 9223372036854775807
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
