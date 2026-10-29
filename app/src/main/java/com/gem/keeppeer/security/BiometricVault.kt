package com.gem.keeppeer.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class BiometricVault(private val context: Context, private val alias: String = "keeppeer.biometric.v1") {
    private val file = AtomicEnvelopeFile(File(context.noBackupFilesDir, "biometric-envelope.bin"))
    private fun store() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    fun available() = BiometricManager.from(context).canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    fun enabled() = file.exists()
    fun disable(): Boolean {
        file.delete()
        if (file.exists()) return false
        return runCatching {
            val keys = store()
            keys.deleteEntry(alias)
            !keys.containsAlias(alias)
        }.getOrDefault(false)
    }
    fun encryptionCipher(): Cipher {
        check(available())
        // Enrollment failures leave the password envelope untouched.
        check(disable()) { "Biometric wrapper could not be cleared" }
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true).setInvalidatedByBiometricEnrollment(true)
        if (Build.VERSION.SDK_INT >= 30) spec.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        else {
            @Suppress("DEPRECATION")
            spec.setUserAuthenticationValidityDurationSeconds(-1)
        }
        val key = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(spec.build())
        }.generateKey()
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, key)
            updateAAD(AAD)
        }
    }
    fun save(cipher: Cipher, key: ByteArray) {
        val encrypted = cipher.doFinal(key) // Requires successful per-operation biometric auth.
        file.write(byteArrayOf(1) + cipher.iv + encrypted)
    }
    fun decryptionCipher(): Cipher {
        val bytes = file.read()
        require(bytes.size == 61 && bytes[0] == 1.toByte())
        val key = store().getKey(alias, null) as? SecretKey ?: error("Biometric key unavailable")
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes.copyOfRange(1, 13)))
            updateAAD(AAD)
        }
    }
    fun decrypt(cipher: Cipher): ByteArray {
        val bytes = file.read()
        require(bytes.size == 61 && bytes[0] == 1.toByte())
        return cipher.doFinal(bytes, 13, 48).also { require(it.size == 32) }
    }
    companion object {
        private val AAD = "Keeppeer biometric vault key v1".toByteArray(Charsets.UTF_8)
    }
}
