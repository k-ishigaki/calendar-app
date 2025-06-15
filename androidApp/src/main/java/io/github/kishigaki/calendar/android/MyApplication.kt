package io.github.kishigaki.calendar.android

import android.app.Application
import io.github.kishigaki.calendar.AppContext

class MyApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        AppContext.setUp(this)
    }
}