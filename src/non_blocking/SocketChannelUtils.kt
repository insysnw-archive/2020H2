package non_blocking

import common.Message
import java.io.*
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

fun SocketChannel.writeMessage(message: Message) {
    val bos = ByteArrayOutputStream()
    ObjectOutputStream(bos).apply {
        writeObject(message)
        flush()
    }
    write(ByteBuffer.wrap(bos.toByteArray()))
}

fun SocketChannel.readMessage(): Message {
    val bos = ByteArrayOutputStream()
    var result: Message

    while (true) {
        val buffer = ByteBuffer.allocate(256)
        read(buffer)
        bos.write(buffer.array())

        val bis = ByteArrayInputStream(bos.toByteArray())
        try {
            result = ObjectInputStream(bis).readObject() as Message
            break
        } catch (e: EOFException) {
        }
    }

    return result
}