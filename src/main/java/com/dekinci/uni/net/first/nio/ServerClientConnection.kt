package com.dekinci.uni.net.first.nio

import com.dekinci.uni.net.first.*
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class ServerClientConnection(val writer: DumpingWriter, val receiver: AccumulatingReceiver) {
    enum class Status {
        AWAIT_HANDSHAKE,
        ONLINE,
        DEAD
    }

    private var status = Status.AWAIT_HANDSHAKE
    private var logName: String? = null
    private var name: String? = null

    fun readInput(channel: SocketChannel) {
        receiver.dumpRemaining(inBuffer)
        channel.read(inBuffer)
        inBuffer.flip()
        receiver.update(inBuffer, writer)
    }

    fun handleRead(channel: SocketChannel, clients: ConcurrentHashMap<String, ServerClientConnection>) {
        var mapMessage = receiver.findMessage()

        while (mapMessage != null) {
            when (status) {
                Status.AWAIT_HANDSHAKE -> {
                    name = decode<Handshake>(mapMessage).name
                    logName = "${channel.socket().inetAddress.hostAddress}:${channel.socket().port} aka $name"
                    if (clients.putIfAbsent(name!!, this) != null) {
                        println("$logName kicked for duplicating name")
                        writer.writeMessage(MessageEncoder.encode(encode(Kick(Instant.now(), "Get lost, U R not special"))))
                        channel.close()
                        status = Status.DEAD
                        return
                    }

                    writer.writeMessage(encodeBytes(Announcement(Instant.now(), "Welcome! Online: ${clients.keys}")))

                    println("$logName connected")

                    val encodedMessage = encodeBytes(Announcement(Instant.now(), "$name connected"))
                    clients.asSequence()
                            .filter { it.key != name!! }
                            .forEach { it.value.writer.writeMessage(encodedMessage) }

                    status = Status.ONLINE
                }
                Status.ONLINE -> {
                    val receivedMessage = decode<Message>(mapMessage)
                    println("$logName incoming message of length ${receivedMessage.text.length}")

                    val encodedMessage = encodeBytes(MessageUpdate(name!!, Instant.now(), receivedMessage.text.trim()))
                    clients.asSequence()
                            .filter { it.key != name }
                            .forEach { it.value.writer.writeMessage(encodedMessage) }
                }
                Status.DEAD -> return
            }

            mapMessage = receiver.findMessage()
        }
        inBuffer.clear()
    }

    fun handleDisconnect(clients: ConcurrentHashMap<String, ServerClientConnection>) {
        if (name == null) {
            return
        }
        clients.remove(name)
        println("$logName disconnected")
        val encodedMessage = encodeBytes(Announcement(Instant.now(), "$name disconnected"))
        clients.forEach { it.value.writer.writeMessage(encodedMessage) }
    }

    private inline fun <reified T : Any> encodeBytes(data: T) = MessageEncoder.encode(encode(data))

    companion object {
        private val inBuffer: ByteBuffer = ByteBuffer.allocate(MessageAccumulator.bufferSize)
    }
}