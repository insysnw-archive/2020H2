package ntpserver

import io.ktor.utils.io.core.*
import kotlin.math.min

data class Packet(
        val leap: Leap,
        val version: Byte,
        val mode: Mode,
        val stratum: Stratum,
        val poll: Byte,
        val precision: Byte,
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

    sealed class Stratum(val code: Byte) {
        object Unspecified : Stratum(0)
        object PrimaryServer : Stratum(1)
        class SecondaryServer(code: Byte) : Stratum(code)
        object Unsynchronized : Stratum(16)
        class Reserved(code: Byte) : Stratum(code)

        companion object {
            fun fromCode(code: Byte) = when (code) {
                0.toByte() -> Unspecified
                1.toByte() -> PrimaryServer
                in 2..15 -> SecondaryServer(code)
                16.toByte() -> Unsynchronized
                in 17..255 -> Reserved(code)
                else -> error("Неожиданный код $code")
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
        const val referenceIdSize = 4

        fun decode(input: Input): Packet {
            val flags = input.readByte().toInt()
            return Packet(
                    leap = Leap.values()[(flags ushr 6) and 3],
                    version = ((flags ushr 3) and 7).toByte(),
                    mode = Mode.values()[flags and 7],
                    stratum = Stratum.fromCode(input.readByte()),
                    poll = input.readByte(),
                    precision = input.readByte(),
                    rootDelay = input.readNTPShort(),
                    rootDispersion = input.readNTPShort(),
                    referenceId = with(ByteArray(referenceIdSize)) {
                        input.readFully(this)
                        val length = this.indexOf(0).let {
                            if (it == -1) this.size else it
                        }
                        String(this, 0, length)
                    },
                    originTimestamp = input.readNTPTimestamp(),
                    receiveTimestamp = input.readNTPTimestamp(),
                    transmitTimestamp = input.readNTPTimestamp(),
                    destinationTimestamp = input.readNTPTimestamp()
            )
        }
    }
}