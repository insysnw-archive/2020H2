import kotlinx.serialization.Serializable

@Serializable
class ServerMessage(
    val message: String
)

fun welcomeMsg() = ServerMessage("Welcome to Auction")

fun getQuitMsg(email: String) = ServerMessage("Goodbye, $email")
