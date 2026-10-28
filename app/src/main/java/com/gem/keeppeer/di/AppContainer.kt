package com.gem.keeppeer.di

import android.content.Context
import com.gem.keeppeer.data.local.DatabaseProvider
import com.gem.keeppeer.data.local.PlaintextDatabaseProvider
import com.gem.keeppeer.data.repo.EntryRepository
import com.gem.keeppeer.data.repo.EntryRepositoryImpl

/**
 * Dependency injection container for Keeppeer.
 * Manual DI approach per project contract.
 */
interface AppContainer {
    val databaseProvider: DatabaseProvider
    val entryRepository: EntryRepository
}

/**
 * Default production implementation of [AppContainer].
 */
class DefaultAppContainer(private val context: Context) : AppContainer {

    override val databaseProvider: DatabaseProvider by lazy {
        PlaintextDatabaseProvider(context)
    }

    override val entryRepository: EntryRepository by lazy {
        EntryRepositoryImpl(databaseProvider.database.entryDao())
    }
}
