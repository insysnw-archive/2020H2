package com.github.antoshka77.inet.dns

import com.github.antoshka77.inet.Int16
import com.github.antoshka77.inet.Int32
import com.github.antoshka77.inet.Int8
import io.ktor.utils.io.core.*

class DNSEncoder private constructor(private var cache: MutableMap<String, Int>, private var offset: Int) {
    private val packet = BytePacketBuilder()

    constructor() : this(hashMapOf(), 0)

    private inline fun displace(count: Int, block: () -> Unit) {
        val result = block()
        offset += count
        return result
    }

    fun encodeInt32(value: Int32) = displace(Int32.SIZE_BYTES) { packet.writeInt(value) }

    fun encodeInt16(value: Int16) = displace(Int16.SIZE_BYTES) { packet.writeShort(value) }

    fun encodeInt8(value: Int8) = displace(Int8.SIZE_BYTES) { packet.writeByte(value) }

    fun encodeBytes(value: ByteArray) = displace(value.size) { packet.writeFully(value) }

    private fun encodeLabel(label: String) {
        val bytes = label.encodeToByteArray()
        if (bytes.size > 0x3F) {
            formatError("label \"$label\" is too big")
        }
        encodeInt8(bytes.size.toByte())
        encodeBytes(bytes)
    }

    fun encodeName(name: String) = encodeNamePrivate(name.removeSuffix("."))

    private fun encodeNamePrivate(name: String) {
        val encoded = cache[name]
        if (encoded != null && encoded <= 0x3FFF) {
            val ptr = (encoded or 0xC000).toShort()
            encodeInt16(ptr)
        } else {
            cache[name] = offset // cache this name
            val index = name.indexOf('.')
            if (index == -1) {
                // last label
                encodeLabel(name)
                encodeInt8(0)
            } else {
                // next label
                encodeLabel(name.substring(0, index))
                encodeNamePrivate(name.substring(index + 1))
            }
        }
    }

    fun buildPacket() = packet.build()
}
