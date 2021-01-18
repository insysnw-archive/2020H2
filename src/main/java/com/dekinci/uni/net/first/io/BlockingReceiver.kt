package com.dekinci.uni.net.first.io

import com.dekinci.uni.net.first.MessageEncoder
import com.dekinci.uni.net.first.decodeInt
import java.io.InputStream
import java.lang.Integer.max
import java.lang.Integer.min
import java.util.concurrent.LinkedBlockingQueue


open class BlockingReceiver(private val inputStream: InputStream, private val writer: DelegateWriter) {
    private val messageQueue = LinkedBlockingQueue<Map<String, String>>()

    fun findMessage(): Map<String, String>? {
        return messageQueue.poll()
    }

    fun blockingUpdate() {
        when (inputStream.read()) {
            MessageEncoder.ackType -> writer.receiveAck()
            MessageEncoder.messageType -> messageQueue.add(readMessage())
        }
    }

    private fun readMessage(): Map<String, String> {
        val length = decodeInt(inputStream.read(), inputStream.read(), inputStream.read(), inputStream.read())
        val result = HashMap<String, String>()

        for (i in 0 until length) {
            result[readPayload()] = readPayload()
        }
        return result
    }

    private fun readPayload(): String {
        var toRead = decodeInt(inputStream.read(), inputStream.read(), inputStream.read(), inputStream.read())
        val buf = ByteArray(toRead)

        if (toRead < MessageEncoder.chunkSize) {
            inputStream.read(buf)
        } else {
            writer.sendAck()
            while (toRead > 0) {
                inputStream.read(buf, buf.size - toRead, min(MessageEncoder.chunkSize, toRead))
                toRead = max(toRead - MessageEncoder.chunkSize, 0)
                writer.sendAck()
            }
        }
        return buf.toString(Charsets.UTF_8)
    }

    companion object {
        const val bufferSize = 500
    }
}
