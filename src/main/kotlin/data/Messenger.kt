package data

import models.Header
import models.Message
import models.ResponseHeader
import models.ResponseMessage
import org.mockito.Mock
import org.mockito.Mockito
import writeLog
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread

class Messenger(ip: String, port: Int) {

    val clientSocket: Socket = Socket(ip, port)

    private val inputStream: InputStream
    private val outputStream: OutputStream

    init {
        println("Connected")
        inputStream = clientSocket.getInputStream()
        outputStream = clientSocket.getOutputStream()
    }

    fun readMessage(): ResponseMessage {
        return try {
            val readBytes = mutableListOf<Byte>()
            val headerSize = 4

            var buffer = ByteArray(headerSize)
            inputStream.read(buffer)
            readBytes += buffer.toList()

            writeLog("HeaderBytes: $readBytes")

            val code = ByteBuffer.wrap(readBytes.subList(0, 2).toByteArray()).order(ByteOrder.LITTLE_ENDIAN).short
                .toInt()
            val bodySize =
                ByteBuffer.wrap(readBytes.subList(2, 4).toByteArray()).order(ByteOrder.LITTLE_ENDIAN).short
                    .toInt()
            if (bodySize == 0) return ResponseMessage(ResponseHeader(code))

            buffer = ByteArray(bodySize)
            inputStream.read(buffer)
            readBytes += buffer.toList()

            writeLog("ByteArray: $readBytes")
            ResponseMessage(ResponseHeader(code), String(buffer))
        } catch (e: Exception) {
            println("Read message error: $e")
            throw e
        }
    }

    fun sendMessage(message: Message) {
        try {
            val writer = DataOutputStream(outputStream)
            val formattedMessage = message.toByteArray()
            writer.write(formattedMessage)
            writer.flush()
            writeLog("Message sent: ${message.toStringForLogging()}\n")
        } catch (e: Exception) {
            println("Send message error: $e")
        }
    }

    private fun Message.toByteArray(): ByteArray {
        val bitsToHeaderDictionary = mapOf(
            Header("GET", "/events") to 0,
            Header("POST", "/add-event") to 1,
            Header("POST", "/delete-event") to 2,
            Header("POST", "/subscribe") to 3,
            Header("POST", "/unsubscribe") to 4,
            Header("GET", "/notification") to 5,
            Header("POST", "/register") to 6
        )
        val headerByte = bitsToHeaderDictionary[this.header.copy(token = ByteArray(32))]!!.toByte()
        val token = this.header.token
        val body = this.body.encodeToByteArray()
        val bodySize = body.size.toShort().toByteBuffer(2)
        writeLog(
            "Header type: " + byteArrayOf(headerByte).toList() + " " + "Token: " + token.toList() + " " + "BodySize: " + bodySize.array()
                .toList() + " " + "Body: " + body.toList() + " "
        )
        return byteArrayOf(headerByte) + token + bodySize.array() + body
    }

    fun onDestroy() {
        inputStream.close()
        outputStream.close()
        clientSocket.close()
    }
}

fun Short.toByteBuffer(capacity: Int): ByteBuffer =
    ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN).putShort(this)