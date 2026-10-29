package com.gem.keeppeer

import com.gem.keeppeer.security.CryptoManager
import org.junit.Assert.*
import org.junit.Test
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class CryptoManagerTest {
    private val crypto = CryptoManager()
    private fun rejected(block: () -> Unit) {
        try { block(); fail("Expected authenticated decryption to fail") } catch (_: Exception) {}
    }
    @Test fun pbkdf2MatchesPublishedSha256VectorAndPlatformUnicodeEncoding() {
        val key = crypto.derive("password".toCharArray(), "salt".toByteArray(), 1)
        assertEquals("120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b", key.joinToString("") { "%02x".format(it) })
        val password = "Synthetic 雲🙂 spaces ".toCharArray()
        val salt = ByteArray(16) { it.toByte() }
        val spec = PBEKeySpec(password, salt, 210_000, 256)
        val expected = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        assertArrayEquals(expected, crypto.derive(password, salt, 210_000))
        password.fill('\u0000'); spec.clearPassword(); expected.fill(0); key.fill(0)
    }
    @Test fun roundTripWrongPasswordAndTamperedCiphertextOrMetadata() {
        val key = crypto.randomKey()
        val envelope = crypto.wrap("synthetic master".toCharArray(), key)
        assertArrayEquals(key, crypto.unwrap("synthetic master".toCharArray(), envelope))
        rejected { crypto.unwrap("wrong synthetic".toCharArray(), envelope) }
        for (index in listOf(0, 4, 11, 12, 28, 40, 87)) {
            val tampered = envelope.copyOf()
            tampered[index] = (tampered[index].toInt() xor 1).toByte()
            rejected { crypto.unwrap("synthetic master".toCharArray(), tampered) }
        }
        rejected { crypto.unwrap(charArrayOf(), envelope.copyOf(40)) }
        key.fill(0)
    }
    @Test fun saltsAndNoncesAreFreshAndPasswordChangePreservesDatabaseKey() {
        val key = crypto.randomKey()
        val first = crypto.wrap("synthetic old".toCharArray(), key)
        val second = crypto.wrap("synthetic old".toCharArray(), key)
        assertFalse(first.copyOfRange(12, 28).contentEquals(second.copyOfRange(12, 28)))
        assertFalse(first.copyOfRange(28, 40).contentEquals(second.copyOfRange(28, 40)))
        val opened = crypto.unwrap("synthetic old".toCharArray(), first)
        val changed = crypto.wrap("synthetic new".toCharArray(), opened)
        assertArrayEquals(key, crypto.unwrap("synthetic new".toCharArray(), changed))
        rejected { crypto.unwrap("synthetic old".toCharArray(), changed) }
        key.fill(0); opened.fill(0)
    }
}
