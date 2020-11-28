package com.github.antoshka77.inet.ntp

import com.github.antoshka77.inet.Int16
import com.github.antoshka77.inet.Int32
import io.ktor.utils.io.core.*
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.seconds

data class NTPTime<T>(val seconds: T, val fraction: T)
        where T : Number, T : Comparable<T>

typealias NTPShort = NTPTime<Int16>
typealias NTPTimestamp = NTPTime<Int32>

operator fun NTPTimestamp.plus(other: NTPTimestamp): NTPTimestamp {
    var seconds = other.seconds + seconds
    val fraction = other.fraction + fraction
    if (fraction xor Int.MIN_VALUE < this.fraction xor Int.MIN_VALUE) {
        ++seconds
    }
    return NTPTimestamp(seconds, fraction)
}

fun NTPShort.toDuration(): Duration = seconds.toInt().seconds + fraction.toInt().seconds / (2 shl 31) / 2

fun Input.readNTPShort() = NTPShort(readShort(), readShort())
fun Input.readNTPTimestamp() = NTPTimestamp(readInt(), readInt())

fun Output.writeNTPShort(time: NTPShort) {
    writeShort(time.seconds)
    writeShort(time.fraction)
}

fun Output.writeNTPTimestamp(time: NTPTimestamp) {
    writeInt(time.seconds)
    writeInt(time.fraction)
}

private const val NTP_DELTA = 2208988800

fun Instant.toNTPTimestamp(): NTPTimestamp {
    val seconds = epochSeconds.toInt()
    val fraction = ((nanosecondsOfSecond.toLong() and 0xFFFF_FFFFL) shl 23) / 1953125
    return NTPTimestamp((seconds + NTP_DELTA).toInt(), fraction.toInt())
}
