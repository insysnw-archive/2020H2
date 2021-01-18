package network

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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
                        clientsNames.removeIfPossible(client)
                    }
                    val serverReader = DataInputStream(client.getInputStream())
                    val newMessage = serverReader.readUTF()
                    val parsedMessage = formatMessage(newMessage)
                    println("received message: $parsedMessage")
                    parsedMessage.header.serviceInformation.forEach { entry ->
                        getActionForParam(client, entry).invoke()
                    }
                    if (parsedMessage.header.time.isNotEmpty()) {
                        getNickname(client)?.let {
                            parsedMessage.header.nickname = it
                            messages.add(parsedMessage.getFormattedMessage())
                            sendBroadcastMessage(parsedMessage.getFormattedMessage())
                        } ?: sendMessage(client, "Auth error")
                    }
                }
            } catch (e: Exception) {
                client.close()
                clients.remove(client)
                clientsNames.removeIfPossible(client)
            }
        }
    }

    private fun getNickname(client: Socket): String? =
            clientsNames.find {it.startsWith("${client.inetAddress}:${client.port}")}?.split(":")?.last()


    private fun getActionForParam(client: Socket, entry: Map.Entry<String, String>): () -> Unit {
        return when (entry.key) {
            "nickname" -> {
                {
                    val nickname = entry.value
                    val equalsNickname = clientsNames.find { it.endsWith(nickname) }
                    val newNickname = if (equalsNickname == null) nickname
                    else findNewNickname(nickname)
                    sendBroadcastMessage(
                            "New user: $newNickname"
                    )
                    clientsNames.add("${client.inetAddress}:${client.port}:$newNickname")
                }
            }
            "renameNickname" -> {
                { renameUser(client, entry.value) }
            }
            else -> {
                {}
            }
        }
    }

    private fun formatMessage(rawString: String): Message {
        return try {
            val (serviceInfo, time, name, body) = rawString.split(" ", limit = 4)
            val serviceParams = serviceInfo.split(",")
            val parsedParams = if (serviceParams.isBlank()) emptyMap()
            else serviceParams.associate {
                val (first, second) = it.split(":")
                first to second
            }
            Message(Header(parsedParams, time, name), body)
        } catch (e: Exception) {
            println(e)
            Message.emptyMessage()
        }
    }

    private fun findNewNickname(nickname: String): String {
        var equalsNickname: String? = null
        var counter = 1
        while (equalsNickname != null) {
            equalsNickname = clientsNames.find { it.endsWith("$nickname$counter") }
            counter++
        }
        return "$nickname$counter"
    }

    private fun renameUser(client: Socket, nickname: String) {
        val equalsNickname = clientsNames.find { it.endsWith(nickname) }
        if (equalsNickname == null) {
            val oldNickname =
                    clientsNames.find { it.startsWith("${client.inetAddress}:${client.port}") }?.split(":")
                            ?.last() ?: ""
            messages.add("User $oldNickname now is $nickname")
            sendBroadcastMessage(
                    "User $oldNickname now is $nickname"
            )
            clientsNames.removeIf { it.endsWith(oldNickname) }
            clientsNames.add("${client.inetAddress}:${client.port}:$nickname")
        } else sendMessage(client, "This nickname is already in use")
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

fun ObservableList<String>.removeIfPossible(client: Socket) {
    this.removeIf { it.startsWith("${client.inetAddress}:${client.port}") }
}

fun List<String>.isBlank(): Boolean {
    this.forEach { elem ->
        if (elem.isBlank()) return true
    }
    return false
}