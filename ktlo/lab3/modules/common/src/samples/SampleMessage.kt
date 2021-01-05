package com.handtruth.net.lab3.sample

import com.google.auto.service.AutoService
import com.handtruth.net.lab3.options.*
import com.handtruth.net.lab3.types.*
import com.handtruth.net.lab3.message.*
import io.ktor.utils.io.core.*
import io.ktor.network.sockets.*

fun sampleMessage() {
    // Пример использования классов Message и MessageCompanion
    data class SampleMessage(val integer: Int, override val options: Map<Byte, Option>) : Message() {

        override fun writeBody(output: Output) {
            output.writeVarInt(integer)
        }

        @AutoService(MessageCompanion::class)
        companion object : MessageCompanion(42) {
            override fun read(input: ByteReadPacket, options: Map<Byte, Option>) = SampleMessage(input.readVarInt(), options)
        }
    }
}

suspend fun transmitterSample(socket: Socket) {
    coroutineScope {
        // Запускаем задачу преобразования в этом контексте и получаем каналы сообщений общего протокола
        val (recv: ReceiveChannel<Message>, send: SendChannel<Message>) = transmitter(socket.openReadChannel(), socket.openWriteChannel())

        launch {
            // Из первого канала читаем сообщения
            for (message in recv) {
                doSomething(message)
            }
        }

        launch {
            // Во второй канал пишем сообщения
            while (true) {
                send.send(getMessage())
            }
        }

        launch {
            // Можем паральлельно писать в тот же канал и ошибок не произойдёт
            val source: Flow<Message> = subscribeOnAdditionalMessages()
            source.collect { message: Message ->
                send.send(message)
            }
        }
    }
}
