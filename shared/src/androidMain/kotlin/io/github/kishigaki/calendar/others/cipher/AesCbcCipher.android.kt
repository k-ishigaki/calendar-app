package io.github.kishigaki.calendar.others.cipher

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

actual class AesCommonKey(val rawValue: SecretKey)

actual fun createKeyProvider() = object : PersistentAesCommonKeyProvider {
    override fun loadOrCreateKey(): AesCommonKey {
        @Suppress("SpellCheckingInspection")
        val keyAlias = "com.kishigaki.calendar.key.aes"
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }
        val key = keyStore.getKey(keyAlias, null) as? SecretKey
        if (key != null) {
            return AesCommonKey(key)
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        val parameterSpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).apply {
            setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            setUserAuthenticationRequired(false)
        }.build()

        keyGenerator.init(parameterSpec)
        return AesCommonKey(keyGenerator.generateKey())
    }
}

internal actual fun aesCbcEncrypt(key: AesCommonKey, iv: ByteArray, data: ByteArray): ByteArray {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val ivSpec = IvParameterSpec(iv)
    cipher.init(Cipher.ENCRYPT_MODE, key.rawValue, ivSpec)
    return cipher.doFinal(data)
}

internal actual fun aesCbcDecrypt(key: AesCommonKey, iv: ByteArray, data: ByteArray): ByteArray? {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val ivSpec = IvParameterSpec(iv)
    cipher.init(Cipher.DECRYPT_MODE, key.rawValue, ivSpec)
    return cipher.doFinal(data)
}

internal actual fun generateRandomBytes(size: Int): ByteArray {
    val random = SecureRandom()
    val bytes = ByteArray(size)
    random.nextBytes(bytes)
    return bytes
}
