import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

fun main(args: Array<String>) {
    var host = "127.0.0.1"
    var port = 8888

    if (args.size != 2) {
        println("HOST and PORT are not provided, using default")
    } else {
        host = args[0]
        port = args[1].toInt()
    }
    println("Starting server on $host:$port")
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
    private val clients = mutableMapOf<String, Socket>()

    fun listenForConnections() {
        while (true) {
            try {
                val clientSocket = main.accept()
                val buffer = ByteArray(buffSize)
                clientSocket.inputStream.read(buffer)
                val message = UsernameMessage.fromBytes(buffer)
                println("RECIEVED: $message")
                if (message.username in clients) {
                    handleSameUsername(message.username, clientSocket)
                } else {
                    handleNewUsername(message.username, clientSocket)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleSameUsername(username: String, socket: Socket) {
        broadcast(ServerMessageType.SERVICE.encode() + TextOnlyMessage("Impostor just tried to connect under $username's name").toBytes())
        socket.getOutputStream().write(ServerMessageType.SERVICE.encode() + TextOnlyMessage("name $username is already in use, srry").toBytes())
        socket.close()
    }

    private fun handleNewUsername(username: String, socket: Socket){
        broadcast(ServerMessageType.SERVICE.encode() + TextOnlyMessage("$username just connected").toBytes())
        clients[username] = socket
        thread {
            handle(socket, username)
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
        clients.remove(name)
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
            if (it.value != except) it.value.getOutputStream().write(bytes)
        }
    }

}