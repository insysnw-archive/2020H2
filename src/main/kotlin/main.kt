import data.clientsStorage
import model.ERROR_CODE
import model.loginFirstMsg
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
    handleClient()
}

fun handleClient() {
    while (true) {
        val clientSocket = server.accept()
        println("new client accepted")
        val buf = ByteArray(maxSize)
        clientSocket.inputStream.read(buf)
        val typeReq = buf.first()
        if (typeReq == 0.toByte()) {
            login(buf, clientSocket)
        } else {
            clientSocket.getOutputStream().write(ERROR_CODE.toServerResponse(loginFirstMsg()))
        }
    }
}

fun listenClient(clientSocket: Socket, username: String) {
    try {
        while (true) {
            val buf = ByteArray(maxSize)
            clientSocket.getInputStream().read(buf)
            if (!buf.all { it == 0.toByte() }) {
                val option = buf.first().toInt()
                // println("new option $option")
                when (option) {
                    1 -> sendMail(buf, clientSocket, username)
                    2 -> readMails(clientSocket, username)
                    3 -> deleteMail(clientSocket, buf, username)
                    4 -> quit(clientSocket, username)
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

