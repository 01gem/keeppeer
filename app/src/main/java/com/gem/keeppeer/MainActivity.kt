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
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.gem.keeppeer.security.*
import com.gem.keeppeer.ui.lock.VaultRoot
import com.gem.keeppeer.ui.theme.KeeppeerTheme
import javax.crypto.Cipher

class MainActivity : FragmentActivity() {
    private val session: VaultSession get() = (application as KeeppeerApplication).container.session
    private var prompt: BiometricPrompt? = null
    private var cancelPrompt: (() -> Unit)? = null
    // Legacy biometric prompts are app-owned DialogFragments with separate windows.
    // Set the flag in the start dispatch, before the first dialog frame is drawn.
    private val secureDialogs = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentStarted(manager: FragmentManager, fragment: Fragment) {
            (fragment as? DialogFragment)?.dialog?.window
                ?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) { cancelPrompt?.invoke(); session.lock() }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        supportFragmentManager.registerFragmentLifecycleCallbacks(secureDialogs, true)
        enableEdgeToEdge()
        ContextCompat.registerReceiver(this, screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED)
        setContent { KeeppeerTheme { VaultRoot(session, ::authenticateBiometric) } }
    }
    override fun onStart() {
        super.onStart()
        BiometricDiagnostics.record(BiometricStage.ACTIVITY_START)
        session.foreground()
    }
    override fun onStop() {
        BiometricDiagnostics.record(BiometricStage.ACTIVITY_STOP)
        cancelPrompt?.invoke()
        if (!isChangingConfigurations) session.background()
        super.onStop()
    }
    override fun onDestroy() {
        BiometricDiagnostics.record(BiometricStage.ACTIVITY_DESTROY)
        cancelPrompt?.invoke()
        supportFragmentManager.unregisterFragmentLifecycleCallbacks(secureDialogs)
        unregisterReceiver(screenOff)
        super.onDestroy()
    }
    private fun authenticateBiometric(enroll: Boolean, cipher: Cipher, done: (BiometricResult) -> Unit) {
        cancelPrompt?.invoke()
        var completed = false
        var awaitingPrompt = true
        lateinit var localPrompt: BiometricPrompt
        fun complete(result: BiometricResult) {
            if (completed) return
            completed = true
            if (enroll && !result.success) session.cancelBiometricEnrollment(cipher)
            if (prompt === localPrompt) { prompt = null; cancelPrompt = null }
            done(result)
        }
        localPrompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    BiometricDiagnostics.record(BiometricStage.PROMPT_SUCCEEDED)
                    if (completed || !awaitingPrompt) return
                    awaitingPrompt = false
                    val authenticated = result.cryptoObject?.cipher
                    BiometricDiagnostics.record(BiometricStage.CIPHER_BINDING)
                    if (authenticated !== cipher) {
                        complete(BiometricResult(BiometricOutcome.CIPHER_MISMATCH, BiometricStage.CIPHER_BINDING))
                        return
                    }
                    if (enroll) session.finishBiometricEnrollment(authenticated, ::complete)
                    else session.unlockBiometric(authenticated, ::complete)
                }
                override fun onAuthenticationFailed() {
                    // A rejected scan is nonterminal: the system prompt permits another attempt.
                    BiometricDiagnostics.record(BiometricStage.PROMPT_FAILED)
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    BiometricDiagnostics.record(BiometricStage.PROMPT_ERROR, code = errorCode)
                    if (!awaitingPrompt) return
                    val outcome = when (errorCode) {
                        BiometricPrompt.ERROR_CANCELED, BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> BiometricOutcome.CANCELLED
                        BiometricPrompt.ERROR_LOCKOUT, BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> BiometricOutcome.LOCKOUT
                        else -> BiometricOutcome.UNAVAILABLE
                    }
                    complete(BiometricResult(outcome, BiometricStage.PROMPT_ERROR, errorCode))
                }
            })
        prompt = localPrompt
        cancelPrompt = {
            BiometricDiagnostics.record(BiometricStage.SESSION_CANCELLED)
            complete(BiometricResult(BiometricOutcome.SESSION_EXPIRED, BiometricStage.SESSION_CANCELLED))
            localPrompt.cancelAuthentication()
        }
        try {
            BiometricDiagnostics.record(BiometricStage.PROMPT_LAUNCH)
            localPrompt.authenticate(BiometricPrompt.PromptInfo.Builder()
                .setTitle(if (enroll) "Enable biometric unlock" else "Unlock Keeppeer")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .setNegativeButtonText("Use master password").build(), BiometricPrompt.CryptoObject(cipher))
        } catch (error: Exception) {
            complete(BiometricDiagnostics.failure(BiometricStage.PROMPT_LAUNCH, error))
        }
    }
}
