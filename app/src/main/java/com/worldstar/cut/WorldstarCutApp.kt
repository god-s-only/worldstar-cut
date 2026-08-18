package com.worldstar.cut

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application entry point.
 * Hilt generates the component graph from this class.
 */
@HiltAndroidApp
class WorldstarCutApp : Application() {

    override fun onCreate() {
        super.onCreate()
        initLogging()
    }

    private fun initLogging() {
        if (BuildConfig.IS_DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
