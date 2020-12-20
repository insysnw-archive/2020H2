package com.handtruth.net.lab3.message

import com.handtruth.net.lab3.options.Option
import com.handtruth.net.lab3.options.OptionCompanion
import com.handtruth.net.lab3.util.validateNotNull
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
    open val options: Map<Byte, Option> get() = emptyMap()

    /**
     * Функция, кодирующая и записывающая тело данного сообщения в синхронный поток.
     */
    abstract fun writeBody(output: Output)

    /**
     * Найти конкретную опцию в сообщении по её типу. Если опция отсутствует, то будет возвращено значение null.
     *
     * @param O тип опции, которую следует найти в сообщении; тип должен иметь companion объект [OptionCompanion]
     * @return найденная опция или null, если опция не присутствует
     */
    inline fun <reified O : Option> getOptionOrNull(): O? {
        val option = O::class.companionObjectInstance as OptionCompanion
        return options[option.optionId] as O?
    }

    /**
     * Найти конкретную опцию в сообщении по её типу. Опция обязана присутствовать в
     * сообщении иначе будет выброшено исключение [com.handtruth.net.lab3.util.MessageFormatException]
     *
     * @param O тип опции, которую следует найти в сообщении; тип должен иметь companion объект [OptionCompanion]
     * @return найденная опция
     */
    inline fun <reified O : Option> getOption(): O =
        validateNotNull(getOptionOrNull()) { "no option ${O::class.simpleName} in message" }
}
