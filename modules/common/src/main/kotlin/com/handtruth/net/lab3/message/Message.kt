package com.handtruth.net.lab3.message

import com.handtruth.net.lab3.options.Option
import io.ktor.utils.io.core.*
import kotlin.reflect.full.companionObjectInstance

/**
 * Базовый класс для всех типов сообщений любого протокола приложения.
 *
 * @see MessageCompanion
 * @sample com.handtruth.net.lab3.sample.sampleMessage
 */
abstract class Message {
    /**
     * Объект, который хранит id сообщения и может считать сообщения данного типа из [Input].
     */
    val companion = this::class.companionObjectInstance as? MessageCompanion
        ?: error("message has illegal factory companion")

    /**
     * Байт с кодом типа сообщения **message_id**.
     */
    val id get() = companion.messageId

    /**
     * Опции сообщения.
     */
    open val options: List<Option> get() = emptyList()

    /**
     * Функция, кодирующая и записывающая тело данного сообщения в синхронный поток.
     */
    abstract fun writeBody(output: Output)
}