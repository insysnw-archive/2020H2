package com.github.antoshka77.inet.dns.records

import com.github.antoshka77.inet.dns.DNSDecoder
import com.github.antoshka77.inet.dns.Record
import com.github.antoshka77.inet.dns.RecordFactory
import com.google.auto.service.AutoService
import kotlinx.serialization.Serializable

@Serializable
class TXT(val texts: List<String>) : Record() {
    @AutoService(RecordFactory::class)
    companion object : RecordFactory(16, "TXT") {
        override fun decode(decoder: DNSDecoder): TXT {
            val result = mutableListOf<String>()
            while (decoder.remaining != 0L) {
                val size = decoder.decodeInt8().toInt() and 0xFF
                result += String(decoder.decodeBytes(size), Charsets.US_ASCII)
            }
            return TXT(result)
        }
    }
}
