package com.dekinci.uni.net.first.nio

import com.dekinci.uni.net.first.MessageEncoder
import com.dekinci.uni.net.first.encode
import java.io.EOFException
import java.net.InetSocketAddress
import java.net.SocketException
import java.net.StandardSocketOptions
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class NonBlockingServer(val addr: InetSocketAddress) {
    private val clients = ConcurrentHashMap<String, ServerClientConnection>()
    private val selector: Selector = Selector.open()
    private val serverSocket = ServerSocketChannel.open()
    private val pinguin = Executors.newSingleThreadScheduledExecutor()

    init {
        serverSocket.setOption(StandardSocketOptions.SO_REUSEADDR, true)
        serverSocket.bind(addr)
        serverSocket.configureBlocking(false)
        serverSocket.register(selector, SelectionKey.OP_ACCEPT)

        pinguin.scheduleAtFixedRate({
            clients.forEach { it.value.writer.sendPing() }
        }, 0, 500, TimeUnit.MILLISECONDS)

        println("Running server on port ${addr.port}")
    }

    fun handleConnections() {
        selector.select()
        val selectedKeys: MutableSet<SelectionKey> = selector.selectedKeys()
        val iter = selectedKeys.iterator()
        while (iter.hasNext()) {
            val key = iter.next()
            if (key.isAcceptable) {
                val client = serverSocket.accept()
                client.configureBlocking(false)
                client.register(selector, SelectionKey.OP_READ or SelectionKey.OP_WRITE, ServerClientConnection(DumpingWriter(), AccumulatingReceiver()))
            }
            if (key.isReadable) {
                val channel = key.channel() as SocketChannel
                val connection = key.attachment() as ServerClientConnection

                try {
                    connection.readInput(channel)
                    connection.handleRead(channel, clients)
                } catch (e: Exception) {
                    handleException(e, connection)
                    channel.close()
                    continue
                }
            }
            if (key.isWritable) {
                val channel = key.channel() as SocketChannel
                val connection = key.attachment() as ServerClientConnection

                try {
                    connection.writer.send(channel)
                } catch (e: Exception) {
                    handleException(e, connection)
                    channel.close()
                    continue
                }
            }
            iter.remove()
        }
    }

    private fun handleException(e: Exception, connection: ServerClientConnection) {
        when {
            e is SocketException && e.message in okExceptionMessages -> Unit
            e is EOFException -> Unit
            e is IllegalStateException && e.message in okExceptionMessages -> Unit
            else -> e.printStackTrace()
        }

        connection.handleDisconnect(clients)
    }

    private inline fun <reified T : Any> encodeBytes(data: T) = MessageEncoder.encode(encode(data))

    companion object {
        private val okExceptionMessages = listOf(
                "Connection reset",
                "An established connection was aborted by the software in your host machine",
                "Same name"
        )
    }
}