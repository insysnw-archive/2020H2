import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import kotlin.system.exitProcess

const val DEFAULT_PORT = 123
const val DEFAULT_ADDRESS = "127.0.0.1"


val ktorSelector = ActorSelectorManager(Dispatchers.IO)

fun main(args: Array<String>) = runBlocking {

    val parser = ArgParser("server")

    val address by parser.option(
        type = ArgType.String,
        shortName = "a"
    ).default(DEFAULT_ADDRESS)

    val port by parser.option(
        type = ArgType.Int,
        shortName = "p"
    ).default(DEFAULT_PORT)

    parser.parse(args)

    try {

        println("Server started with address $address ; port $port")

        val socket = aSocket(ktorSelector).udp().bind(InetSocketAddress(address, port))

        while (true) {
            val datagram = socket.receive()
            println("Datagram from ${datagram.address} recieved")

            try {

//                socket.send(Datagram(buildPacket { response.encode(this) }, datagram.address))
//                println("RESPONSE: $response")
            } catch (e: Exception) {

                println("ERROR occured ${e.message}")


//                socket.send(Datagram(buildPacket { response.encode(this) }, datagram.address))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        exitProcess(1)
    }

}
