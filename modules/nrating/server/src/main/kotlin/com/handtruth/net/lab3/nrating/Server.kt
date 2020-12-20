package com.handtruth.net.lab3.nrating

import com.handtruth.net.lab3.message.readMessage
import com.handtruth.net.lab3.message.writeMessage
import com.handtruth.net.lab3.nrating.messages.QueryMessage
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress

fun main() {
    runBlocking {
        val server = aSocket(ActorSelectorManager(Dispatchers.IO))
            .tcp()
            .bind(InetSocketAddress("127.0.0.1", 3433))
        println("Started nrating server at port 3433")

        val serverState = ServerState()

        while (true) {
            val clientSocket = server.accept()

            launch {
                println("Client accepted: ${clientSocket.remoteAddress}")

                val input = clientSocket.openReadChannel()
                val output = clientSocket.openWriteChannel(autoFlush = true)

                try {
                    clientHandler(serverState, input, output)
                } catch (e: Throwable) {
                    e.printStackTrace()
                    clientSocket.close()
                }
            }
        }
    }
}

suspend fun clientHandler(serverState: ServerState, input: ByteReadChannel, output: ByteWriteChannel) {
    while (true) {
        when (val message = input.readMessage()) {
            is QueryMessage -> output.writeMessage(handleQueryMessage(serverState, message))
            else -> println("Invalid message type!")
        }
    }
}
