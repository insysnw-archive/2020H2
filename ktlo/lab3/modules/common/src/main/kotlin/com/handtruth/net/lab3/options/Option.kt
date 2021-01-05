package com.handtruth.net.lab3.options

import io.ktor.utils.io.core.*
import kotlin.reflect.full.companionObjectInstance

/**
 * Базовый класс для всех типов опций любого протокола приложения.
 *
 * @see OptionCompanion
 * @sample com.handtruth.net.lab3.sample.sampleOption
 */
abstract class Option {

    /**
     * Объект, который хранит id опции и может считать опции данного типа из [Input].
     */
    val companion = this::class.companionObjectInstance as? OptionCompanion
        ?: error("option has illegal factory companion")

    /**
     * Байт с кодом типа опции **option_id**.
     */
    val id get() = companion.optionId

    /**
     * Функция, кодирующая и записывающая данные в текущей опции в синхронный поток.
     * Размер тела **option_body** этой опции будет определён по количеству записанных октетов.
     */
    abstract fun write(output: Output)
}
