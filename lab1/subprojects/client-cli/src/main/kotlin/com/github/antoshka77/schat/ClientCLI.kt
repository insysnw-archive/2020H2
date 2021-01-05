package com.github.antoshka77.schat

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.optional
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.net.URI
import java.net.URISyntaxException
import kotlin.system.exitProcess

suspend fun main(args: Array<String>) {
    val argParser = ArgParser("schat-server")
    val address by argParser.argument(
        ArgType.String,
        fullName = "[username@]hostname[:port]",
        description = "server address and username"
    ).optional().default("localhost:$DEFAULT_PORT")
    argParser.parse(args)
    try {
        coroutineScope {
            val uri = URI("tcp://$address")
            val messages = Channel<Message>()
            val parcels = Channel<Say>()
            launch { stdinReceiver(parcels) }
            launch { stdoutTeller(messages) }
            client(
                parcels,
                messages,
                uri.host ?: error("host must be specified"),
                uri.port.let { if (it == -1) DEFAULT_PORT else it },
                uri.userInfo
            )
        }
    } catch (e: URISyntaxException) {
        System.err.println("address format error in --address option: ${e.message}")
    } catch (e: Exception) {
        e.printStackTrace(System.err)
    } catch (e: ShutdownError) {
        exitProcess(0)
    }
}

suspend fun stdinReceiver(channel: SendChannel<Say>) {
    withContext(Dispatchers.IO) {
        forever {
            val line = readLine() ?: run {
                channel.close()
                return@withContext
            }
            channel.send(Say(line))
        }
    }
}

suspend fun stdoutTeller(channel: ReceiveChannel<Message>) {
    for (message in channel) {
        val time = message.time.toLocalDateTime(TimeZone.currentSystemDefault())
        println("<${time.hour}:${time.minute}> [${message.nick}] ${message.message}")
    }
}
