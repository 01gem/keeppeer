package com.gem.keeppeer.data.local

import android.content.Context
import androidx.room.Room

/**
 * Replaceable database provider boundary.
 *
 * In Phase 1, [PlaintextDatabaseProvider] instantiates an unencrypted SQLite database.
 * In Phase 2, this boundary will be replaced with an encrypted provider utilizing SQLCipher.
 */
interface DatabaseProvider {
    val database: AppDatabase
    val isEncrypted: Boolean
}

/**
 * Plaintext SQLite database provider for Phase 1 prototype development.
 */
class PlaintextDatabaseProvider(
    private val context: Context,
    private val inMemory: Boolean = false,
) : DatabaseProvider {

    override val isEncrypted: Boolean = false

    override val database: AppDatabase by lazy {
        if (inMemory) {
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .build()
        } else {
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                AppDatabase.DATABASE_NAME,
            )
                // Destructive migration fallbacks are strictly avoided
                .build()
        }
    }
}
