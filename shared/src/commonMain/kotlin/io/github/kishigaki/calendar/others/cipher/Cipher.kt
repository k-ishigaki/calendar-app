package io.github.kishigaki.calendar.others.cipher

/**
 * Cipher is an interface for encryption and decryption of data.
 * It provides methods to encrypt and decrypt byte arrays.
 */
interface Cipher {
    /**
     * Encrypts the given byte array.
     * @param data The byte array to encrypt.
     * @return The encrypted byte array.
     */
    fun encrypt(data: ByteArray): ByteArray

    /**
     * Decrypts the given byte array.
     * @param data The byte array to decrypt.
     * @return The decrypted byte array, or null if decryption fails.
     */
    fun decrypt(data: ByteArray): ByteArray?
}
