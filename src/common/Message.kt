package common

import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

sealed class Message : Serializable {
    abstract var username: String
}

class ConnectionRequest(override var username: String) : Message()
class ConnectionResponse(override var username: String, var date: Date? = null) : Message() {
    override fun toString() = "${formatDate(date)} $username присоединился к чату"
}

class ConnectionDenied(override var username: String) : Message()

class DisconnectionRequest(override var username: String) : Message()
class DisconnectionResponse(override var username: String, var date: Date? = null) : Message() {
    override fun toString() = "${formatDate(date)} $username вышел из чата"
}

class UserMessage(override var username: String, var message: String, var date: Date? = null) : Message() {
    override fun toString() = "${formatDate(date)} [$username] $message"
}

fun formatDate(date: Date?) = "<${SimpleDateFormat("HH:mm:ss").format(date ?: Date())}>"
