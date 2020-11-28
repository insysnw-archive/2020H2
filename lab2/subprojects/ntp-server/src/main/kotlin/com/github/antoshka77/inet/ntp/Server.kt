package com.github.antoshka77.inet.dns

import com.github.antoshka77.inet.ntp.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.core.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import mu.KotlinLogging
import java.net.InetSocketAddress
import java.net.URI
import java.net.URISyntaxException
import kotlin.system.exitProcess

val log = KotlinLogging.logger { }

private val selector = ActorSelectorManager(Dispatchers.IO)

inline fun ntpCheck(value: Boolean, crossinline block: () -> String) {
    if (!value) {
        log.warn { block() }
    }
}

suspend fun main(args: Array<String>) {
    val parser = ArgParser("ntp-server")
    val bind by parser.option(
        type = ArgType.String,
        shortName = "b",
        description = "UDP address to bind to"
    ).default("localhost:$PORT")
    val reference by parser.option(
        type = ArgType.String,
        shortName = "r",
        description = "reference id value"
    ).default("PPS")
    val stratum by parser.option(
        type = ArgType.Int,
        shortName = "s",
        description = "NTP stratum level"
    ).default(2)
    parser.parse(args)

    try {
        val uri = URI("udp://$bind")
        val host = uri.host
        val port = uri.port.let { if (it == -1) PORT else it }
        val address = InetSocketAddress(host, port)
        val socket = aSocket(selector).udp().bind(address)
        while (true) {
            val inDgram = socket.receive()
            val t2 = Clock.System.now().toNTPTimestamp()
            try {
                log.info { "incoming datagram from ${inDgram.address}" }
                val incoming = Packet.decode(inDgram.packet)
                log.info { "received: $incoming" }
                ntpCheck(incoming.mode == Packet.Mode.Client) { "only operates with clients" }
                ntpCheck(incoming.poll in MINPOLL..MAXPOLL) { "forbidden poll value" }
                ntpCheck(incoming.version <= VERSION) { "unsupported NTP version" }
                ntpCheck(incoming.rootDispersion.toDuration() in MINDISP..MAXDISP) {
                    "dispersion out of bounds"
                }
                val t3 = Clock.System.now().toNTPTimestamp()
                val toSend = Packet(
                    leap = Packet.Leap.NoWarning,
                    version = VERSION,
                    mode = Packet.Mode.Server,
                    stratum = Packet.Stratum.fromCode(stratum.toByte()),
                    poll = incoming.poll,
                    precision = -29, // ~ 1 nanosecond
                    rootDelay = NTPShort(0, 0),
                    rootDispersion = NTPShort(1, 0),
                    referenceId = reference,
                    originTimestamp = NTPTimestamp(0, 0),
                    receiveTimestamp = incoming.transmitTimestamp,
                    transmitTimestamp = t2,
                    destinationTimestamp = t3
                )
                socket.send(Datagram(buildPacket { toSend.encode(this) }, inDgram.address))
                log.info { "sent: $toSend" }
            } catch (e: Exception) {
                log.error { "ntp format error: ${e.message}" }
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
                log.info(e) { "sent error: $toSend" }
            }
        }
    } catch (e: URISyntaxException) {
        log.error { "invalid address for binding" }
        exitProcess(2)
    } catch (e: Exception) {
        log.error(e) { "fatal server error" }
        exitProcess(1)
    }
}
