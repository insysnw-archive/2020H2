import NTPPacket.Companion.decode
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.core.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import java.net.InetSocketAddress
import kotlin.system.exitProcess

const val DEFAULT_PORT = 123
const val DEFAULT_ADDRESS = "127.0.0.1"
const val DEFAULT_REF_ID = "PPS"

const val DUMMY_PRECISION = -29
val NULL_TIMESTAMP = Timestamp64(0, 0)
val ROOT_DELAY = Timestamp32(1, 0)
val ROOT_DISPERSION = Timestamp32(1, 0)

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

    val reference by parser.option(
        type = ArgType.String,
        shortName = "r"
    ).default(DEFAULT_REF_ID)

    val stratum by parser.option(
        type = ArgType.Int,
        shortName = "s"
    ).default(1)

    parser.parse(args)

    try {

        println("Server started with address $address ; port $port")

        val socket = aSocket(ktorSelector).udp().bind(InetSocketAddress(address, port))

        while (true) {
            val datagram = socket.receive()
            val transmitTimestamp = Clock.System.now().toNTP()
            println("Datagram from ${datagram.address} recieved")

            try {
                val request = datagram.packet.decode()
                println("Decoded: $request")

                check(request)

                val destinationTimestamp = Clock.System.now().toNTP()
                val response = NTPPacket(
                    leap = Leap.NoWarning,
                    version = VERSION,
                    mode = Mode.Server,
                    stratum = Stratum.fromCode(stratum.toByte()),
                    poll = request.poll,
                    precision = DUMMY_PRECISION,
                    rootDelay = ROOT_DELAY,
                    rootDispersion = ROOT_DISPERSION,
                    referenceId = reference,
                    originTimestamp = NULL_TIMESTAMP,
                    receiveTimestamp = request.transmitTimestamp,
                    transmitTimestamp = transmitTimestamp,
                    destinationTimestamp = destinationTimestamp
                )
                socket.send(Datagram(buildPacket { response.encode(this) }, datagram.address))
                println("RESPONSE: $response")
            } catch (e: Exception) {

                println("ERROR occured ${e.message}")

                val response = NTPPacket(
                    leap = Leap.NoWarning,
                    version = VERSION,
                    mode = Mode.Server,
                    stratum = Stratum.Unspecified,
                    poll = 0,
                    precision = 0,
                    rootDelay = ROOT_DELAY,
                    rootDispersion = ROOT_DISPERSION,
                    referenceId = "NOPE",
                    originTimestamp = NULL_TIMESTAMP,
                    receiveTimestamp = NULL_TIMESTAMP,
                    transmitTimestamp = NULL_TIMESTAMP,
                    destinationTimestamp = NULL_TIMESTAMP
                )
                socket.send(Datagram(buildPacket { response.encode(this) }, datagram.address))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        exitProcess(1)
    }

}

fun check(packet: NTPPacket) {
    require(packet.mode == Mode.Client) {
        "Wrong mode: ${packet.mode}"
    }
    check(packet.version <= VERSION) {
        "Wrong version: ${packet.version}"
    }
}
