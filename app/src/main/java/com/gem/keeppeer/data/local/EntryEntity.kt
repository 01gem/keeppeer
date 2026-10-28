package com.gem.keeppeer.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a password vault entry in local storage.
 *
 * NOTE: In Phase 1, entries are stored in a plaintext SQLite development database for
 * prototype validation. Encrypted storage via SQLCipher will be introduced in Phase 2.
 *
 * Requirements:
 * - [id]: Long auto-generated primary key.
 * - [title]: Non-blank entry title (e.g., service or account name).
 * - [username]: Optional account username or email.
 * - [password]: Exact password string; MUST NOT be trimmed.
 * - [url]: Optional website or service URL.
 * - [notes]: Optional multiline notes; MUST NOT be trimmed.
 * - [category]: Optional category name.
 * - [isFavorite]: Flag indicating if entry is starred/favorite.
 * - [createdAt]: Epoch millisecond creation timestamp; immutable across edits.
 * - [updatedAt]: Epoch millisecond last modification timestamp; updated on every edit.
 */
@Entity(tableName = "entries")
data class EntryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "username")
    val username: String? = null,

    @ColumnInfo(name = "password")
    val password: String,

    @ColumnInfo(name = "url")
    val url: String? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "category")
    val category: String? = null,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
) {
    init {
        require(title.isNotBlank()) { "Entry title cannot be blank" }
    }
}
