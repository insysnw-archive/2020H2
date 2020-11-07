import kotlinx.serialization.Serializable

@Serializable
data class MessageItem(val userName: String, val time: String, val content: String)