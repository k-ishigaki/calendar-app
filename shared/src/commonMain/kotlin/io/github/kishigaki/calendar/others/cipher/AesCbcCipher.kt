package io.github.kishigaki.calendar.others.cipher

/**
 * AesCbcCipher is an implementation of the Cipher interface that uses AES encryption in CBC mode.
 * It generates a random IV for each encryption operation and prepends it to the encrypted data.
 */
class AesCbcCipher(private val keyProvider: PersistentAesCommonKeyProvider) : Cipher {

    companion object {
        private const val SIZE_OF_IV = 16 // AES block size in bytes
    }

    override fun encrypt(data: ByteArray): ByteArray {
        val key = keyProvider.loadOrCreateKey()
        val iv = generateRandomBytes(SIZE_OF_IV)
        // Encrypt the data using AES in CBC mode with the generated IV
        return iv + aesCbcEncrypt(key, iv, data)
    }

    override fun decrypt(data: ByteArray): ByteArray? {
        val key = keyProvider.loadOrCreateKey()
        // The first 16 bytes of the data is the IV
        val iv = data.copyOfRange(0, SIZE_OF_IV)
        // The rest of the data is the encrypted content
        val encryptedData = data.copyOfRange(SIZE_OF_IV, data.size)
        return aesCbcDecrypt(key, iv, encryptedData)
    }
}

/**
 * interface for providing a persistent AES common key.
 */
interface PersistentAesCommonKeyProvider {
    /**
     * Loads or creates a new AES common key.
     * @return The loaded or newly created AES common key.
     */
    fun loadOrCreateKey(): AesCommonKey
}

expect fun createKeyProvider(): PersistentAesCommonKeyProvider

/// AesCommonKey is a placeholder for the actual implementation of the AES common key.
expect class AesCommonKey

internal expect fun generateRandomBytes(size: Int): ByteArray
internal expect fun aesCbcEncrypt(key: AesCommonKey, iv: ByteArray, data: ByteArray): ByteArray
internal expect fun aesCbcDecrypt(key: AesCommonKey, iv: ByteArray, data: ByteArray): ByteArray?
