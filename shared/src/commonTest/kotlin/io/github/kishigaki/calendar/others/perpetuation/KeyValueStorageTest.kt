package io.github.kishigaki.calendar.others.perpetuation

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import io.github.kishigaki.calendar.others.cipher.Cipher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KeyValueStorageTest {

    private lateinit var keyValueStorage : KeyValueStorage

    private class FakeDataStore : DataStore<Preferences> {
        private val _state = MutableStateFlow(emptyPreferences())

        override val data: Flow<Preferences> get() = _state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            val newValue = transform(_state.value)
            _state.value = newValue
            return newValue
        }
    }

    private class FakeCipher : Cipher {
        override fun encrypt(data: ByteArray) = data
        override fun decrypt(data: ByteArray) = data
    }

    @BeforeTest
    fun setup() {
        keyValueStorage = EncryptedKeyValueStorage(
            base = DataStoreKeyValueStorage(FakeDataStore()),
            cipher = FakeCipher()
        )
    }

    @Test
    fun savesAndLoadsValueCorrectly() = runTest {
        val originalValue = 123
        val key = StorageKey.create<Int>("intKey")
        keyValueStorage.save(key, originalValue)
        val loadedValue = keyValueStorage.load(key)
        assertEquals(originalValue, loadedValue)
    }

    @Test
    fun returnsNullForNonExistentKey() = runTest {
        val key = StorageKey.create<String>("stringKey")
        val loadedValue = keyValueStorage.load(key)
        assertNull(loadedValue)
    }

    @Test
    fun removesKeyCorrectly() = runTest {
        val key = StorageKey.create<Int>("intKey")
        keyValueStorage.save(key, 999)
        keyValueStorage.remove(key)
        val loadedValue = keyValueStorage.load(key)
        assertNull(loadedValue)
    }

    @Test
    fun containsKey() = runTest {
        val intKey = StorageKey.create<Int>("intKey")
        val stringKey = StorageKey.create<String>("stringKey")
        keyValueStorage.save(intKey, 100)
        assertTrue(keyValueStorage.contains(intKey))
        assertFalse(keyValueStorage.contains(stringKey))
    }
}
