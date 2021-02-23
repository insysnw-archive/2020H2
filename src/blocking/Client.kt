package blocking

import common.*
import java.io.*
import java.net.Socket
import kotlin.system.exitProcess

class Client(addr: String, port: Int) {
    private var socket: Socket
    private lateinit var socketInput: ObjectInputStream
    private lateinit var socketOutput: ObjectOutputStream
    private var username: String? = null

    init {
        try {
            socket = Socket(addr, port)
        } catch (e: IOException) {
            exitProcess(-1)
        }
        try {
            socketInput = ObjectInputStream(socket.getInputStream())
            socketOutput = ObjectOutputStream(socket.getOutputStream())

            enterUsername()
            readThread().start()
            writeThread().start()
        } catch (e: IOException) {
            shutdown(Status.EXCEPTION)
        }
    }

    private fun ObjectOutputStream.writeAndFlush(obj: Any) {
        writeObject(obj)
        flush()
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

                socketOutput.writeAndFlush(ConnectionRequest(userInput!!))

                when (socketInput.readObject()) {
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
            if (!socket.isClosed) {
                socketInput.close()
                socketOutput.close()
                socket.close()
            }
        } catch (e: IOException) {
        }
        println(status.message)
        exitProcess(status.code)
    }

    private fun readThread() = Thread {
        try {
            while (!socket.isClosed) {
                println(socketInput.readObject())
            }
        } catch (e: IOException) {
            shutdown(Status.EXCEPTION)
        }
    }

    private fun writeThread() = Thread {
        try {
            while (!socket.isClosed) {
                val userInput = readLine()
                if (userInput == "quit" || userInput == null) {
                    socketOutput.writeAndFlush(DisconnectionRequest(username!!))
                    shutdown(Status.OK)
                    break
                } else {
                    socketOutput.writeAndFlush(UserMessage(username!!, userInput))
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

