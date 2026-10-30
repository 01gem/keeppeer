package com.gem.keeppeer.ui.privacy

import android.content.ContextWrapper
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.gem.keeppeer.MainActivity
import java.util.WeakHashMap

/**
 * Redacts app-owned windows when none has input focus, including live Recents on providers
 * that keep the Activity RESUMED. This changes presentation only: session expiry remains
 * governed by background time / screen lock, and biometric prompt ownership is unchanged.
 *
 * Window alpha is applied in the focus dispatch, without waiting for a Compose frame or a
 * timer. Keep the composition and its in-memory drafts alive. An owned-dialog focus handoff
 * restores the whole app surface when the dialog gains focus; a system prompt does not.
 */
internal class PrivacyWindows : AutoCloseable {
    private data class Registration(
        val alpha: Float,
        val listener: ViewTreeObserver.OnWindowFocusChangeListener,
    )
    private val windows = linkedMapOf<Window, Registration>()
    // A stopped dialog may reuse its still-redacted Window. Keep the restoration basis
    // independently of listener ownership, without strongly retaining disposed windows.
    // Values must never reference their weak Window keys.
    private val originalAlphas = WeakHashMap<Window, Float>()

    fun register(window: Window): () -> Unit {
        check(window !in windows)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val listener = ViewTreeObserver.OnWindowFocusChangeListener { updateVisibility() }
        val originalAlpha = originalAlphas.getOrPut(window) { window.attributes.alpha }
        windows[window] = Registration(originalAlpha, listener)
        window.decorView.viewTreeObserver.addOnWindowFocusChangeListener(listener)
        updateVisibility()
        return {
            windows.remove(window)?.let {
                val observer = window.decorView.viewTreeObserver
                if (observer.isAlive) observer.removeOnWindowFocusChangeListener(it.listener)
                updateVisibility()
            }
        }
    }

    private fun updateVisibility() {
        val visible = windows.keys.any { it.decorView.hasWindowFocus() }
        windows.forEach { (window, registration) ->
            val alpha = if (visible) registration.alpha else 0f
            if (window.attributes.alpha != alpha) {
                window.attributes = window.attributes.apply { this.alpha = alpha }
            }
            if (!visible) WindowInsetsControllerCompat(window, window.decorView)
                .hide(WindowInsetsCompat.Type.ime())
        }
    }

    override fun close() {
        windows.forEach { (window, registration) ->
            val observer = window.decorView.viewTreeObserver
            if (observer.isAlive) observer.removeOnWindowFocusChangeListener(registration.listener)
        }
        windows.clear()
        originalAlphas.clear()
    }
}

/** Invoke inside the dialog composition, whose LocalView belongs to its separate window. */
@Composable
internal fun ProtectDialogWindow() {
    val view = LocalView.current
    val window = (view.parent as? DialogWindowProvider)?.window
    val activity = generateSequence(view.context) { (it as? ContextWrapper)?.baseContext }
        .filterIsInstance<MainActivity>().firstOrNull()
    DisposableEffect(window, activity) {
        val unregister = if (window != null && activity != null)
            activity.privacyWindows.register(window) else ({})
        onDispose { unregister() }
    }
}
