import io.ktor.utils.io.core.*
import kotlinx.datetime.Instant


data class Timestamp32(val seconds: Short, val fraction: Short) {
    companion object {
        fun Input.readNTP32() = Timestamp32(readShort(), readShort())

        fun Output.writeNTP32(time: Timestamp32) {
            writeShort(time.seconds)
            writeShort(time.fraction)
        }
    }
}

data class Timestamp64(val seconds: Int, val fraction: Int) {
    companion object {
        fun Input.readNTP64() = Timestamp64(readInt(), readInt())

        fun Output.writeNTP64(time: Timestamp64) {
            writeInt(time.seconds)
            writeInt(time.fraction)
        }
    }
}

const val NTP_UNIX_TIME_DELTA = 2208988800 //70 years

fun Instant.toNTP(): Timestamp64 {
    val seconds = epochSeconds.toInt()
    val fraction = ((nanosecondsOfSecond.toLong() and 0xFFFF_FFFFL) shl 23) / 1953125
    return Timestamp64((seconds + NTP_UNIX_TIME_DELTA).toInt(), fraction.toInt())
}