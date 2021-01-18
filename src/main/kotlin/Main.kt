import data.participantStorage
import data.stewardStorage
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
    handleParticipant()
}

fun handleParticipant() {
    while (true) {
        val clientSocket = server.accept()
        try {
            println("new client accepted")
            thread {
                val buf = ByteArray(maxSize)
                clientSocket.inputStream.read(buf)
                auth(buf, clientSocket)
            }
        } catch (e: java.net.SocketException) {
            clientSocket.close()
        }
    }
}

fun listenClient(clientSocket: Socket, userName: String?) {
    try {
        while (true) {
            val buf = ByteArray(maxSize)
            clientSocket.getInputStream().read(buf)
            if (!buf.all { it == 0.toByte() }) {
                val command = buf.first().toInt()
                println("new command $command")
                when (command) {
                    0 -> auth(buf, clientSocket)
                    1 -> changePrice(clientSocket, buf, userName!!)
                    2 -> readItems(clientSocket)
                    3 -> addItem(buf, clientSocket)
                    4 -> stopAuction(clientSocket)
                    5 -> quit(clientSocket, userName!!)
                }
            } else {
                println("$userName exited")
                clientSocket.close()
                participantStorage.remove(userName)
                stewardStorage.remove(userName)
            }
        }
    } catch (e: java.net.SocketException) {
        clientSocket.close()
        participantStorage.remove(userName)
        stewardStorage.remove(userName)
    }
}

