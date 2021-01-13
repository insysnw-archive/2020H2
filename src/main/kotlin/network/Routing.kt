package network

import models.Header
import models.Message

fun routing(configuration: Routing.() -> Unit): () -> Routing {
    return { Routing().apply(configuration) }
}

class Routing() {

    lateinit var call: Messenger

    private val callsPipeline = mutableMapOf<Header, Messenger.() -> Unit>(Header.notFound() to {
        respondError(
            Message(
                Header.notFound()
            )
        )
    })

    fun get(path: String, onAction: Messenger.() -> Unit) {
        httpCall("GET", path, onAction)
    }

    fun post(path: String, onAction: Messenger.() -> Unit) {
        httpCall("POST", path, onAction)
    }

    private fun httpCall(method: String, uri: String, onAction: Messenger.() -> Unit) {
        callsPipeline[Header(method, uri)] = onAction
    }

    fun execute(header: Header) {
        val foundCall = findCall(header)
        foundCall.invoke(call)
    }

    private fun findCall(header: Header): Messenger.() -> Unit {
        val foundFunction = callsPipeline[header]
        return foundFunction ?: {
            sendMessage(Message(Header.notFound()))
        }
    }

}
