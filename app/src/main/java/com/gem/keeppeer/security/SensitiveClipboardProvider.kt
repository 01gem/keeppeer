package com.gem.keeppeer.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

/** Covers text selection, cut and keyboard copy as well as explicit copy buttons. */
@Suppress("DEPRECATION")
@Composable
fun SensitiveClipboardProvider(service: SensitiveClipboard, content: @Composable () -> Unit) {
    val original = LocalClipboard.current
    val legacy = LocalClipboardManager.current
    val secure = remember(service, original) {
        object : Clipboard by original {
            override suspend fun setClipEntry(clipEntry: ClipEntry?) {
                if (clipEntry == null) service.clear()
                else clipEntry.clipData.getItemAt(0).text?.let { service.copy(it.toString()) }
            }
        }
    }
    val secureLegacy = remember(service, legacy) {
        object : ClipboardManager by legacy {
            override fun setText(annotatedString: AnnotatedString) { service.copy(annotatedString.text) }
            override fun setClip(clipEntry: ClipEntry?) {
                if (clipEntry == null) service.clear()
                else clipEntry.clipData.getItemAt(0).text?.let { service.copy(it.toString()) }
            }
        }
    }
    CompositionLocalProvider(LocalClipboard provides secure, LocalClipboardManager provides secureLegacy, content = content)
}
