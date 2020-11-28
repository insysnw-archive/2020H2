package com.github.antoshka77.inet.dns

import com.github.antoshka77.inet.Int16
import com.github.antoshka77.inet.Int32
import com.github.antoshka77.inet.Int8
import io.ktor.utils.io.core.*

class DNSDecoder private constructor(
    private val packet: ByteReadPacket,
    private val cache: MutableMap<Int, String>,
    private var offset: Int
) {
    constructor(packet: ByteReadPacket) : this(packet, mutableMapOf(), 0)

    private inline fun <reified R> displace(count: Int, block: () -> R): R {
        val result = block()
        offset += count
        return result
    }

    fun decodeInt32(): Int32 = displace(Int32.SIZE_BYTES) { packet.readInt() }

    fun decodeInt16(): Int16 = displace(Int16.SIZE_BYTES) { packet.readShort() }

    fun decodeInt8(): Int8 = displace(Int8.SIZE_BYTES) { packet.readByte() }

    fun decodeBytes(size: Int): ByteArray = displace(size) { packet.readBytes(size) }

    private fun decodeLabel(size: Int): String {
        return if (size == 0) {
            ""
        } else {
            String(decodeBytes(size), Charsets.US_ASCII)
        }
    }

    fun decodeName(): String {
        val byte = decodeInt8().toInt() and 0xFF
        return when (byte ushr 6) {
            0 -> {
                // read label
                val ptr = offset - 1
                val label = decodeLabel(byte)
                val result = if (byte == 0) {
                    label
                } else {
                    "$label.${decodeName()}"
                }
                cache[ptr] = result
                result
            }
            3 -> {
                // get cached name
                val second = decodeInt8().toInt()
                val key = ((byte and 0x3F) shl 8) or second
                cache[key] ?: throw formatError("no label at pointer $key, offset: $offset, cache: $cache")
            }
            else -> formatError("unknown label type")
        }
    }

    fun decodeRecordData(size: Int): DNSDecoder {
        val newOffset = offset
        val newPacket = buildPacket {
            writeFully(decodeBytes(size))
        }
        return DNSDecoder(newPacket, cache, newOffset)
    }

    val remaining get() = packet.remaining
}
