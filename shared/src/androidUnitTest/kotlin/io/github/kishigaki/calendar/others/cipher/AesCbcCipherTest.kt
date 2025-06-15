package io.github.kishigaki.calendar.others.cipher

import javax.crypto.spec.SecretKeySpec
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class AesCbcCipherTest {

    companion object {
        // This is a test key and IV. In a real application, you should use a secure key management system.
        private val keyByteArray = "000102030405060708090a0b0c0d0e0f00102030405060708090a0b0c0d0e0f0".hexToByteArray()
        private val key = AesCommonKey(SecretKeySpec(keyByteArray, 0, keyByteArray.size, "AES"))
        private val iv = "000102030405060708090a0b0c0d0e0f".hexToByteArray()
    }

    private class FakeKeyProvider : PersistentAesCommonKeyProvider {
        override fun loadOrCreateKey() = key
    }

    /**
     * Test the encryption and decryption process.
     */
    @Test
    fun testEncryptDecrypt() {
        val data = "Hello, World!".encodeToByteArray()

        val encryptedData = aesCbcEncrypt(key, iv, data)
        val decryptedData = aesCbcDecrypt(key, iv, encryptedData)

        // Encrypted data calculated with the above key and IV
        // This is the expected output of the encryption.
        assertEquals("785fd2ae5dbe52398feb094c21e9e0d5", encryptedData.toHexString())

        assertNotNull(encryptedData)
        assertFalse(encryptedData.contentEquals(data))
        assertContentEquals(data, decryptedData)
    }

    /**
     * Test the AesCbcCipher class.
     */
    @Test
    fun testCipher() {
        val data = "Hello, World!".encodeToByteArray()

        val cipher = AesCbcCipher(FakeKeyProvider())
        val encryptedData = cipher.encrypt(data)
        val decryptedData = cipher.decrypt(encryptedData)

        assertNotNull(encryptedData)
        assertFalse(encryptedData.contentEquals(data))
        assertContentEquals(data, decryptedData)
    }
}

private fun ByteArray.toHexString(): String {
    @Suppress("SpellCheckingInspection")
    val hexChars = "0123456789abcdef"
    val result = StringBuilder(size * 2)
    for (byte in this) {
        val v = byte.toInt() and 0xFF
        result.append(hexChars[v ushr 4])  // upper 4 bits
        result.append(hexChars[v and 0x0F]) // under 4 bits
    }
    return result.toString()
}

private fun String.hexToByteArray(): ByteArray {
    return chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}