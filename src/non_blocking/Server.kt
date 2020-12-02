package non_blocking

import common.*
import java.io.IOException
import java.net.InetSocketAddress
import java.net.SocketException
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import kotlin.system.exitProcess

class Server(addr: String, port: Int) {
    private val sockets = mutableMapOf<SocketChannel, String>()

    init {
        val selector = Selector.open()
        val serverSocket = try {
            ServerSocketChannel.open().apply {
                bind(InetSocketAddress(addr, port))
                configureBlocking(false)
                register(selector, SelectionKey.OP_ACCEPT)
            }
        }
        catch (e: IOException) {
            System.err.println(Strings.SERVER_NOT_STARTED)
            exitProcess(-1)
        }
        println(Strings.SERVER_STARTED)

        while (true) {
            selector.select()
            val selectedKeys = selector.selectedKeys()
            val iter = selectedKeys.iterator()
            while (iter.hasNext()) {
                val key = iter.next()
                if (key.isAcceptable) {
                    register(selector, serverSocket)
                }
                if (key.isReadable) {
                    readAndSendResponse(key)
                }
                iter.remove()
            }
        }
    }

    @Throws(IOException::class)
    private fun readAndSendResponse(key: SelectionKey) {
        val socket: SocketChannel = key.channel() as SocketChannel

        when (val received = try {
            socket.readMessage()
        } catch (e: SocketException) {
            DisconnectionRequest(sockets[socket] ?: return)
        }) {
            is ConnectionRequest -> {
                if (received.username in sockets.values) {
                    socket.writeMessage(ConnectionDenied(received.username))
                } else {
                    val username = received.username
                    sockets[socket] = username
                    println(ConnectionResponse(username))
                    sockets.forEach {
                        it.key.writeMessage(ConnectionResponse(username))
                    }
                }
            }
            is DisconnectionRequest -> {
                println(DisconnectionResponse(received.username))
                sockets.remove(socket)
                sockets.forEach {
                    it.key.writeMessage(DisconnectionResponse(received.username))
                }
            }
            is UserMessage -> {
                println(received)
                sockets.forEach {
                    it.key.writeMessage(received)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun register(selector: Selector, serverSocket: ServerSocketChannel) {
        val socket = serverSocket.accept()
        socket.configureBlocking(false)
        socket.register(selector, SelectionKey.OP_READ)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            when {
                args.size >= 2 -> Server(args[0], args[1].toIntOrNull() ?: DEFAULT_PORT)
                args.size == 1 -> Server(args[0], DEFAULT_PORT)
                else -> Server(DEFAULT_ADDRESS, DEFAULT_PORT)
            }
        }
    }
}
