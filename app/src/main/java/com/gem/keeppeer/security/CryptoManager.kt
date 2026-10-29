package com.gem.keeppeer.security

import org.bouncycastle.crypto.PBEParametersGenerator
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.bouncycastle.crypto.params.KeyParameter
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Version 1: big-endian version, KDF ID, iterations, salt[16], nonce[12], GCM[48].
 * The entire header is AAD. API 24–25 uses BC; API 26+ uses platform SHA256 PBKDF2.
 */
class CryptoManager {
    private val random = SecureRandom()
    fun randomKey() = ByteArray(32).also(random::nextBytes)

    fun wrap(password: CharArray, databaseKey: ByteArray): ByteArray {
        require(databaseKey.size == 32)
        val salt = ByteArray(16).also(random::nextBytes)
        val nonce = ByteArray(12).also(random::nextBytes)
        val header = ByteBuffer.allocate(40).putInt(1).putInt(1).putInt(ITERATIONS)
            .put(salt).put(nonce).array()
        val kek = derive(password, salt, ITERATIONS)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(kek, "AES"), GCMParameterSpec(128, nonce))
            cipher.updateAAD(header)
            header + cipher.doFinal(databaseKey)
        } finally { kek.fill(0) }
    }

    fun unwrap(password: CharArray, envelope: ByteArray): ByteArray {
        require(envelope.size == 88) { "Invalid envelope size" }
        val input = ByteBuffer.wrap(envelope)
        require(input.int == 1 && input.int == 1) { "Unsupported envelope version or KDF" }
        val iterations = input.int
        // Bounds prevent malformed public metadata from causing unbounded KDF work.
        require(iterations in 210_000..2_000_000)
        val salt = ByteArray(16).also(input::get)
        val nonce = ByteArray(12).also(input::get)
        val ciphertext = ByteArray(48).also(input::get)
        val kek = derive(password, salt, iterations)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(kek, "AES"), GCMParameterSpec(128, nonce))
            cipher.updateAAD(envelope.copyOfRange(0, 40))
            cipher.doFinal(ciphertext).also { require(it.size == 32) }
        } finally { kek.fill(0) }
    }

    internal fun derive(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val spec = javax.crypto.spec.PBEKeySpec(password, salt, iterations, 256)
            return try {
                javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            } finally { spec.clearPassword() }
        }
        return derivePortable(password, salt, iterations)
    }

    internal fun derivePortable(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val bytes = PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(password)
        return try {
            val generator = PKCS5S2ParametersGenerator(SHA256Digest())
            generator.init(bytes, salt, iterations)
            (generator.generateDerivedParameters(256) as KeyParameter).key
        } finally { bytes.fill(0) }
    }

    companion object { const val ITERATIONS = 600_000 }
}
