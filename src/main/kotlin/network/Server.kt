package network

import java.net.ServerSocket
import kotlin.concurrent.thread

class Server(private val routing: () -> Routing) {
    private val port = 1490
    private val serverSocket: ServerSocket
    private val clients = mutableListOf<ClientListener>()
    var isStop = false

    init {
        serverSocket = ServerSocket(port)
        println("Server started\n")
        waitForClients()
    }

    private fun waitForClients() {
        thread {
            while (!isStop) {
                try {
                    val newClient = serverSocket.accept()
                    newClient?.let {
                        clients.add(ClientListener(it, routing.invoke(), clients.size + 1))
                        println("New client: [${clients.size}] $it\n")
                    }
                } catch (e: Exception) {
                    println(e.toString())
                }
            }
        }
    }

    fun stop() {
        isStop = true
        clients.forEach { it.stopListening() }
    }

    fun start() {
        isStop = false
        waitForClients()
    }

}