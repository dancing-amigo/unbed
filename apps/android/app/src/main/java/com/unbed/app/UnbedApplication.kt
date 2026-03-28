package com.unbed.app

import android.app.Application
import com.unbed.app.logging.LoggingExceptionHandler

class UnbedApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
        Thread.setDefaultUncaughtExceptionHandler(
            LoggingExceptionHandler(
                logger = appContainer.logger,
                delegate = Thread.getDefaultUncaughtExceptionHandler(),
            ),
        )
        appContainer.logger.info("UnbedApplication", "Application started")
    }
}
