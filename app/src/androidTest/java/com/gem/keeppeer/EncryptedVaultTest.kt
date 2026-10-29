package com.gem.keeppeer

import android.database.sqlite.SQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import com.gem.keeppeer.data.local.EncryptedDatabaseProvider
import com.gem.keeppeer.data.repo.EntryRepositoryImpl
import com.gem.keeppeer.domain.PasswordEntry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.security.SecureRandom

class EncryptedVaultTest {
    @Test fun activeVaultWithMissingSchemaCannotRunRoomOnCreateAgain() {
        val context = SyntheticVaultContext(InstrumentationRegistry.getInstrumentation().targetContext)
        val name = "synthetic-missing-schema.db"
        val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
        try {
            val db = EncryptedDatabaseProvider.open(context, key, name, allowCreate = true)
            db.openHelper.writableDatabase.execSQL("DROP TABLE entries")
            db.openHelper.writableDatabase.execSQL("PRAGMA user_version = 0")
            db.close()
            var failed = false
            try { EncryptedDatabaseProvider.open(context, key, name).close() }
            catch (_: Exception) { failed = true }
            assertTrue(failed)
            assertTrue(context.getDatabasePath(name).exists())
        } finally { key.fill(0); context.deleteDatabase(name) }
    }

    @Test fun correctKeyReopensButWrongKeyAndPlainSqliteFailWithoutDeleting() = runBlocking {
        val context = SyntheticVaultContext(InstrumentationRegistry.getInstrumentation().targetContext)
        val name = "synthetic-encrypted-test.db"
        context.deleteDatabase(name)
        val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val file = context.getDatabasePath(name)
        try {
            val db = EncryptedDatabaseProvider.open(context, key, name, allowCreate = true)
            val id = EntryRepositoryImpl(db.entryDao()).create(
                PasswordEntry(title = "Synthetic cipher", password = "synthetic-only-secret"))
            db.close()
            assertFalse(file.readBytes().toString(Charsets.ISO_8859_1).contains("synthetic-only-secret"))
            assertFalse(file.inputStream().use { it.readBytes().take(16).toByteArray() }
                .contentEquals("SQLite format 3\u0000".toByteArray()))
            var failed = false
            try {
                SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY) {
                    throw android.database.sqlite.SQLiteException("Synthetic encrypted file rejected; preserve file")
                }.use {
                    it.rawQuery("SELECT count(*) FROM entries", null).use { cursor -> cursor.moveToFirst() }
                }
            } catch (_: Exception) { failed = true }
            assertTrue(failed)
            failed = false
            try { EncryptedDatabaseProvider.open(context, ByteArray(32) { 7 }, name).close() }
            catch (_: Exception) { failed = true }
            assertTrue(failed)
            assertTrue(file.exists())
            val reopened = EncryptedDatabaseProvider.open(context, key, name)
            try { assertEquals("synthetic-only-secret", EntryRepositoryImpl(reopened.entryDao()).getById(id)?.password) }
            finally { reopened.close() }
        } finally { key.fill(0); context.deleteDatabase(name) }
    }
}
