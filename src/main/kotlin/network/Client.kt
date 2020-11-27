package network

import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import kotlinx.coroutines.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.*

class Client(var nickname: String) {
    private val clientScope = CoroutineScope(Dispatchers.IO)

    val chatStrings: ObservableList<String> = FXCollections.observableArrayList()
    val newMessage = SimpleStringProperty()

    private val address = "localhost"
    private val port = 8080
    private lateinit var clientSocket: Socket

    private lateinit var serverReader: DataInputStream
    private lateinit var serverWriter: DataOutputStream

    init {
        initSocket()
    }

    private fun initSocket() {
        clientScope.launch(Dispatchers.IO) {
            try {
                clientSocket = Socket(address, port)
                serverReader = DataInputStream(clientSocket.getInputStream())
                serverWriter = DataOutputStream(clientSocket.getOutputStream())
                sendMessage("nickname:$nickname")
                listenForNewMessagesFromServer()
            } catch (e: Exception) {
                println(e.toString())
                delay(500)
                initSocket()
            }
        }
    }

    private fun listenForNewMessagesFromServer() {
        clientScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    val newMessage = serverReader.readUTF()
                    chatStrings.add(newMessage)
                } catch (e: Exception) {
                    chatStrings.add("Server is closed")
                }
            }
        }
    }

    fun sendMessage(message: String = "") {
        try {
            val messageForSent = if (message.isEmpty()) getFormattedMessage(newMessage.value) else message
            serverWriter.writeUTF(messageForSent)
            serverWriter.flush()
        } catch (e: Exception) {
        }
    }

    private fun getFormattedMessage(message: String): String {
        val currentDate = getCurrentTime()
        return "($currentDate) $nickname: $message"
    }

    private fun getCurrentTime() = SimpleDateFormat("HH:mm", Locale("ru")).format(Date())

    fun onDestroy() {
        try {
            if (::clientSocket.isInitialized) {
                serverWriter.close()
                serverReader.close()
                clientSocket.close()
            }
            clientScope.cancel()
        } catch (e: Exception) {
        }
    }
}