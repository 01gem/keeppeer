package com.gem.keeppeer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.gem.keeppeer.security.VaultSession
import com.gem.keeppeer.ui.lock.VaultRoot
import com.gem.keeppeer.ui.theme.KeeppeerTheme
import javax.crypto.Cipher

class MainActivity : FragmentActivity() {
    private val session: VaultSession get() = (application as KeeppeerApplication).container.session
    private var prompt: BiometricPrompt? = null
    private var promptDone: ((Boolean) -> Unit)? = null
    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) { prompt?.cancelAuthentication(); session.lock() }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        ContextCompat.registerReceiver(this, screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED)
        setContent { KeeppeerTheme { VaultRoot(session, ::authenticateBiometric) } }
    }
    override fun onStart() { super.onStart(); session.foreground() }
    override fun onStop() {
        prompt?.cancelAuthentication()
        // A configuration change does not count as backgrounding.
        if (!isChangingConfigurations) session.background()
        super.onStop()
    }
    override fun onDestroy() {
        prompt?.cancelAuthentication()
        promptDone = null
        unregisterReceiver(screenOff)
        super.onDestroy()
    }
    private fun authenticateBiometric(enroll: Boolean, cipher: Cipher, done: (Boolean) -> Unit) {
        prompt?.cancelAuthentication()
        promptDone = done
        val expected = cipher
        prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val authenticated = result.cryptoObject?.cipher
                    val callback = promptDone
                    promptDone = null
                    if (authenticated !== expected) { callback?.invoke(false); return }
                    if (enroll) session.finishBiometricEnrollment(authenticated) { callback?.invoke(it) }
                    else { session.unlockBiometric(authenticated); callback?.invoke(true) }
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    promptDone?.invoke(false)
                    promptDone = null
                }
            })
        try {
            prompt?.authenticate(BiometricPrompt.PromptInfo.Builder()
                .setTitle(if (enroll) "Enable biometric unlock" else "Unlock Keeppeer")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .setNegativeButtonText("Use master password").build(), BiometricPrompt.CryptoObject(cipher))
        } catch (_: Exception) { promptDone?.invoke(false); promptDone = null }
    }
}
