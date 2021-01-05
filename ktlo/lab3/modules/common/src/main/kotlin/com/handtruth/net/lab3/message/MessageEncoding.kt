package com.handtruth.net.lab3.message

import com.handtruth.net.lab3.options.*
import com.handtruth.net.lab3.types.readVarInt
import com.handtruth.net.lab3.types.writeVarInt
import com.handtruth.net.lab3.util.MessageFormatException
import com.handtruth.net.lab3.util.loadObjects
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch

/**
 * Ассоциативный массив, где составлены пары **message_id** и объект
 * компаньон для каждого типа сообщения, которые известны приложению.
 * Для того, чтобы в этом массиве появился тип сообщения следует применить
 * аннотацию [com.google.auto.service.AutoService] на объекте компаньоне
 * сообщения. Также можно вручную прописать в файле сервиса имя класса
 * компаньона сообщения.
 */
val allMessages = loadObjects<MessageCompanion>().associateBy { it.messageId }

/**
 * Эта функция считывает сообщение в соответствии с форматом общего протокола.
 * Информация об известных типах сообщений берётся из значения [allMessages]. Если из потока был
 * считан неизвестный тип сообщения - будет брошено исключение.
 *
 * @receiver асинхронный поток, из которого будут прочитано и декодировано сообщение
 * @return декодированное сообщение
 */
suspend fun ByteReadChannel.readMessage(): Message {
    val messageSize = readVarInt()
    val messageId = readByte()
    val factory = allMessages[messageId]
    if (factory == null) {
        throw MessageFormatException("message type #$messageId not found")
    } else {
        readPacket(messageSize - 1).use { messageBody ->
            val options = messageBody.readOptions()
            return factory.read(messageBody, options)
        }
    }
}

/**
 * Эта функция кодирует и записывает сообщение в асинхронный поток в
 * соответствии с форматом общего протокола.
 *
 * @receiver асинхронный поток, куда будет записано закодированное сообщение
 * @param message сообщение, которое будет закодировано и записано в поток
 */
suspend fun ByteWriteChannel.writeMessage(message: Message) {
    buildPacket {
        writeByte(message.id)
        writeOptions(message.options.values)
        message.writeBody(this)
    }.use { messageBody ->
        writeVarInt(messageBody.remaining.toInt())
        writePacket(messageBody)
    }
}

/**
 * Задача, которая преобразует асинхронные потоки октетов в каналы сообщений.
 * Удобно для задач параллельного доступа к одному и тому же потоку октетов. Не требуется
 * дополнительной защиты от параллельной записи в поток, так как поток строго разделён на
 * сообщения общего протокола на логическом уровне.
 *
 * @receiver контекст в котором будут запущены задачи преобразования потоков
 * @param readChannel входящий асинхронный поток
 * @param writeChannel исходящий асинхронный поток
 * @return пара каналов отдельных сообщений общего протокола
 * @sample com.handtruth.net.lab3.sample.transmitterSample
 */
fun CoroutineScope.transmitter(
    readChannel: ByteReadChannel,
    writeChannel: ByteWriteChannel
): Pair<ReceiveChannel<Message>, SendChannel<Message>> {
    val sender = Channel<Message>()
    val receiver = Channel<Message>()

    launch {
        try {
            for (message in sender) {
                writeChannel.writeMessage(message)
                writeChannel.flush()
            }
        } finally {
            sender.cancel()
            writeChannel.close()
        }
    }

    launch {
        try {
            while (!readChannel.isClosedForRead) {
                val message = readChannel.readMessage()
                receiver.send(message)
            }
        } finally {
            receiver.close()
            readChannel.cancel()
        }
    }

    return receiver to sender
}
