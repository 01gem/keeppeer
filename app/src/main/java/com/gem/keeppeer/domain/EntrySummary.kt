package com.gem.keeppeer.domain

/** List projection deliberately contains neither passwords nor notes. */
data class EntrySummary(
    val id: Long,
    val title: String,
    val username: String?,
    val url: String?,
    val category: String?,
    val isFavorite: Boolean = false,
)
