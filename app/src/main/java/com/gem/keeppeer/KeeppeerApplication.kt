package com.gem.keeppeer

import android.app.Application
import com.gem.keeppeer.di.AppContainer
import com.gem.keeppeer.di.DefaultAppContainer

/**
 * Custom Application class providing the application-level dependency container.
 */
class KeeppeerApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
