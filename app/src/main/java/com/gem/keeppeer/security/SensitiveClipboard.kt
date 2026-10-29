package com.gem.keeppeer.security

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import kotlinx.coroutines.*
import java.util.UUID

/** Never retains the copied value itself. Token distinguishes our clip from newer clips. */
class SensitiveClipboard(context: Context) {
    private val clipboard = context.getSystemService(ClipboardManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var token: String? = null
    private var expiry: Job? = null
    fun copy(value: String): Boolean = try {
        clear()
        val label = "Keeppeer:" + UUID.randomUUID().toString()
        val clip = ClipData.newPlainText(label, value)
        clip.description.extras = PersistableBundle().apply {
            putBoolean(if (Build.VERSION.SDK_INT >= 33) ClipDescription.EXTRA_IS_SENSITIVE
                else "android.content.extra.IS_SENSITIVE", true)
        }
        clipboard.setPrimaryClip(clip)
        token = label
        expiry = scope.launch { delay(30_000); clear() }
        true
    } catch (_: Exception) { false }
    fun clear() {
        expiry?.cancel()
        expiry = null
        val owned = token ?: return
        try {
            if (clipboard.primaryClipDescription?.label?.toString() == owned) {
                if (Build.VERSION.SDK_INT >= 28) clipboard.clearPrimaryClip()
                else clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                token = null
            } else if (clipboard.primaryClipDescription != null) token = null
        } catch (_: Exception) { /* Retain ownership token for a foreground retry. */ }
    }
}
