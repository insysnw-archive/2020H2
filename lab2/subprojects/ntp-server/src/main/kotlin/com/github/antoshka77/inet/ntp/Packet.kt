package com.github.antoshka77.inet.ntp

import com.github.antoshka77.inet.Int8
import io.ktor.utils.io.core.*
import kotlin.math.min

data class Packet(
    val leap: Leap,
    val version: Int8,
    val mode: Mode,
    val stratum: Stratum,
    val poll: Int8,
    val precision: Int8,
    val rootDelay: NTPShort,
    val rootDispersion: NTPShort,
    val referenceId: String,
    val originTimestamp: NTPTimestamp,
    val receiveTimestamp: NTPTimestamp,
    val transmitTimestamp: NTPTimestamp,
    val destinationTimestamp: NTPTimestamp,
) {
    enum class Leap {
        NoWarning, LastMinute61Seconds, LastMinute59Seconds, Unknown
    }
    enum class Mode {
        Reserved, SymmetricActive, SymmetricPassive, Client, Server, Broadcast, NTPControlMessage, PrivateReserved
    }
    sealed class Stratum(val code: Int8) {
        object Unspecified : Stratum(0)
        object PrimaryServer : Stratum(1)
        class SecondaryServer(code: Int8) : Stratum(code)
        object Unsynchronized : Stratum(16)
        class Reserved(code: Int8) : Stratum(code)

        companion object {
            fun fromCode(code: Int8) = when (code) {
                0.toByte() -> Unspecified
                1.toByte() -> PrimaryServer
                in 2..15 -> SecondaryServer(code)
                16.toByte() -> Unsynchronized
                in 17..255 -> Reserved(code)
                else -> error("can not be there")
            }
        }
    }

    fun encode(output: Output) {
        var flags = 0
        flags = flags or (leap.ordinal shl 6)
        flags = flags or ((version.toInt() and 7) shl 3)
        flags = flags or mode.ordinal
        output.writeByte(flags.toByte())
        output.writeByte(stratum.code)
        output.writeByte(poll)
        output.writeByte(precision)
        output.writeNTPShort(rootDelay)
        output.writeNTPShort(rootDispersion)
        val bytes = referenceId.toByteArray(Charsets.US_ASCII)
        val size = min(bytes.size, referenceIdSize)
        output.writeFully(bytes, 0, size)
        for (i in size until referenceIdSize)
            output.writeByte(0)
        output.writeNTPTimestamp(originTimestamp)
        output.writeNTPTimestamp(receiveTimestamp)
        output.writeNTPTimestamp(transmitTimestamp)
        output.writeNTPTimestamp(destinationTimestamp)
    }

    companion object {
        private inline val end: Int8 get() = 0
        const val referenceIdSize = 4

        private fun strlen(bytes: ByteArray): Int {
            for (i in bytes.indices) {
                if (bytes[i] == end) {
                    return i
                }
            }
            return bytes.size
        }

        fun decode(input: Input): Packet {
            val flags = input.readByte().toInt()
            val leap = Leap.values()[(flags ushr 6) and 3]
            val version = ((flags ushr 3) and 7).toByte()
            val mode = Mode.values()[flags and 7]
            val stratum = Stratum.fromCode(input.readByte())
            val poll = input.readByte()
            val precision = input.readByte()
            val rootDelay = input.readNTPShort()
            val rootDispersion = input.readNTPShort()
            val referenceIdBytes = ByteArray(referenceIdSize)
            input.readFully(referenceIdBytes)
            val referenceId = String(referenceIdBytes, 0, strlen(referenceIdBytes))
            val originTimestamp = input.readNTPTimestamp()
            val receiveTimestamp = input.readNTPTimestamp()
            val transmitTimestamp = input.readNTPTimestamp()
            val destinationTimestamp = input.readNTPTimestamp()
            return Packet(
                leap,
                version,
                mode,
                stratum,
                poll,
                precision,
                rootDelay,
                rootDispersion,
                referenceId,
                originTimestamp,
                receiveTimestamp,
                transmitTimestamp,
                destinationTimestamp
            )
        }
    }
}
