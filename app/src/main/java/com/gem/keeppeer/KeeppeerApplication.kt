package com.gem.keeppeer

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.gem.keeppeer.di.AppContainer
import com.gem.keeppeer.di.DefaultAppContainer

open class KeeppeerApplication : Application() {
    lateinit var container: AppContainer
        private set
    protected open fun createContainer(): AppContainer = DefaultAppContainer(this)
    override fun onCreate() {
        super.onCreate()
        container = createContainer()
        ContextCompat.registerReceiver(this, object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) container.session.lock()
            }
        }, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED)
    }
}
