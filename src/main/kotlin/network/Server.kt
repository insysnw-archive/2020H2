package network

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket

class Server {
    private val serverScope = CoroutineScope(Dispatchers.IO)

    private val port = 8080
    private lateinit var serverSocket: ServerSocket
    private val clients = mutableListOf<Socket>()

    val messages: ObservableList<String> = FXCollections.observableArrayList()
    val clientsNames: ObservableList<String> = FXCollections.observableArrayList()

    init {
        initSocket()
    }

    private fun initSocket() {
        serverScope.launch(Dispatchers.IO) {
            serverSocket = ServerSocket(port)
            waitForClients()
        }
    }

    private fun sendBroadcastMessage(message: String) {
        clients.forEach { client ->
            sendMessage(client, message)
        }
    }

    private fun sendMessage(client: Socket, message: String) {
        try {
            val serverWriter = DataOutputStream(client.getOutputStream())
            serverWriter.writeUTF(message)
            serverWriter.flush()
        } catch (e: Exception) {
            println(e.toString())
        }
    }

    private fun waitForClients() {
        serverScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    val newClient = serverSocket.accept()
                    newClient?.let {
                        clients.add(it)
                        sendHistory(it)
                        startListenForMessages(it)
                    }
                } catch (e: Exception) {
                    println(e.toString())
                }
            }
        }
    }

    private fun sendHistory(client: Socket) {
        if (messages.isEmpty()) return
        sendMessage(client, messages.toList().joinToString("\n"))
    }

    private fun startListenForMessages(client: Socket) {
        serverScope.launch(Dispatchers.IO) {
            try {
                while (true) {
                    if (client.isClosed) {
                        clients.remove(client)
                        withContext(Dispatchers.JavaFx) {
                            clientsNames.removeIf { it.startsWith("${client.inetAddress}:${client.port}") }
                        }
                    }
                    val serverReader = DataInputStream(client.getInputStream())
                    val newMessage = serverReader.readUTF()
                    when {
                        newMessage.startsWith("nickname:") -> {
                            delay(100)
                            val nickname = newMessage.substringAfter(
                                "nickname:"
                            )
                            sendBroadcastMessage(
                                "New user: $nickname"
                            )
                            clientsNames.add("${client.inetAddress}:${client.port}:$nickname")
                        }
                        newMessage.startsWith("renameNickname:") -> {
                            renameUser(client, newMessage)
                        }
                        else -> {
                            messages.add(newMessage)
                            sendBroadcastMessage(newMessage)
                        }
                    }
                }
            } catch (e: Exception) {
                client.close()
                clients.remove(client)
                serverScope.launch(Dispatchers.JavaFx) {
                    clientsNames.removeIf { it.startsWith("${client.inetAddress}:${client.port}") }
                }
            }
        }
    }

    private fun renameUser(client: Socket, message: String) {
        val newNickname = message.substringAfter(
            "renameNickname:"
        )
        val oldNickname =
            clientsNames.find { it.startsWith("${client.inetAddress}:${client.port}") }?.split(":")
                ?.last() ?: ""
        sendBroadcastMessage(
            "User $oldNickname now is $newNickname"
        )
        clientsNames.removeIf { it.endsWith(oldNickname) }
        clientsNames.add("${client.inetAddress}:${client.port}:$newNickname")
    }

    fun onDestroy() {
        try {
            serverSocket.close()
            serverScope.cancel()
            clients.forEach { it.close() }
        } catch (e: Exception) {
        }
    }
}