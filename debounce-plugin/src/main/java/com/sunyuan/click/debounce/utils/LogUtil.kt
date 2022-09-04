package com.sunyuan.click.debounce.utils

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import java.util.function.Consumer

object LogUtil {
    lateinit var sLogger: Logger

    fun init(logger: Logger) {
        sLogger = logger
    }

    fun warn(msg: String) {
        if (!canPrintLog()) {
            return
        }
        sLogger.warn(msg)
    }

    fun error(msg: String) {
        if (!canPrintLog()) {
            return
        }
        sLogger.error(msg)
    }

    private fun canPrintLog(): Boolean {
        if (!this::sLogger.isInitialized) {
            return false
        }
        return sLogger.isEnabled(LogLevel.WARN)
    }
}