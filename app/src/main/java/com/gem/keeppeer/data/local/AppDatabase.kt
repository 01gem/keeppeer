package com.gem.keeppeer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database definition for Keeppeer.
 *
 * PHASE 1 PROTOTYPE NOTICE:
 * This database operates in plaintext SQLite mode strictly for Phase 1 development
 * and testing. Destructive migration fallbacks are strictly prohibited per security
 * requirements. In Phase 2, this database boundary will be replaced with SQLCipher
 * encryption.
 */
@Database(
    entities = [EntryEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao

    companion object {
        const val DATABASE_NAME = "keeppeer_prototype.db"
    }
}
