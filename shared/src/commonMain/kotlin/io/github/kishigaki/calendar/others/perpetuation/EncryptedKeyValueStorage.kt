package io.github.kishigaki.calendar.others.perpetuation

import io.github.kishigaki.calendar.others.cipher.Cipher
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * EncryptedKeyValueStorage is a wrapper around KeyValueStorage that encrypts and decrypts data using a Cipher.
 * It uses the provided Cipher to encrypt data before saving it and decrypt data after loading it.
 * @param base The base KeyValueStorage to use for saving and loading data.
 * @param cipher The Cipher to use for encryption and decryption.
 */
class EncryptedKeyValueStorage(
    private val base: KeyValueStorage,
    private val cipher: Cipher
) : KeyValueStorage {

    override suspend fun <T : Any> save(key: StorageKey<T>, value: T) {
        val json = Json.encodeToString(key.serializer(), value)
        val encryptedData = cipher.encrypt(json.encodeToByteArray())
        base.save(StorageKey.create(key.name), encryptedData)
    }

    override suspend fun <T : Any> load(key: StorageKey<T>): T? =
        base.load<ByteArray>(StorageKey.create(key.name))
            ?.let { cipher.decrypt(it) }
            ?.let { Json.decodeFromString(key.serializer(), it.decodeToString()) }

    override suspend fun <T : Any> remove(key: StorageKey<T>) {
        // Remove the underlying encrypted data stored as ByteArray
        base.remove(StorageKey.create<ByteArray>(key.name))
    }
}

fun <T: Any> StorageKey<T>.serializer(): KSerializer<T> {
    @Suppress("UNCHECKED_CAST")
    return when (type) {
        String::class -> Json.serializersModule.serializer<String>()
        Int::class -> Json.serializersModule.serializer<Int>()
        Long::class -> Json.serializersModule.serializer<Long>()
        Float::class -> Json.serializersModule.serializer<Float>()
        Double::class -> Json.serializersModule.serializer<Double>()
        Boolean::class -> Json.serializersModule.serializer<Boolean>()
        else -> throw IllegalArgumentException("Unsupported type: $type")
    } as KSerializer<T>
}
