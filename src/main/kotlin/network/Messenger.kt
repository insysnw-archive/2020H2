package network

import models.Header
import models.Message
import java.io.DataOutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder


class Messenger(clientSocket: Socket, val clientNumber: Int) {

    lateinit var currentMessage: Message

    private val inputStream = clientSocket.getInputStream()
    private val outputStream = clientSocket.getOutputStream()

    fun waitForMessage(): Message {
        return try {
            val receivedMessage = readMessage()
            val message = formatMessage(receivedMessage)
            currentMessage = message
            message
        } catch (e: Exception) {
            throw e
        }
    }

    private fun readMessage(): List<Byte> {
        return try {
            val result = mutableListOf<Byte>()
            val headerSize = 3

            var buffer = ByteArray(headerSize)
            inputStream.read(buffer)
            result += buffer.toList()

            println("HeaderBytes: " + result)

            val bodySize =
                ByteBuffer.wrap(result.subList(1, 3).toByteArray()).order(ByteOrder.LITTLE_ENDIAN).short
                    .toInt()
            if (bodySize == 0) return result

            buffer = ByteArray(bodySize)
            inputStream.read(buffer)
            result += buffer.toList()

            println("ByteArray: " + result)
            result
        } catch (e: Exception) {
            println("[$clientNumber] Read message error: $e")
            throw e
        }
    }

    private fun formatMessage(packet: List<Byte>): Message {
        return try {
            val bitsToHeaderDictionary = mapOf(
                0 to Header("GET", "/current-section"),
                1 to Header("GET", "/current-articles"),
                2 to Header("POST", "/open-section"),
                3 to Header("GET", "/previous-section"),
                4 to Header("POST", "/get-article-by-name"),
                5 to Header("POST", "/get-articles-by-author"),
                6 to Header("POST", "/add-article")
            )
            val methodNumber = packet[0].toInt()
            val header = bitsToHeaderDictionary[methodNumber] ?: Header.notFound()
            val body = if (packet.size == 3) "" else String(packet.subList(3, packet.size).toByteArray())
            Message(header, body)
        } catch (e: Exception) {
            Message(Header.notFound())
        }
    }

    internal fun sendMessage(message: Message) {
        try {
            val writer = DataOutputStream(outputStream)
            val code = message.header.method.toShort().toByteBuffer(2)
            val body = message.body.encodeToByteArray()
            val bodySize = body.size.toShort().toByteBuffer(2)
            writer.write(code.array() + bodySize.array() + body)
            writer.flush()
            println("[$clientNumber] Message sent: ${message.toStringForLogging()}\n")
            println(
                "[$clientNumber] Message sent (bytes): ${
                    code.array().toList() + bodySize.array().toList() + body.toList()
                }\n"
            )
        } catch (e: Exception) {
            println("[$clientNumber] Send message error: $e")
        }
    }
}

class WrongPacketFormatException(text: String) : Exception()

fun Short.toByteBuffer(capacity: Int): ByteBuffer =
    ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN).putShort(this)