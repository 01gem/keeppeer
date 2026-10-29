package com.gem.keeppeer.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/** No production plaintext factory. Only an authenticated session calls this boundary. */
object EncryptedDatabaseProvider {
    const val VAULT_NAME = "keeppeer_vault.db"
    const val LEGACY_NAME = "keeppeer_prototype.db"

    // Caller owns key and keeps it alive until database.close(); SQLCipher's factory retains it.
    fun open(context: Context, key: ByteArray, name: String = VAULT_NAME, allowCreate: Boolean = false): AppDatabase {
        require(key.size == 32)
        val file = context.getDatabasePath(name)
        check(allowCreate || file.isFile && file.length() >= 4096) { "Active vault file is missing or truncated" }
        System.loadLibrary("sqlcipher")
        val cipherFactory = SupportOpenHelperFactory(key)
        val factory = SupportSQLiteOpenHelper.Factory { configuration ->
            val original = configuration.callback
            val guarded = object : SupportSQLiteOpenHelper.Callback(original.version) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    check(allowCreate) { "Active vault schema is missing; refusing to recreate" }
                    original.onCreate(db)
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) =
                    original.onUpgrade(db, oldVersion, newVersion)
                override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) =
                    original.onDowngrade(db, oldVersion, newVersion)
                override fun onConfigure(db: SupportSQLiteDatabase) = original.onConfigure(db)
                override fun onOpen(db: SupportSQLiteDatabase) = original.onOpen(db)
                override fun onCorruption(db: SupportSQLiteDatabase) {
                    error("Vault corruption reported; existing files preserved")
                }
            }
            cipherFactory.create(SupportSQLiteOpenHelper.Configuration.builder(configuration.context)
                .name(configuration.name).callback(guarded).build())
        }
        val database = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, name)
            .openHelperFactory(factory)
            .build()
        try {
            database.openHelper.writableDatabase.query("SELECT count(*) FROM entries").use {
                check(it.moveToFirst())
            }
            return database
        } catch (failure: Exception) {
            database.close()
            throw failure
        }
    }
}
