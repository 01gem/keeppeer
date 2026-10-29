package com.gem.keeppeer.di

import android.content.Context
import com.gem.keeppeer.security.SensitiveClipboard
import com.gem.keeppeer.security.VaultSession

interface AppContainer { val session: VaultSession }
class DefaultAppContainer(context: Context) : AppContainer {
    override val session = VaultSession(context.applicationContext, SensitiveClipboard(context.applicationContext))
}
