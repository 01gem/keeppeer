package com.gem.keeppeer.security

import android.util.Log
import android.os.Build
import com.gem.keeppeer.BuildConfig
import kotlinx.coroutines.CancellationException
import java.io.IOException
import android.security.keystore.KeyPermanentlyInvalidatedException
import javax.crypto.AEADBadTagException

enum class BiometricStage {
    PASSWORD_INPUT, SESSION_CHECK, PASSWORD_ENVELOPE_READ, PASSWORD_VERIFY, AVAILABILITY,
    WRAPPER_REMOVE, KEY_GENERATE, KEY_LOAD, ENCRYPT_INIT, ENCRYPT_AAD, DECRYPT_INIT, DECRYPT_AAD,
    PROMPT_LAUNCH, PROMPT_SUCCEEDED, PROMPT_FAILED, PROMPT_ERROR, CIPHER_BINDING,
    APPROVAL_CHECK, FOREGROUND_CHECK, ENCRYPT_FINAL, WRAPPER_WRITE, WRAPPER_READ,
    DECRYPT_FINAL, DATABASE_OPEN, COMPLETE, ACTIVITY_START, ACTIVITY_STOP,
    ACTIVITY_DESTROY, SESSION_GENERATION, SESSION_BACKGROUND, SESSION_CANCELLED
}
enum class BiometricOutcome { SUCCESS, EMPTY_PASSWORD, WRONG_PASSWORD, CANCELLED, SESSION_EXPIRED,
    UNAVAILABLE, LOCKOUT, KEY_INVALIDATED, KEYSTORE_ERROR, IO_ERROR, CRYPTO_ERROR, CIPHER_MISMATCH }
data class BiometricResult(
    val outcome: BiometricOutcome,
    val stage: BiometricStage,
    val platformCode: Int? = null,
) {
    val success get() = outcome == BiometricOutcome.SUCCESS
    fun message(): Int = when (outcome) {
        BiometricOutcome.SUCCESS -> com.gem.keeppeer.R.string.bio_success
        BiometricOutcome.EMPTY_PASSWORD -> com.gem.keeppeer.R.string.bio_empty
        BiometricOutcome.WRONG_PASSWORD -> com.gem.keeppeer.R.string.bio_wrong
        BiometricOutcome.CANCELLED -> com.gem.keeppeer.R.string.bio_cancelled
        BiometricOutcome.SESSION_EXPIRED -> com.gem.keeppeer.R.string.bio_expired
        BiometricOutcome.UNAVAILABLE -> com.gem.keeppeer.R.string.bio_unavailable
        BiometricOutcome.LOCKOUT -> com.gem.keeppeer.R.string.bio_lockout
        BiometricOutcome.KEY_INVALIDATED -> com.gem.keeppeer.R.string.bio_invalidated
        BiometricOutcome.KEYSTORE_ERROR -> com.gem.keeppeer.R.string.bio_keystore
        BiometricOutcome.IO_ERROR -> com.gem.keeppeer.R.string.bio_io
        BiometricOutcome.CRYPTO_ERROR -> com.gem.keeppeer.R.string.bio_crypto
        BiometricOutcome.CIPHER_MISMATCH -> com.gem.keeppeer.R.string.bio_mismatch
    }
    companion object {
        fun ok() = BiometricResult(BiometricOutcome.SUCCESS, BiometricStage.COMPLETE)
    }
}
class BiometricFailure(val stage: BiometricStage, cause: Exception) : Exception(null, cause)

object BiometricDiagnostics {
    // Never pass a Throwable to Log: messages, causes and object dumps can contain secrets.
    fun record(stage: BiometricStage, error: Exception? = null, code: Int? = null) {
        if (BuildConfig.DEBUG) Log.d("KeeppeerBiometric",
            "stage=${stage.name}" + (error?.let { " exception=${it.javaClass.simpleName}" } ?: "") +
                (code?.takeIf { it in 0..18 || it == -1 || it == -2 }?.let { " code=$it" } ?: ""))
    }
    fun failure(stage: BiometricStage, error: Exception): BiometricResult {
        val at = (error as? BiometricFailure)?.stage ?: stage
        val cause = (error as? BiometricFailure)?.cause as? Exception ?: error
        val chain = generateSequence(cause as Throwable) { it.cause }.take(8).toList()
        val platform = if (Build.VERSION.SDK_INT >= 33)
            chain.filterIsInstance<android.security.KeyStoreException>().firstOrNull()?.numericErrorCode else null
        record(at, cause, platform)
        val outcome = when {
            cause is CancellationException -> BiometricOutcome.SESSION_EXPIRED
            chain.any { it is KeyPermanentlyInvalidatedException } -> BiometricOutcome.KEY_INVALIDATED
            chain.any { it is IOException } -> BiometricOutcome.IO_ERROR
            at == BiometricStage.KEY_LOAD || chain.any {
                it.javaClass.name == "android.security.KeyStoreException" ||
                    it is java.security.KeyStoreException
            } -> BiometricOutcome.KEYSTORE_ERROR
            at == BiometricStage.PASSWORD_VERIFY && cause is AEADBadTagException -> BiometricOutcome.WRONG_PASSWORD
            else -> BiometricOutcome.CRYPTO_ERROR
        }
        return BiometricResult(outcome, at, platform?.takeIf { it in 0..18 })
    }
    inline fun <T> at(stage: BiometricStage, block: () -> T): T {
        record(stage)
        try { return block() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { throw BiometricFailure(stage, error) }
    }
}
