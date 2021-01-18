package com.dekinci.uni.net.first.nio

import com.dekinci.uni.net.first.*
import java.io.IOException

import java.net.InetSocketAddress
import java.nio.ByteBuffer

import java.nio.channels.SocketChannel
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.random.Random

fun main(args: Array<String>) {
    val serverString = if (args.isEmpty()) "${Random.nextInt(0, 1000)}@localhost:4269" else args[0]
    val adds = serverString.split(":", "@")

    registerMapping(Announcement::class)
    registerMapping(MessageUpdate::class)
    registerMapping(Kick::class)

    val run = AtomicBoolean(true)
    val keepConnection = AtomicBoolean(true)
    val currentConnection = AtomicReference<ByteWriter>()

    val pinguin = Executors.newSingleThreadScheduledExecutor()
    pinguin.scheduleAtFixedRate({
        val connection = currentConnection.get()
        if (keepConnection.get() && connection != null) {
            connection.sendPing()
        }
    }, 0, 500, TimeUnit.MILLISECONDS)

    val inBuffer = ByteBuffer.allocate(MessageAccumulator.bufferSize)

    while (run.get()) {
        keepConnection.set(true)
        try {
            val socketChannel = SocketChannel.open(InetSocketAddress(adds[1], adds[2].toInt()))
            socketChannel.configureBlocking(false)

            socketChannel.use { channel ->
                val writer = DumpingWriter()
                val receiver = AccumulatingReceiver()
                currentConnection.set(writer)

                println("Connecting to $serverString")
                writer.writeMessage(encodeBytes(Handshake(adds[0])))

                thread {
                    while (keepConnection.get()) {
                        if (keepConnection.get() && System.`in`.available() > 0) {
                            var text = readLine()
                            if (text == "long")
                                text = "rdtfhmgyhu".repeat(1_000_000)
                            text?.let { writer.writeMessage(encodeBytes(Message(it))) }
                        }
                        // rate and cycle limiter XD
                        Thread.sleep(10)
                    }
                }

                while (keepConnection.get()) {
                    receiver.dumpRemaining(inBuffer)
                    channel.read(inBuffer)
                    inBuffer.flip()
                    receiver.update(inBuffer, writer)

                    var mapMessage = receiver.findMessage()
                    while (mapMessage != null) {
                        when (val message = decodeMapped(mapMessage)) {
                            is MessageUpdate -> println(message)
                            is Announcement -> println(message)
                            is Kick -> {
                                run.set(false)
                                keepConnection.set(false)
                                println(message)
                            }
                        }
                        mapMessage = receiver.findMessage()
                    }
                    inBuffer.clear()

                    writer.send(channel)
                }
                currentConnection.set(null)
            }
        } catch (e: Exception) {
            currentConnection.set(null)
            keepConnection.set(false)
            System.err.println("${e.javaClass.simpleName} ${e.message}")
            Thread.sleep(1000)
        }
    }


}

inline fun <reified T : Any> encodeBytes(data: T) = MessageEncoder.encode(encode(data))

class EchoClient private constructor() {
    fun sendMessage(msg: String): String? {
        buffer = ByteBuffer.wrap(msg.toByteArray())
        var response: String? = null
        try {
            client!!.write(buffer)
            buffer!!.clear()
            client!!.read(buffer)
            response = String(buffer!!.array()).trim { it <= ' ' }
            println("response=$response")
            buffer!!.clear()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return response
    }

    companion object {
        private var client: SocketChannel? = null
        private var buffer: ByteBuffer? = null
        private var instance: EchoClient? = null
        fun start(): EchoClient {
            if (instance == null) instance = EchoClient()
            return instance!!
        }

        fun stop() {
            client!!.close()
            buffer = null
        }
    }

    init {
        try {
            client = SocketChannel.open(InetSocketAddress("localhost", 5454))
            buffer = ByteBuffer.allocate(256)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
