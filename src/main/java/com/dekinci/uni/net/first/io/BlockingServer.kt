package com.dekinci.uni.net.first.io

import com.dekinci.uni.net.first.*
import java.io.EOFException
import java.net.*
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class BlockingServer(address: InetSocketAddress) {
    private val clients = ConcurrentHashMap<String, IoFacade>()
    private val server = ServerSocket()
    private val massMailer = Executors.newSingleThreadExecutor()
    private val pinguin = Executors.newSingleThreadScheduledExecutor()

    init {
        server.receiveBufferSize = BlockingReceiver.bufferSize
        server.reuseAddress = true
        server.bind(address)
        println("Running server on $address")

        pinguin.scheduleAtFixedRate({
            clients.filter {
                try {
                    it.value.ping()
                    true
                } catch (e: Exception) {
                    handleCommunicationException(e, it.key, it.key)
                    false
                }
            }
        }, 0, 500, TimeUnit.MILLISECONDS)
    }

    fun handleConnection() {
        val socket = server.accept()
        thread(isDaemon = true) {
            socket.sendBufferSize = BlockingReceiver.bufferSize
            val connection = IoFacade(socket.getInputStream(), socket.getOutputStream())
            val name = decode<Handshake>(connection.waitForMessage()).name
            val logName = "${socket.inetAddress.hostAddress}:${socket.port} aka $name"

            if (!handleHandshake(connection, name, logName)) {
                socket.close()
                return@thread
            }

            try {
                while (true) {
                    handleMessage(connection, name, logName)
                }
            } catch (e: Exception) {
                handleCommunicationException(e, name, logName)
            }
        }
    }

    private fun handleHandshake(connection: IoFacade, name: String, logName: String): Boolean {
        if (clients.putIfAbsent(name, connection) != null) {
            println("$logName kicked for duplicating name")
            connection.writeMessage(encode(Kick(Instant.now(), "Get lost, U R not special")))
            return false
        }
        connection.writeMessage(encode(Announcement(Instant.now(), "Welcome! Online: ${clients.keys}")))

        println("$logName connected")

        massMailer.submit {
            val encodedMessage = encode(Announcement(Instant.now(), "$name connected"))
            clients.asSequence()
                    .filter { it.key != name }
                    .forEach { it.value.writeSilently(encodedMessage, it.key) }
        }
        return true
    }

    private fun handleMessage(connection: IoFacade, name: String, logName: String) {
        val receivedMessage = decode<Message>(connection.waitForMessage())
        println("$logName incoming message of length ${receivedMessage.text.length}")

        massMailer.submit {
            val encodedMessage = encode(MessageUpdate(name, Instant.now(), receivedMessage.text.trim()))
            clients.asSequence()
                    .filter { it.key != name }
                    .forEach { it.value.writeSilently(encodedMessage, it.key) }
        }
    }

    private fun handleCommunicationException(e: Exception, name: String, logName: String) {
        when {
            e is SocketException && e.message in okExceptionMessages -> Unit
            e is EOFException -> Unit
            else -> e.printStackTrace()
        }

        handleDisconnect(name, logName)
    }

    private fun handleDisconnect(name: String, logName: String) {
        clients.remove(name)
        println("$logName disconnected")
        massMailer.submit {
            val encodedMessage = encode(Announcement(Instant.now(), "$name disconnected"))
            clients.forEach { it.value.writeSilently(encodedMessage, it.key) }
        }
    }

    companion object {
        private val okExceptionMessages = listOf(
                "Connection reset",
                "An established connection was aborted by the software in your host machine"
        )
    }
}