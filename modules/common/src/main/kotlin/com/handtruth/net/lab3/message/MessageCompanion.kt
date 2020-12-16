package com.handtruth.net.lab3.message

import com.handtruth.net.lab3.options.Option
import io.ktor.utils.io.core.*

/**
 * Базовый класс для объектов компаньонов каждого типа сообщения. Обязан присутствовать
 * у каждого final класса опции. Нельзя инстанцировать сообщение, у которого нет объекта компаньона
 * типа [MessageCompanion].
 *
 * @property messageId уникальный тип сообщения для прикладного протокола; может иметь значения в диапазоне [0:127]
 * @see Message
 * @sample com.handtruth.net.lab3.sample.sampleMessage
 */
abstract class MessageCompanion(val messageId: Byte) {
    init {
        require(messageId >= 0) {
            "forbidden message id #$messageId"
        }
    }

    /**
     * Читает данные сообщения из тела **body**, которое представлено в виде пакета.
     *
     * @param input тело сообщения, которое нужно декодировать
     * @param options декодированные опции
     * @return сообщение типа, за который отвечает данный объект
     */
    abstract fun read(input: ByteReadPacket, options: List<Option>): Message
}