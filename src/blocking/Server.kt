package blocking

import common.*
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import kotlin.system.exitProcess

class Server(addr: String, port: Int) {
    val connections = mutableMapOf<ServerConnection, String>()

    init {
        val server = try {
            ServerSocket(port, -1, InetAddress.getByName(addr))
        } catch (e: Exception) {
            System.err.println(Strings.SERVER_NOT_STARTED)
            exitProcess(-1)
        }
        println(Strings.SERVER_STARTED)
        server.use {
            while (true) {
                val socket = it.accept()
                try {
                    ServerConnection(socket)
                } catch (e: IOException) {
                    socket.close()
                }
            }
        }
    }

    inner class ServerConnection(private val socket: Socket) : Thread() {
        private val output = ObjectOutputStream(socket.getOutputStream())
        private val input = ObjectInputStream(socket.getInputStream())

        init {
            start()
        }

        private fun ObjectOutputStream.writeAndFlush(obj: Any) {
            writeObject(obj)
            flush()
        }

        override fun run() {
            try {
                listener@ while (!socket.isClosed) {
                    when (val received = try {
                        input.readObject()
                    } catch (e: SocketException) {
                        DisconnectionRequest(connections[this] ?: return)
                    }) {
                        is ConnectionRequest -> {
                            val username = received.username
                            if (username in connections.values)
                                output.writeAndFlush(ConnectionDenied(username))
                            else {
                                connections[this] = username
                                println(ConnectionResponse(username))
                                connections.forEach {
                                    it.key.output.writeAndFlush(ConnectionResponse(username))
                                }
                            }
                        }
                        is DisconnectionRequest -> {
                            println(DisconnectionResponse(received.username))
                            connections.remove(this)
                            connections.forEach {
                                it.key.output.writeAndFlush(DisconnectionResponse(received.username))
                            }
                            shutdown()
                            break@listener
                        }
                        is UserMessage -> {
                            println(received)
                            connections.forEach {
                                it.key.output.writeAndFlush(received)
                            }
                        }
                    }

                }
            } catch (e: SocketException) {
                connections.remove(this)
                shutdown()
            }
        }

        private fun shutdown() {
            if (!socket.isClosed) {
                input.close()
                output.close()
                socket.close()
                this.interrupt()
            }
        }
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
