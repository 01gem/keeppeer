package com.gem.keeppeer.csv

import com.gem.keeppeer.domain.PasswordEntry
import java.nio.ByteBuffer
import java.security.MessageDigest
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

enum class DuplicatePolicy { SKIP, UPDATE, KEEP_BOTH }
data class ImportPreview(val total: Int, val new: Int, val conflicting: Int, val revision: String)
data class ImportCounts(val inserted: Int, val updated: Int, val skipped: Int)
sealed interface ImportResult {
    data class Applied(val counts: ImportCounts) : ImportResult
    data class Changed(val preview: ImportPreview) : ImportResult
}

/** Sequential matching: exact case-sensitive title and username, null/empty username equivalent. */
object CsvImport {
    fun key(e: PasswordEntry) = e.title to e.username.orEmpty()
    fun validate(e: PasswordEntry) {
        require(e.id == 0L && e.title.isNotBlank() && e.createdAt >= 0 && e.updatedAt >= e.createdAt) {
            "Invalid CSV model"
        }
    }
    suspend fun preview(existing: List<PasswordEntry>, rows: List<PasswordEntry>): ImportPreview {
        require(rows.size <= VaultCsv.limits.rows) { "CSV row limit" }
        var chars = VaultCsv.header.sumOf { it.length }.toLong()
        rows.forEach { row ->
            currentCoroutineContext().ensureActive()
            validate(row)
            listOf(row.title, row.username.orEmpty(), row.password, row.url.orEmpty(), row.notes.orEmpty(),
                row.category.orEmpty(), row.isFavorite.toString(), row.createdAt.toString(), row.updatedAt.toString())
                .forEach { field ->
                    require(field.length <= VaultCsv.limits.fieldChars) { "CSV field limit" }
                    chars += field.length
                    require(chars <= VaultCsv.limits.totalChars) { "CSV aggregate limit" }
                }
        }
        val keys = existing.mapTo(HashSet()) { key(it) }
        var conflicts = 0
        rows.forEach { validate(it); if (!keys.add(key(it))) conflicts++ }
        // Only an ephemeral digest survives preview; it includes all stored fields and IDs.
        val digest = MessageDigest.getInstance("SHA-256")
        fun value(s: String?) {
            if (s == null) { digest.update(ByteBuffer.allocate(4).putInt(-1).array()); return }
            val bytes = s.toByteArray(Charsets.UTF_8)
            try {
                digest.update(ByteBuffer.allocate(4).putInt(bytes.size).array())
                digest.update(bytes)
            } finally { bytes.fill(0) }
        }
        existing.sortedBy { it.id }.forEach { e ->
            currentCoroutineContext().ensureActive()
            listOf(e.id.toString(), e.title, e.username, e.password, e.url, e.notes, e.category,
                e.isFavorite.toString(), e.createdAt.toString(), e.updatedAt.toString()).forEach(::value)
        }
        val revision = digest.digest().joinToString("") { "%02x".format(it) }
        return ImportPreview(rows.size, rows.size - conflicts, conflicts, revision)
    }

    fun nextTimestamp(stored: Long, imported: Long, now: Long): Long {
        if (stored == Long.MAX_VALUE) throw ArithmeticException("Timestamp exhausted")
        return maxOf(stored + 1, imported, now)
    }
}
