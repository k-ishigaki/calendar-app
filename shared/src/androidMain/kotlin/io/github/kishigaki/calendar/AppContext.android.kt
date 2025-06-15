package io.github.kishigaki.calendar

import android.app.Application
import android.content.Context

/**
 * AppContext is a singleton object that holds the application context.
 * It provides a method to set up the application context and a method to retrieve it.
 */
actual object AppContext {
    private lateinit var application: Application

    /**
     * Sets up the application context.
     * This method should be called in the Application class of the Android app.
     *
     * @param application The application context to set up.
     */
    fun setUp(application: Application) {
        this.application = application
    }

    /**
     * Retrieves the application context.
     * This method should be called to get the application context.
     *
     * @return The application context.
     * @throws Exception if the application context is not initialized.
     */
    fun get(): Context {
        if (::application.isInitialized.not()) error("Application context isn't initialized")
        return application.applicationContext
    }
}
