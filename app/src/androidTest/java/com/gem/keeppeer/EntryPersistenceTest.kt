package com.gem.keeppeer

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gem.keeppeer.data.local.AppDatabase
import com.gem.keeppeer.data.repo.EntryRepositoryImpl
import com.gem.keeppeer.domain.PasswordEntry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EntryPersistenceTest {
    @Test fun savedEntrySurvivesDatabaseCloseAndReopen() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "synthetic-persistence-test.db"
        context.deleteDatabase(name)
        fun open() = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        var database = open()
        try {
            val id = EntryRepositoryImpl(database.entryDao()).create(
                PasswordEntry(title = "Synthetic persistence", password = " synthetic ", notes = "雲\n🙂"),
            )
            database.close()
            database = open()
            val stored = EntryRepositoryImpl(database.entryDao()).getById(id)
            assertNotNull(stored)
            assertEquals(" synthetic ", stored?.password)
            assertEquals("雲\n🙂", stored?.notes)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun inMemoryCrudUsesRealDeviceSqlite() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repository = EntryRepositoryImpl(database.entryDao())
            val id = repository.create(PasswordEntry(title = "Synthetic", password = ""))
            val entry = requireNotNull(repository.getById(id))
            assertTrue(repository.update(entry.copy(title = "Edited")))
            assertEquals(entry.createdAt, repository.getById(id)?.createdAt)
            assertTrue(requireNotNull(repository.getById(id)).updatedAt > entry.updatedAt)
            assertTrue(repository.delete(id))
            assertNull(repository.getById(id))
        } finally { database.close() }
    }
}
