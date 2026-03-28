package com.unbed.app.logging

import android.util.Log

interface AppLogger {
    fun info(
        tag: String,
        message: String,
    )

    fun warn(
        tag: String,
        message: String,
    )

    fun error(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    )
}

class AndroidAppLogger : AppLogger {
    override fun info(
        tag: String,
        message: String,
    ) {
        Log.i(tag, message)
    }

    override fun warn(
        tag: String,
        message: String,
    ) {
        Log.w(tag, message)
    }

    override fun error(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        Log.e(tag, message, throwable)
    }
}

class LoggingExceptionHandler(
    private val logger: AppLogger,
    private val delegate: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(
        thread: Thread,
        throwable: Throwable,
    ) {
        logger.error(
            tag = "UnbedCrash",
            message = "Uncaught exception on thread ${thread.name}",
            throwable = throwable,
        )
        delegate?.uncaughtException(thread, throwable)
    }
}
