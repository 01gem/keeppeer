package com.gem.keeppeer.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** AtomicFile rolls back an incomplete write, including after process death. */
class AtomicEnvelopeFile(file: File) {
    private val atomic = AtomicFile(file)
    fun exists() = atomic.baseFile.exists() || File(atomic.baseFile.path + ".bak").exists()
    fun read(): ByteArray = atomic.openRead().use {
        val bytes = it.readBytesBounded(8192)
        require(bytes.isNotEmpty())
        bytes
    }
    @Synchronized fun write(bytes: ByteArray, beforeCommit: () -> Unit = {}) {
        var stream: FileOutputStream? = null
        try {
            stream = atomic.startWrite()
            stream.write(bytes)
            stream.fd.sync()
            beforeCommit()
            atomic.finishWrite(stream)
        } catch (failure: Exception) {
            atomic.failWrite(stream)
            throw failure
        }
    }
    fun delete() = atomic.delete()
}

private fun java.io.InputStream.readBytesBounded(limit: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(1024)
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        require(output.size() + count <= limit) { "Oversized envelope" }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

/** Private no-backup storage + an independent, non-auth-bound Keystore key. */
class EnvelopeStore(context: Context, private val alias: String = "keeppeer.storage.v1",
                    name: String = "vault-envelope.bin") {
    private val file = AtomicEnvelopeFile(File(context.noBackupFilesDir, name))
    private fun keyStore() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    fun exists() = file.exists()
    fun initializeStorageKey() {
        if (keyStore().containsAlias(alias)) return
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    private fun key() = (keyStore().getKey(alias, null) as? SecretKey)
        ?: error("Device storage key is unavailable; existing vault preserved")
    data class Record(val envelope: ByteArray, val creationPending: Boolean)
    fun readRecord(): Record {
        val bytes = file.read()
        require(bytes.size == 121 && ByteBuffer.wrap(bytes).int == 1 && bytes[4].toInt() in 0..1)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(5, 17)))
        cipher.updateAAD(AAD + bytes.copyOfRange(0, 5))
        return Record(cipher.doFinal(bytes, 17, bytes.size - 17), bytes[4].toInt() == 1)
    }
    fun read(): ByteArray = readRecord().envelope
    fun write(envelope: ByteArray, creationPending: Boolean = false, beforeCommit: () -> Unit = {}) {
        require(envelope.size == 88)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val header = ByteBuffer.allocate(5).putInt(1).put(if (creationPending) 1.toByte() else 0.toByte()).array()
        cipher.updateAAD(AAD + header)
        file.write(header + cipher.iv + cipher.doFinal(envelope), beforeCommit)
    }
    companion object { private val AAD = "Keeppeer device envelope v1".toByteArray(Charsets.UTF_8) }
}
