package com.github.antoshka77.inet.dns

import com.github.antoshka77.inet.Int16
import com.github.antoshka77.inet.Int8
import kotlinx.serialization.Serializable

@Serializable
data class Packet(
    val id: Int16,
    val isResponse: Boolean,
    val opcode: Opcode,
    val isAuthoritative: Boolean,
    val isTruncated: Boolean,
    val recursionDesired: Boolean,
    val recursionAvailable: Boolean,
    val Z: Int8,
    val rcode: Rcode,
    val questions: List<Question>,
    val answers: List<Answer>,
    val authority: List<Answer>,
    val additional: List<Answer>
) {
    fun encode(encoder: DNSEncoder) {
        encoder.encodeInt16(id)
        run {
            var octet = 0
            if (isResponse) octet = octet or 0x80
            octet = octet or ((opcode.id and 0xF) shl 3)
            if (isAuthoritative) octet = octet or 0x4
            if (isTruncated) octet = octet or 0x2
            if (recursionDesired) octet = octet or 0x1
            encoder.encodeInt8(octet.toByte())
        }
        run {
            var octet = 0
            if (recursionAvailable) octet = octet or 0x80
            octet = octet or ((Z.toInt() and 0x7) shl 4)
            octet = octet or (rcode.id and 0xF)
            encoder.encodeInt8(octet.toByte())
        }
        val qdCount = questions.size
        check(qdCount <= 0xFFFF) { "too much questions" }
        encoder.encodeInt16(qdCount.toShort())
        // Record encoding not implemented, because not required in client code
        encoder.encodeInt16(0)
        encoder.encodeInt16(0)
        encoder.encodeInt16(0)

        questions.forEach { it.encode(encoder) }
    }

    companion object {
        fun decode(decoder: DNSDecoder): Packet {
            val id = decoder.decodeInt16()
            val flagsA = decoder.decodeInt8().toInt()
            val isResponse = (flagsA and 0x80) != 0
            val opcode = Opcode.fromId((flagsA ushr 3) and 0xF)
            val isAuthoritative = (flagsA and 0x4) != 0
            val isTruncated = (flagsA and 0x2) != 0
            val recursionDesired = (flagsA and 0x1) != 0
            val flagsB = decoder.decodeInt8().toInt()
            val recursionAvailable = (flagsB and 0x80) != 0
            val Z = ((flagsB ushr 4) and 0x7).toByte()
            val rcode = Rcode.fromId(flagsB and 0xF)

            val qdCount = decoder.decodeInt16().toInt() and 0xFFFF
            val anCount = decoder.decodeInt16().toInt() and 0xFFFF
            val nsCount = decoder.decodeInt16().toInt() and 0xFFFF
            val arCount = decoder.decodeInt16().toInt() and 0xFFFF

            val questions = List(qdCount) { Question.decode(decoder) }
            val answers = List(anCount) { Answer.decode(decoder) }
            val authority = List(nsCount) { Answer.decode(decoder) }
            val additional = List(arCount) { Answer.decode(decoder) }

            return Packet(
                id = id,
                isResponse = isResponse,
                opcode = opcode,
                isAuthoritative = isAuthoritative,
                isTruncated = isTruncated,
                recursionDesired = recursionDesired,
                recursionAvailable = recursionAvailable,
                Z = Z,
                rcode = rcode,
                questions = questions,
                answers = answers,
                authority = authority,
                additional = additional
            )
        }
    }
}
