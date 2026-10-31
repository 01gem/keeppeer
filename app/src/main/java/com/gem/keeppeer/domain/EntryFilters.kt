package com.gem.keeppeer.domain

/** Typed identities keep real categories named All/Uncategorized distinct. */
sealed interface CategoryFilter {
    data object All : CategoryFilter
    data object Uncategorized : CategoryFilter
    data class Named(val value: String) : CategoryFilter
}

data class EntrySections(
    val favorites: List<EntrySummary>,
    val remaining: List<EntrySummary>,
) {
    val entries get() = favorites + remaining
}

object EntryFilters {
    // DAO order is updatedAt DESC, id DESC; filtering/partitioning retain it.
    fun categories(entries: List<EntrySummary>): List<String> =
        entries.mapNotNull { it.category?.takeUnless(String::isBlank) }.distinct().sorted()

    fun sections(entries: List<EntrySummary>, query: String, category: CategoryFilter,
        favoritesOnly: Boolean = false): EntrySections {
        val term = query.trim()
        val matching = entries.filter { entry ->
            val categoryMatches = when (category) {
                CategoryFilter.All -> true
                CategoryFilter.Uncategorized -> entry.category.isNullOrBlank()
                is CategoryFilter.Named -> entry.category == category.value
            }
            categoryMatches && (!favoritesOnly || entry.isFavorite) &&
                (term.isEmpty() || listOf(entry.title, entry.username, entry.url, entry.category)
                    .any { it?.contains(term, ignoreCase = true) == true })
        }
        val (favorites, remaining) = matching.partition { it.isFavorite }
        return EntrySections(favorites, remaining)
    }
}
