package com.gem.keeppeer

import com.gem.keeppeer.domain.*
import org.junit.Assert.*
import org.junit.Test

class EntryFiltersTest {
    private fun row(id: Long, category: String?, favorite: Boolean = false, title: String = "ÉCOLE 100%_\\ 雲") =
        EntrySummary(id, title, "ALICE", "https://EXAMPLE.test", category, favorite)
    private val rows = listOf(row(9, "All", true), row(8, "Uncategorized"), row(7, "Work", true),
        row(6, "work"), row(5, "  Work  "), row(4, " "), row(3, ""), row(2, null))
    @Test fun categoriesAreExactCaseSensitiveSortedAndIndependentOfSearch() {
        assertEquals(listOf("  Work  ", "All", "Uncategorized", "Work", "work"), EntryFilters.categories(rows))
        assertEquals(5, EntryFilters.categories(rows).size)
        assertTrue(EntryFilters.sections(rows, "absent", CategoryFilter.All).entries.isEmpty())
    }
    @Test fun reservedLookingNamesRemainTypedAndBlankCategoriesAreUncategorized() {
        assertEquals(listOf(9L), EntryFilters.sections(rows, "", CategoryFilter.Named("All")).entries.map { it.id })
        assertEquals(listOf(8L), EntryFilters.sections(rows, "", CategoryFilter.Named("Uncategorized")).entries.map { it.id })
        assertEquals(listOf(4L, 3L, 2L), EntryFilters.sections(rows, "", CategoryFilter.Uncategorized).entries.map { it.id })
        assertEquals(8, EntryFilters.sections(rows, "", CategoryFilter.All).entries.size)
    }
    @Test fun sectionsAreDistinctStableAndFavoriteFilterComposesWithCategory() {
        val sections = EntryFilters.sections(rows, "", CategoryFilter.All)
        assertEquals(listOf(9L, 7L), sections.favorites.map { it.id })
        assertEquals(listOf(8L, 6L, 5L, 4L, 3L, 2L), sections.remaining.map { it.id })
        assertEquals(8, sections.entries.map { it.id }.toSet().size)
        assertEquals(listOf(7L), EntryFilters.sections(rows, "école", CategoryFilter.Named("Work"), true).entries.map { it.id })
        assertTrue(EntryFilters.sections(rows, "", CategoryFilter.Named("work"), true).entries.isEmpty())
    }
    @Test fun searchRemainsUnicodeLiteralAndMetadataOnly() {
        for (query in listOf("école", "alice", "example", "%", "_", "\\", "雲", "  ")) {
            assertEquals(8, EntryFilters.sections(rows, query, CategoryFilter.All).entries.size)
        }
        assertTrue(EntryFilters.sections(rows, "' OR 1=1 --", CategoryFilter.All).entries.isEmpty())
        assertTrue(EntrySummary::class.java.declaredFields.none { it.name in listOf("password", "notes") })
    }
}
