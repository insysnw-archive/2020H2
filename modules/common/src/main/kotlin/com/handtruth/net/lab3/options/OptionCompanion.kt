package com.handtruth.net.lab3.options

import io.ktor.utils.io.core.*

/**
 * Базовый класс для объектов компаньонов каждого типа опции. Обязан присутствовать
 * у каждого final класса опции. Нельзя инстанцировать опцию, у которой нет объекта компаньона,
 * типа [OptionCompanion].
 *
 * @property optionId уникальный тип опции для прикладного протокола; может иметь значения в диапазоне [1:127]
 * @see Option
 * @sample com.handtruth.net.lab3.sample.sampleOption
 */
abstract class OptionCompanion(val optionId: Byte) {
    init {
        require(optionId != END_OF_OPTIONS) {
            "option id #$END_OF_OPTIONS is used to mark the end of options"
        }
        require(optionId > 0) {
            "forbidden option id #$optionId"
        }
    }

    /**
     * Читает данные опции из тела **option_body**, которое представлено в виде пакета.
     *
     * @param input данные тела опции и их размер
     * @return опция типа, за который отвечает данный объект
     */
    abstract fun read(input: ByteReadPacket): Option
}
