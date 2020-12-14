package chat

import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

var port = 9999
var host = "127.0.0.1"

if (args.isNotEmpty()) {
    host = args[0]
    if (args.size == 2) {
        port = args[1].toInt()
    }
}

val startMsgIndex = 6
val maxSize = 1024
val server = ServerSocket(port, 1024, InetAddress.getByName(host))

var sockets = mutableListOf<Socket>()
var names = mutableListOf<String>()
println("Server Started on address: $host, port: $port")
receive()


fun broadcast(msg: ByteArray, fromClient: Socket?, name: String) {
    val resMsg =
        msg.filter { it != 0.toByte() }
            .toByteArray() + byteArrayOf(name.length.toByte()) + name.toByteArray()
    sockets.forEach { client ->
        if (client != fromClient) {
            client.outputStream.write(resMsg)
        }
    }
}

fun handle(clientSocket: Socket, username: String) {
    try {
        while (true) {
            val buf = ByteArray(maxSize)
            clientSocket.getInputStream().read(buf)
            broadcast(buf, clientSocket, username)
        }
    } catch (e: java.net.SocketException) {
        println("$username exited")
        clientSocket.close()
        names.remove(username)
        sockets.remove(clientSocket)
    }
}

fun receive() {
    while (true) {
        val clientSocket = server.accept()
        val buf = ByteArray(maxSize)
        clientSocket.inputStream.read(buf)
        val lengthMsg = buf.first()
        val username = String(buf.slice(startMsgIndex until startMsgIndex + lengthMsg).toByteArray())
        sockets.add(clientSocket)
        names.add(username)
        println("Client connected with name is $username")
        thread {
            handle(clientSocket, username)
        }
    }
}
