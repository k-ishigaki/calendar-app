package io.github.kishigaki.calendar.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.kishigaki.calendar.others.cipher.AesCbcCipher
import io.github.kishigaki.calendar.others.cipher.Cipher
import io.github.kishigaki.calendar.others.cipher.createKeyProvider
import io.github.kishigaki.calendar.others.perpetuation.DataStoreKeyValueStorage
import io.github.kishigaki.calendar.others.perpetuation.EncryptedKeyValueStorage
import io.github.kishigaki.calendar.others.perpetuation.KeyValueStorage
import io.github.kishigaki.calendar.others.perpetuation.createDataStore
import org.koin.dsl.module

val othersModule = module {
    single<DataStore<Preferences>> { createDataStore() }
    single<Cipher> { AesCbcCipher(createKeyProvider()) }
    single<KeyValueStorage> { EncryptedKeyValueStorage(DataStoreKeyValueStorage(get()), get()) }
}
