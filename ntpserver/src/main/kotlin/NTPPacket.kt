import Timestamp32.Companion.readNTP32
import Timestamp32.Companion.writeNTP32
import Timestamp64.Companion.readNTP64
import Timestamp64.Companion.writeNTP64
import io.ktor.utils.io.core.*
import kotlin.text.String
import kotlin.text.toByteArray

const val VERSION = 4

const val LEAP_OFFSET = 6
const val VERSION_OFFSET = 3

const val LEAP_MASK = 0b11
const val VERSION_MASK = 0b111
const val MODE_MASK = 0b111

data class NTPPacket(
        val leap: Leap,
        val version: Int,
        val mode: Mode,
        val stratum: Stratum,
        val poll: Int,
        val precision: Int,
        val rootDelay: Timestamp32,
        val rootDispersion: Timestamp32,
        val referenceId: String,
        val originTimestamp: Timestamp64,
        val receiveTimestamp: Timestamp64,
        val transmitTimestamp: Timestamp64,
        val destinationTimestamp: Timestamp64,
) {

    fun encode(output: Output) {
        output.writeByte(makeHeader().toByte())
        output.writeByte(stratum.code)
        output.writeByte(poll.toByte())
        output.writeByte(precision.toByte())
        output.writeNTP32(rootDelay)
        output.writeNTP32(rootDispersion)
        output.writeFully(referenceId.to4ByteArray(), 0, REF_ID_BYTES)
        output.writeNTP64(originTimestamp)
        output.writeNTP64(receiveTimestamp)
        output.writeNTP64(transmitTimestamp)
        output.writeNTP64(destinationTimestamp)
    }

    private fun makeHeader() = (leap.ordinal and LEAP_MASK shl LEAP_OFFSET)
            .or(version and VERSION_MASK shl VERSION_OFFSET)
            .or(mode.ordinal and MODE_MASK)

    companion object {
        const val REF_ID_BYTES = 4

        private fun String.to4ByteArray() = this.toByteArray(Charsets.US_ASCII).copyOf(REF_ID_BYTES)

        fun Input.decode(): NTPPacket {
            val header = readByte().toInt()
            return NTPPacket(
                    leap = decodeLeap(header),
                    version = decodeVersion(header),
                    mode = decodeMode(header),
                    stratum = Stratum.fromCode(readByte()),
                    poll = readByte().toInt(),
                    precision = readByte().toInt(),
                    rootDelay = readNTP32(),
                    rootDispersion = readNTP32(),
                    referenceId = with(ByteArray(REF_ID_BYTES)) {
                        readFully(this)
                        val length = this.indexOf(0).let { if (it == -1) this.size else it }
                        String(this, 0, length)
                        String(this)
                    },
                    originTimestamp = readNTP64(),
                    receiveTimestamp = readNTP64(),
                    transmitTimestamp = readNTP64(),
                    destinationTimestamp = readNTP64()
            )
        }

        private fun decodeLeap(header: Int) = Leap.values()[(header ushr LEAP_OFFSET) and LEAP_MASK]
        private fun decodeVersion(header: Int) = ((header ushr VERSION_OFFSET) and VERSION_MASK)
        private fun decodeMode(header: Int) = Mode.values()[header and MODE_MASK]

    }

}

enum class Leap {
    NoWarning, LastMinute61Seconds, LastMinute59Seconds, Unknown
}

enum class Mode {
    Reserved, SymmetricActive, SymmetricPassive, Client, Server, Broadcast, NTPControlMessage, PrivateReserved
}

sealed class Stratum(val code: Byte) {
    object Unspecified : Stratum(0)
    object Primary : Stratum(1)
    class Secondary(code: Byte) : Stratum(code)
    object Unsynchronized : Stratum(16)
    class Reserved(code: Byte) : Stratum(code)

    companion object {
        fun fromCode(code: Byte) = when (code) {
            0.toByte() -> Unspecified
            1.toByte() -> Primary
            in 2..15 -> Secondary(code)
            16.toByte() -> Unsynchronized
            in 17..255 -> Reserved(code)
            else -> error("$code ! in 0..255")
        }
    }
}
