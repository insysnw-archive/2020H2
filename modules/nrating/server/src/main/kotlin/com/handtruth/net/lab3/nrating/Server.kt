package com.handtruth.net.lab3.nrating

import com.handtruth.net.lab3.message.readMessage
import com.handtruth.net.lab3.message.writeMessage
import com.handtruth.net.lab3.nrating.messages.DisconnectMessage
import com.handtruth.net.lab3.nrating.messages.QueryMessage
import com.handtruth.net.lab3.nrating.types.QueryStatus
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import java.net.InetSocketAddress

fun main(args: Array<String>) {
    val parser = ArgParser("nrating-server")
    val port by parser.option(ArgType.Int, shortName = "p", description = "Server port").default(3433)
    val strictMode by parser.option(ArgType.Boolean, shortName = "s", description = "Strict Mode").default(false)
    parser.parse(args)

    runBlocking {
        val server = aSocket(ActorSelectorManager(Dispatchers.IO))
            .tcp()
            .bind(InetSocketAddress("127.0.0.1", port))
        println("Started nrating server at port $port ${if(strictMode) " in strict mode" else ""}")

        val serverState = ServerState()

        while (true) {
            val clientSocket = server.accept()

            launch {
                println("Client accepted: ${clientSocket.remoteAddress}")

                val input = clientSocket.openReadChannel()
                val output = clientSocket.openWriteChannel(autoFlush = true)

                try {
                    clientHandler(serverState, input, output, strictMode)
                } catch (e: Throwable) {
                    e.printStackTrace()
                    clientSocket.close()
                }
            }
        }
    }
}

suspend fun clientHandler(
    serverState: ServerState,
    input: ByteReadChannel,
    output: ByteWriteChannel,
    strictMode: Boolean
) {
    while (true) {
        when (val message = input.readMessage()) {
            is QueryMessage -> {
                val response = handleQueryMessage(serverState, message)
                output.writeMessage(response)
                if (strictMode && response.status == QueryStatus.FAILED) {
                    output.writeMessage(DisconnectMessage("You were disconnected for sending bad queries."))
                }
            }
            else -> println("Invalid message type!")
        }
    }
}
