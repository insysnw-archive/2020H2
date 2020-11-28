package com.github.antoshka77.inet.dns.records

import com.github.antoshka77.inet.Int16
import com.github.antoshka77.inet.dns.DNSDecoder
import com.github.antoshka77.inet.dns.Record
import com.github.antoshka77.inet.dns.RecordFactory
import com.google.auto.service.AutoService
import kotlinx.serialization.Serializable

@Serializable
class MX(val preference: Int16, val exchange: String) : Record() {
    @AutoService(RecordFactory::class)
    companion object : RecordFactory(15, "MX") {
        override fun decode(decoder: DNSDecoder) = MX(decoder.decodeInt16(), decoder.decodeName())
    }
}
