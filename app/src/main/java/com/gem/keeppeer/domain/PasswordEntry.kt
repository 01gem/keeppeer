package com.gem.keeppeer.domain

import com.gem.keeppeer.data.local.EntryEntity

/**
 * Domain model representing a password entry.
 *
 * Requirements:
 * - [id]: 0L for unpersisted entries, positive Long for stored entries.
 * - [title]: Mandatory non-blank string.
 * - [password]: Exact password string; preserved without trimming.
 * - [notes]: Optional multiline notes; preserved without trimming.
 * - [createdAt]: Immutable creation timestamp in epoch milliseconds.
 * - [updatedAt]: Last modification timestamp in epoch milliseconds.
 */
data class PasswordEntry(
    val id: Long = 0L,
    val title: String,
    val username: String? = null,
    val password: String,
    val url: String? = null,
    val notes: String? = null,
    val category: String? = null,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    init {
        require(title.isNotBlank()) { "Entry title cannot be blank" }
    }
}

/**
 * Maps a Room database entity to the clean domain model.
 */
fun EntryEntity.toDomain(): PasswordEntry {
    return PasswordEntry(
        id = id,
        title = title,
        username = username,
        password = password,
        url = url,
        notes = notes,
        category = category,
        isFavorite = isFavorite,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

/**
 * Maps a domain model to the Room database entity.
 */
fun PasswordEntry.toEntity(): EntryEntity {
    return EntryEntity(
        id = id,
        title = title,
        username = username,
        password = password,
        url = url,
        notes = notes,
        category = category,
        isFavorite = isFavorite,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
