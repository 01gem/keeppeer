package com.gem.keeppeer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/** Room schema; production opens only through the authenticated SQLCipher session. */
@Database(
    entities = [EntryEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao

    companion object {
        const val DATABASE_NAME = EncryptedDatabaseProvider.VAULT_NAME
    }
}
