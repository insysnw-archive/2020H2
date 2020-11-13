package com.github.antoshka77.schat

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory

internal fun setLogLevel(level: Level) {
    if (level == Level.DEBUG || level == Level.TRACE) {
        System.setProperty("kotlinx.coroutines.debug", "")
    }
    val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    root.level = level
}
