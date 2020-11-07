import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.ws
import io.ktor.http.HttpMethod
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val client = HttpClient {
    install(WebSockets)
}

val outgoingMessageChannel = BroadcastChannel<Pair<String, String>>(Channel.BUFFERED)
val receiveOutgoingChannel = outgoingMessageChannel.openSubscription()

suspend fun listenChat(
    callback: (incomingMessage: MessageItem) -> Unit,
    receiveOutgoingChannel: ReceiveChannel<Pair<String, String>>
) {

    client.ws(
        method = HttpMethod.Get,
        port = 9090, path = "/chat"
    ) {
        coroutineScope {
            launch {
                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            try {
                                val messageItem = Json.decodeFromString<MessageItem>(text)
                                callback(messageItem)
                            } catch (e: Exception) {
                                println("json decoding ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("api socket incoming: ${e.message}")
                }
            }
            launch {
                try {
                    for (message in receiveOutgoingChannel) {
                        val msg =
                            Json.encodeToString(
                                MessageItem(
                                    message.first,
                                    Clock.System.now().toString(),
                                    message.second
                                )
                            )
                        send(Frame.Text(msg))
                    }
                } catch (e: Exception) {
                    println("api socket outgoing: ${e.message}")
                }
            }
        }
    }
}
