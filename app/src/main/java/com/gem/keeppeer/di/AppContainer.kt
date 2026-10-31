package com.gem.keeppeer.di

import android.content.Context
import com.gem.keeppeer.security.SensitiveClipboard
import com.gem.keeppeer.security.VaultSession

interface AppContainer {
    val session: VaultSession
    val appearance: com.gem.keeppeer.ui.settings.ThemeSettings
}
class DefaultAppContainer(context: Context) : AppContainer {
    override val appearance = com.gem.keeppeer.ui.settings.ThemeSettings(context.applicationContext)
    override val session = VaultSession(context.applicationContext, SensitiveClipboard(context.applicationContext))
}
