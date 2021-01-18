data class Message(
    val messageType: MessageType,
    val token: ByteArray,
    val message: String
)

enum class MessageType(val value: Int) {
    REGISTRATION(0),
    MESSAGE(1),
    DISCONNECT(2),
    UNKNOWN(-1)
}