package io.github.kishigaki.calendar.others.perpetuation

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

/**
 * DataStoreKeyValueStorage is a wrapper around DataStore<Preferences> that implements the KeyValueStorage interface.
 * It provides methods to save, load, remove, and clear values associated with keys in a DataStore.
 * @param dataStore The DataStore<Preferences> instance to use for storage.
 */
class DataStoreKeyValueStorage(
    private val dataStore: DataStore<Preferences>
) : KeyValueStorage {

    override suspend fun <T: Any> save(key: StorageKey<T>, value: T) {
        dataStore.updateData {
            it.toMutablePreferences().apply {
                this[key.toPreferenceKey()] = value
            }
        }
    }

    override suspend fun <T: Any> load(key: StorageKey<T>): T? =
        dataStore.data.map { it[key.toPreferenceKey()] }.firstOrNull()

    override suspend fun <T: Any> remove(key: StorageKey<T>) {
        dataStore.updateData {
            it.toMutablePreferences().apply {
                this.remove(key.toPreferenceKey())
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
fun <T: Any> StorageKey<T>.toPreferenceKey(): Preferences.Key<T> = when (type) {
    String::class -> stringPreferencesKey(name)
    Int::class -> intPreferencesKey(name)
    Long::class -> longPreferencesKey(name)
    Float::class -> floatPreferencesKey(name)
    Double::class -> doublePreferencesKey(name)
    Boolean::class -> booleanPreferencesKey(name)
    ByteArray::class -> byteArrayPreferencesKey(name)
    else -> throw IllegalArgumentException("Unsupported type: $type")
} as Preferences.Key<T>

fun createDataStore(): DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
    produceFile = { createProducePath().toPath() }
)

internal const val DATA_STORE_FILE_NAME = "datastore.preferences_pb"
internal expect fun createProducePath(): String
