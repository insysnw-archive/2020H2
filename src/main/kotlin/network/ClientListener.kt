package network

import java.net.Socket

class ClientListener(private val clientSocket: Socket, private val routing: Routing, val clientNumber: Int) : Thread() {

    private val messenger = Messenger(clientSocket, clientNumber)
    var isRunning = true

    init {
        routing.call = messenger
        start()
    }

    override fun run() {
        try {
            while (isRunning) {
                val message = messenger.waitForMessage()
                println("[$clientNumber] Message received: ${message.toStringForLogging()}")
                routing.execute(message.header)
            }
        } catch (e: Exception) {
            isRunning = false
            interrupt()
            println("[$clientNumber] Client listener error: $e")
        }
    }

    fun stopListening() {
        isRunning = false
        clientSocket.close()
    }
}

