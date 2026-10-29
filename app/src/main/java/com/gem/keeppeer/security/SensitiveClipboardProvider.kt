package com.gem.keeppeer.security

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.AndroidClipboard
import androidx.compose.ui.platform.nativeClipboardManager
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

/** Covers text selection, cut and keyboard copy as well as explicit copy buttons. */
// Compose 1.12 requires this public, test-visible marker for native toolbar metadata.
// Keep its use local to the adapter; the touch-cut regression verifies the pinned contract.
@SuppressLint("VisibleForTests")
@Suppress("DEPRECATION")
@Composable
fun SensitiveClipboardProvider(service: SensitiveClipboard, content: @Composable () -> Unit) {
    val original = LocalClipboard.current
    val legacy = LocalClipboardManager.current
    val secure = remember(service, original) {
        // Foundation's touch toolbar reads platform metadata through AndroidClipboard.
        // Keep that bridge while routing every text write through the sensitive service.
        object : AndroidClipboard, Clipboard by original {
            override val clipboardManager = original.nativeClipboardManager
            @Deprecated("Use nativeClipboardManager")
            override val nativeClipboard get() = clipboardManager
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
