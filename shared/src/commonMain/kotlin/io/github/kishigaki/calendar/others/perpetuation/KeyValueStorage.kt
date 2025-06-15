package io.github.kishigaki.calendar.others.perpetuation

import kotlin.reflect.KClassifier

/**
 * KeyValueStorage is an interface for a key-value storage system.
 * It provides methods to load, save, remove, and clear values associated with keys.
 */
interface KeyValueStorage {

    /**
     * Save the value associated with the key.
     * @param key The key to save the value for.
     * @param value The value to save.
     */
    suspend fun <T: Any> save(key: StorageKey<T>, value: T)

    /**
     * Load the value associated with the key.
     * @param key The key to load the value for.
     * @return The value associated with the key, or null if not found.
     */
    suspend fun <T: Any> load(key: StorageKey<T>): T?

    /**
     * Remove the value associated with the key.
     * @param key The key to remove the value for.
     */
    suspend fun <T: Any> remove(key: StorageKey<T>)

    /**
     * Check if the key exists in the data store.
     * @param key The key to check for existence.
     * @return True if the key exists, false otherwise.
     */
    suspend fun contains(key: StorageKey<*>): Boolean {
        return load(key) != null
    }
}

/**
 * StorageKey is a wrapper for keys used in key-value storage.
 * It contains the name of the key and its type.
 * @param T The type of the value associated with the key.
 * @param name The name of the key.
 * @param type The type of the value associated with the key. must be a same type as T.
 */
class StorageKey<T: Any>(val name: String, val type: KClassifier) {

    companion object {
        /**
         * Create a StorageKey with the specified name and type.
         * @param T The type of the value associated with the key.
         * @param name The name of the key.
         * @return A StorageKey instance.
         */
        inline fun <reified T: Any> create(name: String): StorageKey<T> {
            return StorageKey(name, T::class)
        }
    }
}
