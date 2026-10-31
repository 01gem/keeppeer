package com.gem.keeppeer

import android.app.Application
import androidx.room.Room
import app.cash.turbine.test
import com.gem.keeppeer.data.local.AppDatabase
import com.gem.keeppeer.data.repo.EntryRepositoryImpl
import com.gem.keeppeer.domain.PasswordEntry
import com.gem.keeppeer.domain.toEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, manifest = Config.NONE)
class FavoriteRoomTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: EntryRepositoryImpl
    private var now = 100L
    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java).build()
        repository = EntryRepositoryImpl(database.entryDao(), clock = { now })
    }
    @After fun close() { database.close() }

    @Test fun fieldUpdatePreservesExactCredentialsCreationAndAdvancesWithRollbackClock() = runBlocking {
        val id = repository.create(PasswordEntry(title = "Synthetic", username = " user ",
            password = " exact synthetic ", notes = "\n雲", category = "Work", url = "https://example.test"))
        val original = repository.getById(id)!!
        now = 1
        assertTrue(repository.setFavorite(id, true))
        val favorite = repository.getById(id)!!
        assertTrue(favorite == original.copy(isFavorite = true, updatedAt = original.updatedAt + 1))
        assertTrue(repository.setFavorite(id, false))
        assertEquals(102L, repository.getById(id)!!.updatedAt)
        assertFalse(repository.setFavorite(999, true))
        repository.delete(id)
        assertFalse(repository.setFavorite(id, true))
    }
    @Test fun exhaustedTimestampRejectsWithoutChangingFavoriteOrFields() = runBlocking {
        val id = database.entryDao().insert(PasswordEntry(title = "Synthetic", password = "",
            createdAt = 0, updatedAt = Long.MAX_VALUE).let { it.toEntity() })
        val original = repository.getById(id)
        try { repository.setFavorite(id, true); fail("Must reject exhausted timestamp") }
        catch (_: ArithmeticException) { }
        assertEquals(original, repository.getById(id))
    }
    @Test fun concurrentFieldUpdatesDoNotOverwriteCredentialChanges() = runBlocking {
        val id = repository.create(PasswordEntry(title = "Synthetic", password = "before"))
        val original = repository.getById(id)!!
        coroutineScope {
            val credential = async { repository.update(original.copy(password = "after", notes = "after notes")) }
            val favorite = async { repository.setFavorite(id, true) }
            assertTrue(credential.await()); assertTrue(favorite.await())
        }
        val final = repository.getById(id)!!
        assertTrue(final.password == "after" && final.notes == "after notes")
        assertEquals(original.createdAt, final.createdAt)
        assertEquals(original.updatedAt + 2, final.updatedAt)
    }
    @Test fun metadataReactsAndDeterministicOrderingUsesUpdatedThenId() = runBlocking {
        val first = repository.create(PasswordEntry(title = "First", password = ""))
        val second = repository.create(PasswordEntry(title = "Second", password = ""))
        repository.observeAll().test {
            assertEquals(listOf(second, first), awaitItem().map { it.id })
            repository.setFavorite(first, true)
            val updated = awaitItem()
            assertEquals(listOf(first, second), updated.map { it.id })
            assertTrue(updated.first().isFavorite)
            repository.delete(first)
            assertEquals(listOf(second), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }
}
