package ntpserver

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
import java.net.URISyntaxException
import kotlin.system.exitProcess
import kotlin.time.ExperimentalTime

object Server {
    private val selector = ActorSelectorManager(Dispatchers.IO)

    private fun warn(condition: Boolean, message: String) {
        if (!condition)
            println(message)
    }

    @ExperimentalTime
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        val parser = ArgParser("ntp-server")
        val address by parser.option(
                type = ArgType.String,
                shortName = "a",
                description = "хост"
        ).default(DEFAULT_ADDRESS)
        val port by parser.option(
                type = ArgType.Int,
                shortName = "p",
                description = "порт"
        ).default(DEFAULT_PORT)
        val reference by parser.option(
                type = ArgType.String,
                shortName = "r",
                description = "id источника"
        ).default("PPS")
        val stratum by parser.option(
                type = ArgType.Int,
                shortName = "s",
                description = "слой"
        ).default(2)
        parser.parse(args)

        try {
            val socket = aSocket(selector).udp().bind(InetSocketAddress(address, port))
            println("Сервер запущен")

            while (true) {
                val inDgram = socket.receive()
                val t2 = Clock.System.now().toNTPTimestamp()
                try {
                    println("Входящая дейтаграмма от ${inDgram.address}")
                    val incoming = Packet.decode(inDgram.packet)
                    println("Получено: $incoming")
                    warn(incoming.mode == Packet.Mode.Client,
                            "Недопустимое значение mode: ${incoming.mode}")
                    warn(incoming.poll in MINPOLL..MAXPOLL,
                            "Недопустимое значение poll: ${incoming.poll}")
                    warn(incoming.version <= VERSION,
                            "Недопустимое значение version: ${incoming.version}")
                    warn(incoming.rootDispersion.toDuration() in MINDISP..MAXDISP,
                            "Недопустимое значение rootDispersion: ${incoming.rootDispersion.toDuration()}")

                    val t3 = Clock.System.now().toNTPTimestamp()
                    val toSend = Packet(
                            leap = Packet.Leap.NoWarning,
                            version = VERSION,
                            mode = Packet.Mode.Server,
                            stratum = Packet.Stratum.fromCode(stratum.toByte()),
                            poll = incoming.poll,
                            precision = -29,
                            rootDelay = NTPShort(0, 0),
                            rootDispersion = NTPShort(1, 0),
                            referenceId = reference,
                            originTimestamp = NTPTimestamp(0, 0),
                            receiveTimestamp = incoming.transmitTimestamp,
                            transmitTimestamp = t2,
                            destinationTimestamp = t3
                    )
                    socket.send(Datagram(buildPacket { toSend.encode(this) }, inDgram.address))
                    println("Отправлено: $toSend")
                } catch (e: Exception) {
                    e.printStackTrace()
                    val toSend = Packet(
                            leap = Packet.Leap.NoWarning,
                            version = VERSION,
                            mode = Packet.Mode.Server,
                            stratum = Packet.Stratum.Unspecified,
                            poll = 0,
                            precision = 0,
                            rootDelay = NTPShort(0, 0),
                            rootDispersion = NTPShort(1, 0),
                            referenceId = "DENY",
                            originTimestamp = NTPTimestamp(0, 0),
                            receiveTimestamp = NTPTimestamp(0, 0),
                            transmitTimestamp = NTPTimestamp(0, 0),
                            destinationTimestamp = NTPTimestamp(0, 0)
                    )
                    socket.send(Datagram(buildPacket { toSend.encode(this) }, inDgram.address))
                }
            }
        } catch (e: URISyntaxException) {
            println("Недопустимый адрес")
            exitProcess(2)
        } catch (e: Exception) {
            e.printStackTrace()
            exitProcess(1)
        }
    }
}