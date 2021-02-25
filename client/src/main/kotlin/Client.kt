import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import protocol.*
import utils.BlockingReceiver
import utils.IoFacade
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

const val DEFAULT_PORT = 8888
const val DEFAULT_ADDRESS = "127.0.0.1"
const val DEFAULT_NAME = "ManWithNoName"


fun main(args: Array<String>) {

    registerMapping(Update::class)

    val parser = ArgParser("client")

    val address by parser.option(
        type = ArgType.String,
        shortName = "a"
    ).default(DEFAULT_ADDRESS)

    val port by parser.option(
        type = ArgType.Int,
        shortName = "p"
    ).default(DEFAULT_PORT)

    val name by parser.option(
        type = ArgType.String,
        shortName = "n"
    ).default(DEFAULT_NAME)

    parser.parse(args)

    val run = AtomicBoolean(true)
    val keepConnection = AtomicBoolean(true)
    val currentConnection = AtomicReference<IoFacade>()

    val pingDaemon = Executors.newSingleThreadScheduledExecutor { Thread(it).apply { isDaemon = true } }
    pingDaemon.scheduleAtFixedRate({
        val connection = currentConnection.get()
        if (keepConnection.get() && connection != null) {
            try {
                connection.ping()
            } catch (e: Exception) {
                keepConnection.set(false)
            }
        }
    }, 0, 500, TimeUnit.MILLISECONDS)

    while (run.get()) {
        keepConnection.set(true)
        try {
            Socket(address, port).use { socket ->
                socket.sendBufferSize = BlockingReceiver.bufferSize
                socket.receiveBufferSize = BlockingReceiver.bufferSize
                val connection = IoFacade(socket.getInputStream(), socket.getOutputStream())
                currentConnection.set(connection)

                thread {
                    try {
                        while (keepConnection.get()) {
                            when (val message = decodeMapped(connection.waitForMessage())) {
                                is Update -> println("${message.sender}: ${message.text}")
                                else -> println("unknown message")
                            }
                        }
                    } catch (e: Exception) {
                        keepConnection.set(false)
                        System.err.println("${e.javaClass.simpleName} ${e.message}")
                    }
                }

                println("Connecting to $address:$port as $name")

                connection.writeMessage(encode(Handshake(name)))

                println("Please, write message in the following format \"name -> message\"")
                println("Omitting \"*name->\" part or putting * for name would make a broadcast message")

                while (keepConnection.get()) {
                    if (keepConnection.get() && System.`in`.available() > 0) {
                        readLine()?.let { connection.writeMessage(encode(it.parse())) }
                    }
                    Thread.sleep(10) // debounce
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