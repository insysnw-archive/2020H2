package com.github.antoshka77.schat

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

private val selector = ActorSelectorManager(Dispatchers.IO + CoroutineName("selector"))

class ShutdownError : Error("shutdown")

suspend fun client(
    parcels: ReceiveChannel<Say>,
    messages: SendChannel<Message>,
    host: String,
    port: Int,
    nick: String? = null
) {
    aSocket(selector).tcp().connect(host, port).use { socket ->
        val input = socket.openReadChannel()
        val output = socket.openWriteChannel()
        output.send(Enter(nick ?: randomNick()))
        coroutineScope {
            val sender = launch(Dispatchers.IO) {
                for (parcel in parcels)
                    output.send(parcel)
            }
            val receiver = launch {
                forever {
                    messages.send(Message.read(input))
                }
            }
            sender.join()
            receiver.cancelAndJoin()
            messages.close()
        }
    }
}
