package io.github.kishigaki.calendar.others.perpetuation

import io.github.kishigaki.calendar.AppContext

internal actual fun createProducePath(): String = AppContext.get().filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath
