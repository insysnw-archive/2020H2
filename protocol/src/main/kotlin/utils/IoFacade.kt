package utils

import java.io.InputStream
import java.io.OutputStream
import java.net.SocketException

class IoFacade(inputStream: InputStream, outputStream: OutputStream) {
    private val writer: DelegateWriter = DelegateWriter { bytes ->
        outputStream.write(bytes)
        outputStream.flush()
    }
    private val receiver: BlockingReceiver = BlockingReceiver(inputStream, writer)

    fun waitForMessage(): Map<String, String> {
        var message = receiver.findMessage()
        while (message == null) {
            receiver.blockingUpdate()
            message = receiver.findMessage()
        }
        return message
    }

    fun ping() {
        writer.sendPing()
    }

    fun writeSilently(map: Map<String, String>, note: String) = try {
        writeMessage(map)
        true
    } catch (e: SocketException) {
        System.err.println("Error writing $note")
        e.printStackTrace()
        false
    }

    fun writeMessage(map: Map<String, String>) {
        val encoded = MessageEncoder.encode(map)
        writer.writeMessage(encoded)
    }
}