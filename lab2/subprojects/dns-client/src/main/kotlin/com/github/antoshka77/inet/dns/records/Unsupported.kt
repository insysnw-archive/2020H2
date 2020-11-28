@file:UseSerializers(Base64Serializer::class)

package com.github.antoshka77.inet.dns.records

import com.github.antoshka77.inet.dns.DNSDecoder
import com.github.antoshka77.inet.dns.Record
import com.github.antoshka77.inet.dns.RecordFactory
import com.github.antoshka77.inet.dns.util.Base64Serializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
class Unsupported(val data: ByteArray) : Record() {
    companion object : RecordFactory(0, "?") {
        override fun decode(decoder: DNSDecoder) = Unsupported(decoder.decodeBytes(decoder.remaining.toInt()))
    }
}
