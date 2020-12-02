package non_blocking

import common.*
import java.io.IOException
import java.net.InetSocketAddress
import java.net.SocketException
import java.nio.channels.SocketChannel
import kotlin.system.exitProcess

class Client(addr: String, port: Int) {
    private var socketChannel: SocketChannel
    private var username: String? = null

    init {
        try {
            socketChannel = SocketChannel.open(InetSocketAddress(addr, port))
        } catch (e: IOException) {
            System.err.println(Strings.SOCKET_NOT_CREATED)
            exitProcess(-1)
        }

        try {
            enterUsername()
            readThread().start()
            writeThread().start()
        } catch (e: IOException) {
            shutdown(Status.EXCEPTION)
        }
    }

    private fun enterUsername() {
        print(Strings.ENTER_USERNAME)
        while (username == null) {
            try {
                var userInput = readLine()
                while (userInput != null && userInput.contains(Regex("""[\[\]]"""))) {
                    System.err.print(Strings.BAD_USERNAME)
                    userInput = readLine()
                }

                socketChannel.writeMessage(ConnectionRequest(userInput!!))
                when (socketChannel.readMessage()) {
                    is ConnectionResponse -> {
                        username = userInput
                        println(Strings.HELLO(username))
                    }
                    is ConnectionDenied -> {
                        System.err.print(Strings.TAKEN_USERNAME)
                    }
                }
            } catch (e: SocketException) {
                shutdown(Status.EXCEPTION)
            }
        }
    }

    @Throws(IOException::class)
    private fun shutdown(status: Status) {
        println(status.message)
        socketChannel.close()
        exitProcess(status.code)
    }

    private fun readThread() = Thread {
        try {
            while (true) {
                println(socketChannel.readMessage())
            }
        } catch (e: SocketException) {
            shutdown(Status.EXCEPTION)
        }
    }

    private fun writeThread() = Thread {
        try {
            while (true) {
                val userInput = readLine()
                if (userInput == STOP_WORD) {
                    socketChannel.writeMessage(DisconnectionRequest(username!!))
                    shutdown(Status.OK)
                    break
                } else {
                    socketChannel.writeMessage(UserMessage(username!!, userInput!!))
                }
            }
        } catch (e: SocketException) {
            shutdown(Status.EXCEPTION)
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            when {
                args.size >= 2 -> Client(args[0], args[1].toIntOrNull() ?: DEFAULT_PORT)
                args.size == 1 -> Client(args[0], DEFAULT_PORT)
                else -> Client(DEFAULT_ADDRESS, DEFAULT_PORT)
            }
        }
    }
}
