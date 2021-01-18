import java.net.Socket
import java.net.SocketException
import java.time.Instant
import kotlin.concurrent.thread
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    var host = "127.0.0.1"
    var port = 8888
    var userName = "man_with_no_name"

    if (args.size != 3) {
        println("HOST, PORT and USERNAME are not provided, using default")
    } else {
        host = args[0]
        port = args[1].toInt()
        userName = args[2]
    }
    println("Starting client... $host:$port username: $userName")
    with(Client(host, port, userName)) {
        thread { receive() }
        thread { type() }
    }

}

class Client(
        host: String,
        port: Int,
        userName: String
) {
    companion object {
        private const val buffSize = 1024
    }

    private val socket: Socket

    init {
        try {
            socket = Socket(host, port)
            socket.getOutputStream().write(UsernameMessage(userName).toBytes())
        } catch (e: Exception) {
            println("Can't connect. Shutting down")
            exitProcess(0)
        }
    }

    fun receive() {
        try {
            while (true) {
                val buffer = ByteArray(buffSize)
                socket.getInputStream().read(buffer)
                if (buffer.nonEmptyBytes()) {
                    when (ServerMessageType.fromByte(buffer.first())) {
                        ServerMessageType.CHAT ->
                            printChatMessage(ChatMessage.fromBytes(buffer.sliceArray(1 until buffer.size)))
                        ServerMessageType.SERVICE ->
                            printServerMessage(TextOnlyMessage.fromBytes(buffer.sliceArray(1 until buffer.size)))
                    }
                } else {
                    println("Shutdown")
                    exitProcess(0)
                }
            }
        } catch (e: SocketException) {
            println("Shutdown")
            exitProcess(0)
        }
    }

    fun type() {
        while (true) {
            val text = readLine()
            if (!text.isNullOrBlank()) {
                sendMessage(text)
            }
        }
    }

    private fun sendMessage(text: String) {
        socket.getOutputStream().write(ClientMessage(Instant.now(), text).toBytes())
    }

    private fun printServerMessage(message: TextOnlyMessage) {
        println("FROM SERVER : ${message.username}")
    }

    private fun printChatMessage(message: ChatMessage) {
        println("<${formatter.format(message.time)}> [${message.name}] ${message.text}")
    }
}