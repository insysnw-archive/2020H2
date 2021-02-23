package common

object Strings {
    const val SERVER_STARTED = "Server started"
    const val SERVER_NOT_STARTED = "Server not started"
    const val SOCKET_NOT_CREATED = "Can't create socket"
    const val ENTER_USERNAME = "Enter username: "
    const val BAD_USERNAME = "Name has unsupported symbols: "
    const val TAKEN_USERNAME = "This name is reserved: "
    const val STATUS_OK = "Ok"
    const val STATUS_EXCEPTION = "Connection declined"
    private val welcome_msg = listOf<String>("Hello there, general","Welcome,","An easy landing,", "Execute order 66,",
    "No day can be called real good without", "Nice pics,", "Good day to you,","Party can be started with")
    val HELLO = { username: String? -> welcome_msg.random() + " $username!" }
}