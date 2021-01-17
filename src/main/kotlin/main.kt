import data.clientsStorage
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

var port = 9999
var host = "127.0.0.1"

private lateinit var server: ServerSocket
const val maxSize = 1024

fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        host = args[0]
        if (args.size == 2) {
            port = args[1].toInt()
        }
    }
    runServer()
}

fun runServer() {
    server = ServerSocket(port, 1024, InetAddress.getByName(host))
    println("Server Started on address: $host, port: $port")
    handleClient()
}

fun handleClient() {

    while (true) {
        val clientSocket = server.accept()
        try {
            println("new client accepted")
            thread {
                val buf = ByteArray(maxSize)
                clientSocket.inputStream.read(buf)
                login(clientSocket, buf)
            }
        } catch (e: java.net.SocketException) {
            clientSocket.close()
        }
    }
}

fun listenClient(clientSocket: Socket, username: String?) {
    try {
        while (true) {
            val buf = ByteArray(maxSize)
            clientSocket.getInputStream().read(buf)
            if (!buf.all { it == 0.toByte() }) {
                when (buf.first().toInt()) {
                    0 -> login(clientSocket, buf)
                    1 -> sendMail(buf, clientSocket, username ?: "")
                    2 -> readMails(clientSocket, username ?: "")
                    3 -> deleteMail(clientSocket, buf, username ?: "")
                    4 -> quit(clientSocket, username ?: "")
                }
            } else {
                println("$username exited")
                clientSocket.close()
                clientsStorage.remove(username)
            }
        }
    } catch (e: java.net.SocketException) {
        clientSocket.close()
        clientsStorage.remove(username)
    }
}

