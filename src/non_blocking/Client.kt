package non_blocking

import common.*
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel
import kotlin.system.exitProcess

class Client(addr: String, port: Int) {
    private var socket: SocketChannel
    private var username: String? = null


    init {
        try {
            socket = SocketChannel.open(InetSocketAddress(addr, port))
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
                while (userInput == null) {
                    System.err.print(Strings.BAD_USERNAME)
                    userInput = readLine()
                }
                userInput = "$userInput"
                socket.writeMessage(ConnectionRequest(userInput!!))
                when (socket.readMessage()) {
                    is ConnectionResponse -> {
                        username = userInput
                        println(Strings.HELLO(username))
                    }
                    is ConnectionDenied -> {
                        System.err.print(Strings.TAKEN_USERNAME)
                    }
                }
            } catch (e: IOException) {
                shutdown(Status.EXCEPTION)
            }
        }
    }

    private fun shutdown(status: Status) {
        try {
            socket.close()
        }
        catch (e: IOException) {
        }
        println(status.message)
        exitProcess(status.code)
    }

    private fun readThread() = Thread {
        try {
            while (true) {
                println(socket.readMessage())
            }
        } catch (e: IOException) {
            shutdown(Status.EXCEPTION)
        }
    }

    private fun writeThread() = Thread {
        try {
            while (true) {
                val userInput = readLine()
                if (userInput == "quit" || userInput == null) {
                    socket.writeMessage(DisconnectionRequest(username!!))
                    shutdown(Status.OK)
                    break
                } else {
                    socket.writeMessage(UserMessage(username!!, userInput))
                }
            }
        } catch (e: IOException) {
            shutdown(Status.EXCEPTION)
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val bufferedReader: BufferedReader = File("resources/address.txt").bufferedReader()
            val (address, port) = bufferedReader.use { it.readLine() }.split(":")
            when {
                args.size >= 2 -> Client(args[0], args[1].toIntOrNull() ?: port.toInt())
                args.size == 1 -> Client(args[0], port.toInt())
                else -> Client(address, port.toInt())
            }
        }
    }
}
