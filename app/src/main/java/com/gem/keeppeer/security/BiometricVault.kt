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
    private val file by lazy { AtomicEnvelopeFile(File(context.noBackupFilesDir, "biometric-envelope.bin")) }
    private fun store() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    fun availabilityCode() = BiometricManager.from(context).canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG)
    fun available() = availabilityCode() == BiometricManager.BIOMETRIC_SUCCESS
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
        BiometricDiagnostics.at(BiometricStage.WRAPPER_REMOVE) { check(disable()) }
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true).setInvalidatedByBiometricEnrollment(true)
        if (Build.VERSION.SDK_INT >= 30) spec.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        else {
            @Suppress("DEPRECATION")
            spec.setUserAuthenticationValidityDurationSeconds(-1)
        }
        val key = BiometricDiagnostics.at(BiometricStage.KEY_GENERATE) {
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                init(spec.build())
            }.generateKey()
        }
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            BiometricDiagnostics.at(BiometricStage.ENCRYPT_INIT) { init(Cipher.ENCRYPT_MODE, key) }
        }
    }
    fun save(cipher: Cipher, key: ByteArray) {
        // Auth-per-operation Keystore must authorize the operation before any updates.
        BiometricDiagnostics.at(BiometricStage.ENCRYPT_AAD) { cipher.updateAAD(AAD) }
        val encrypted = BiometricDiagnostics.at(BiometricStage.ENCRYPT_FINAL) { cipher.doFinal(key) }
        BiometricDiagnostics.at(BiometricStage.WRAPPER_WRITE) {
            require(cipher.iv.size == 12 && encrypted.size == 48)
            file.write(byteArrayOf(1) + cipher.iv + encrypted)
        }
    }
    fun decryptionCipher(): Cipher {
        val bytes = BiometricDiagnostics.at(BiometricStage.WRAPPER_READ) { file.read() }
        require(bytes.size == 61 && bytes[0] == 1.toByte())
        val key = BiometricDiagnostics.at(BiometricStage.KEY_LOAD) {
            store().getKey(alias, null) as? SecretKey ?: error("Biometric key unavailable")
        }
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            BiometricDiagnostics.at(BiometricStage.DECRYPT_INIT) {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes.copyOfRange(1, 13)))
            }
        }
    }
    fun decrypt(cipher: Cipher): ByteArray {
        val bytes = BiometricDiagnostics.at(BiometricStage.WRAPPER_READ) { file.read() }
        require(bytes.size == 61 && bytes[0] == 1.toByte())
        BiometricDiagnostics.at(BiometricStage.DECRYPT_AAD) { cipher.updateAAD(AAD) }
        return BiometricDiagnostics.at(BiometricStage.DECRYPT_FINAL) {
            cipher.doFinal(bytes, 13, 48).also { require(it.size == 32) }
        }
    }
    companion object {
        private val AAD = "Keeppeer biometric vault key v1".toByteArray(Charsets.UTF_8)
    }
}
