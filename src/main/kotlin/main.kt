import kotlinx.serialization.json.Json
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

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
    receive()
}

fun receive() {
    while (true) {
        val clientSocket = server.accept()
        println("client accepted")
        val buf = ByteArray(maxSize)
        clientSocket.inputStream.read(buf)
        val typeReq = buf.first()
        println("smthrecived")
        if (typeReq == 0.toByte()) {
            login(buf, clientSocket)
        }
    }
}

fun handle(clientSocket: Socket, username: String) {
    try {
        while (true) {
            val buf = ByteArray(maxSize)
            clientSocket.getInputStream().read(buf)
            if (!buf.all { it == 0.toByte() }) {
                val command = buf.first().toInt()
                println("new command $command")
                when (command) {
                    1 -> sendMail(buf, clientSocket, username)
                    2 -> readMails(clientSocket, username)
                    3 -> deleteMail(buf, username)
                    4 -> quit(clientSocket, username)
                }
            } else {
                println("$username exited")
                clientSocket.close()
            }
        }
    } catch (e: java.net.SocketException) {
        clientSocket.close()
    }
}

