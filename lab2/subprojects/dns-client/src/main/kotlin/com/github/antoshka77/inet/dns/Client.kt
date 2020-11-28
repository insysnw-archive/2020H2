package com.github.antoshka77.inet.dns

import com.github.antoshka77.inet.dns.records.A
import com.github.antoshka77.inet.dns.util.getNameServer
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.vararg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.InetSocketAddress
import kotlin.random.Random

val selector = ActorSelectorManager(Dispatchers.IO)

val random = Random(System.currentTimeMillis())

@Serializable
data class ClientOutput(val server: String, val request: Packet, val response: Packet)

fun main(args: Array<String>) {
    val parser = ArgParser("dns-client")
    val server by parser.option(
        ArgType.String,
        shortName = "s",
        description = "NS server to ask (default from /etc/resolv.conf)"
    )
    val `class` by parser.option(ArgType.String, shortName = "c", description = "record DNS class").default("IN")
    val questions by parser.argument(ArgType.String).vararg()
    parser.parse(args)

    val address = server ?: getNameServer()
    val endpoint = InetSocketAddress(address, 53)
    val socket = aSocket(selector).udp().connect(endpoint)

    val json = Json {}

    val objectClass = RClass.fromString(`class`)
    val objectQuestions = questions.map { parseQuestion(it, objectClass) }

    runBlocking {
        val outgoing = Packet(
            id = random.nextInt().toShort(),
            isResponse = false,
            opcode = Opcode.QUERY,
            isAuthoritative = false,
            isTruncated = false,
            recursionDesired = true,
            recursionAvailable = false,
            Z = 0,
            rcode = Rcode.NoError,
            questions = objectQuestions,
            answers = emptyList(),
            authority = emptyList(),
            additional = emptyList()
        )
        val encoder = DNSEncoder()
        outgoing.encode(encoder)
        val outDgram = Datagram(encoder.buildPacket(), endpoint)
        socket.send(outDgram)
        val inDgram = socket.receive()
        val decoder = DNSDecoder(inDgram.packet)
        val incoming = Packet.decode(decoder)
        val result = ClientOutput(inDgram.address.toString(), outgoing, incoming)
        val resultString = json.encodeToString(ClientOutput.serializer(), result)
        println(resultString)
    }
}

fun parseQuestion(string: String, rClass: RClass): Question {
    val index = string.indexOf(':')
    return if (index == -1) {
        Question(string, A.typeId, rClass)
    } else {
        Question(string.substring(index + 1), RecordFactory[string.substring(0, index)].typeId, rClass)
    }
}
