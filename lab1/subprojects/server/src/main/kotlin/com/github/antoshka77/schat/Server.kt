package com.github.antoshka77.schat

import ch.qos.logback.classic.Level
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.optional
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.datetime.Clock
import mu.KotlinLogging
import java.net.URI
import java.net.URISyntaxException
import kotlin.io.use
import kotlin.time.seconds

suspend fun main(args: Array<String>) {
    val argParser = ArgParser("schat-server")
    val logLevel by argParser.option(
        ArgType.Choice(
            listOf("ALL", "TRACE", "DEBUG", "INFO", "WARN", "ERROR", "NO"),
            { Level.toLevel(it.toUpperCase())!! }
        ), fullName = "log"
    ).default(Level.INFO)
    val address by argParser.argument(
        ArgType.String,
        fullName = "hostname[:port]",
        description = "server address"
    ).optional().default("0.0.0.0:$DEFAULT_PORT")
    argParser.parse(args)
    setLogLevel(logLevel as Level)
    try {
        val uri = URI("tcp://$address")
        server(uri.host, uri.port.let { if (it == -1) DEFAULT_PORT else it })
    } catch (e: URISyntaxException) {
        log.error { "address format error in --address option: ${e.message}" }
    } catch (e: Exception) {
        log.error(e) { "root error" }
    }
}

private val log = KotlinLogging.logger { }

private val selector = ActorSelectorManager(Dispatchers.IO + CoroutineName("selector"))

private val messages = MutableSharedFlow<Message>()

suspend fun server(address: String, port: Int): Nothing = supervisorScope {
    val socket = aSocket(selector).tcp().bind(address, port)
    launch(CoroutineName("messages")) {
        messages.collect { message ->
            log.info { "message: [${message.nick}] ${message.message}" }
        }
    }
    forever {
        val client = socket.accept()
        val clientAddress = client.remoteAddress
        log.info { "new client $clientAddress" }
        launch(Dispatchers.Default) {
            try {
                client.use { connection(it) }
            } catch (e: ClosedReceiveChannelException) {
                log.info { "client $clientAddress disconnected" }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.warn(e) { "client $clientAddress connection closed due error" }
            }
        }
    }
}

suspend fun connection(socket: Socket) = coroutineScope {
    val input = socket.openReadChannel()
    val output = socket.openWriteChannel()
    val enter = withTimeout(3.seconds) { Enter.read(input) }
    log.info { "client ${socket.remoteAddress} picks name \"${enter.nick}\"" }
    launch(CoroutineName("client/${enter.nick}/send")) {
        messages.collect { message ->
            output.send(message)
        }
    }
    launch(CoroutineName("client/${enter.nick}/receive")) {
        forever {
            val told = Say.read(input)
            val message = Message(Clock.System.now(), enter.nick, told.message)
            messages.emit(message)
        }
    }
}
