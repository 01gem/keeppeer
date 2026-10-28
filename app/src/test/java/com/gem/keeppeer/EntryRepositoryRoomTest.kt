package com.gem.keeppeer

import android.app.Application
import androidx.room.Room
import app.cash.turbine.test
import com.gem.keeppeer.data.local.AppDatabase
import com.gem.keeppeer.data.repo.EntryRepositoryImpl
import com.gem.keeppeer.domain.PasswordEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, manifest = Config.NONE)
class EntryRepositoryRoomTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: EntryRepositoryImpl
    private var now = 1_700_000_000_000L

    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(), AppDatabase::class.java,
        ).build()
        repository = EntryRepositoryImpl(database.entryDao(), clock = { now })
    }

    @After fun close() { database.close() }

    @Test fun crudPreservesExactFieldsAndHandlesMissingIds() = runBlocking {
        val original = PasswordEntry(
            title = "Synthetic 雲", password = "  synthetic-only \n ",
            notes = "  第一行\nSecond line\n🙂  ", username = null, url = null,
            category = null, isFavorite = true,
        )
        val id = repository.create(original)
        val stored = requireNotNull(repository.getById(id))
        assertTrue(id > 0)
        assertEquals(original.password, stored.password)
        assertEquals(original.notes, stored.notes)
        assertNull(stored.username)
        assertNull(stored.url)
        assertNull(stored.category)
        assertTrue(stored.isFavorite)
        assertEquals(now, stored.createdAt)
        assertEquals(now, stored.updatedAt)
        assertEquals(stored, repository.observeById(id).first())
        assertTrue(repository.update(stored.copy(title = "Edited", username = "  user  ", category = "Work")))
        assertEquals("  user  ", repository.getById(id)?.username)
        assertEquals("Work", repository.getById(id)?.category)
        assertTrue(repository.delete(id))
        assertNull(repository.getById(id))
        assertNull(repository.observeById(id).first())
        assertFalse(repository.delete(id))
        assertFalse(repository.update(stored))
    }

    @Test fun timestampsIgnoreCallerValuesAndAdvanceWithStationaryOrBackwardClock() = runBlocking {
        val id = repository.create(PasswordEntry(
            title = "Synthetic", password = "", createdAt = 2, updatedAt = Long.MAX_VALUE,
        ))
        val first = requireNotNull(repository.getById(id))
        assertEquals(now, first.createdAt)
        assertEquals(now, first.updatedAt)
        assertTrue(repository.update(first.copy(createdAt = 3)))
        val second = requireNotNull(repository.getById(id))
        assertEquals(first.createdAt, second.createdAt)
        assertEquals(first.updatedAt + 1, second.updatedAt)
        now -= 10_000
        assertTrue(repository.update(second.copy(createdAt = 4)))
        val third = requireNotNull(repository.getById(id))
        assertEquals(first.createdAt, third.createdAt)
        assertEquals(second.updatedAt + 1, third.updatedAt)
        now += 20_000
        repository.update(third)
        assertEquals(now, repository.getById(id)?.updatedAt)
    }

    @Test fun searchMatchesEveryMetadataFieldUnicodeAndLiteralWildcardsOnly() = runBlocking {
        val id = repository.create(PasswordEntry(
            title = "ÉCOLE 100%_\\ 雲", username = "ALICE",
            password = "synthetic-secret-only", notes = "synthetic-notes-only",
            url = "https://EXAMPLE.test", category = "WÖRK",
        ))
        for (term in listOf("école", "alice", "example", "wörk", "%", "_", "\\", "雲")) {
            assertEquals(listOf(id), repository.search(term).first().map { it.id })
        }
        for (term in listOf("synthetic-secret-only", "synthetic-notes-only", "does not exist", "' OR 1=1 --")) {
            assertTrue(repository.search(term).first().isEmpty())
        }
        repository.create(PasswordEntry(title = "Other", password = "synthetic"))
        assertEquals(1, repository.search("%").first().size)
        assertEquals(1, repository.search("_").first().size)
        assertEquals(2, repository.search("  ").first().size)
    }

    @Test fun reactiveSearchUpdatesAfterInsertEditAndDelete() = runBlocking {
        repository.search("sample").test {
            assertTrue(awaitItem().isEmpty())
            val id = repository.create(PasswordEntry(title = "Sample", password = "synthetic"))
            assertEquals(id, awaitItem().single().id)
            val entry = requireNotNull(repository.getById(id))
            repository.update(entry.copy(title = "Changed"))
            assertTrue(awaitItem().isEmpty())
            repository.update(entry.copy(title = "Sample again"))
            assertEquals("Sample again", awaitItem().single().title)
            repository.delete(id)
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun titleMustBeNonblank() {
        assertThrows(IllegalArgumentException::class.java) { PasswordEntry(title = " \n", password = "") }
    }
}
