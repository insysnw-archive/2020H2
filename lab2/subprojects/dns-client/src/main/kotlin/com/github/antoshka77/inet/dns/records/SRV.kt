package com.github.antoshka77.inet.dns.records

import com.github.antoshka77.inet.Int16
import com.github.antoshka77.inet.dns.DNSDecoder
import com.github.antoshka77.inet.dns.Record
import com.github.antoshka77.inet.dns.RecordFactory
import com.google.auto.service.AutoService
import kotlinx.serialization.Serializable

@Serializable
class SRV(val priority: Int16, val weight: Int16, val port: Int16, val target: String) : Record() {
    @AutoService(RecordFactory::class)
    companion object : RecordFactory(33, "SRV") {
        override fun decode(decoder: DNSDecoder) = SRV(
            priority = decoder.decodeInt16(),
            weight = decoder.decodeInt16(),
            port = decoder.decodeInt16(),
            target = decoder.decodeName()
        )
    }
}
