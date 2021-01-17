import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

fun main(args: Array<String>) {
    var host = "127.0.0.1"
    var port = 8888

    if (args.size != 3) {
        println("HOST and PORT are not provided, using default")
    } else {
        host = args[0]
        port = args[1].toInt()
    }

    Server(host, port).listenForConnections()

}

class Server(
        host: String,
        port: Int,
) {
    companion object {
        private const val maxConnections = 1024
        private const val buffSize = 1024
    }

    private val main = ServerSocket(port, maxConnections, InetAddress.getByName(host))
    private val clients = mutableListOf<Socket>()

    fun listenForConnections() {
        while (true) {
            try {
                val clientSocket = main.accept()
                val buffer = ByteArray(buffSize)
                clientSocket.inputStream.read(buffer)
                val message = UsernameMessage.fromBytes(buffer)
                println("RECIEVED: $message")
                broadcast(ServerMessageType.SERVICE.encode() + TextOnlyMessage("${message.text} just connected").toBytes())
                clients.add(clientSocket)
                thread {
                    handle(clientSocket, message.text)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handle(client: Socket, name: String) {
        try {
            while (true) {
                val buffer = ByteArray(buffSize)
                client.getInputStream().read(buffer)
                if (buffer.nonEmptyBytes()) {
                    val clientMessage = ClientMessage.fromBytes(buffer)
                    println("RECIEVED: $clientMessage")
                    broadcastChat(
                            message = ChatMessage(clientMessage.time, name, clientMessage.text),
                            except = client
                    )
                } else {
                    removeClient(client, name)
                }
            }
        } catch (e: Exception) {
            removeClient(client, name)
        }
    }

    private fun removeClient(client: Socket, name: String) {
        client.close()
        clients.remove(client)
        broadcastService(TextOnlyMessage("user $name disconnected"))
    }

    private fun broadcastChat(message: ChatMessage, except: Socket) {
        broadcast(ServerMessageType.CHAT.encode() + message.toBytes(), except = except)
    }

    private fun broadcastService(message: TextOnlyMessage) {
        broadcast(ServerMessageType.SERVICE.encode() + message.toBytes())
    }

    private fun broadcast(bytes: ByteArray, except: Socket? = null) {
        clients.forEach {
            if (it != except) it.getOutputStream().write(bytes)
        }
    }

}