package com.github.antoshka77.inet.dns.records

import com.github.antoshka77.inet.dns.DNSDecoder
import com.github.antoshka77.inet.dns.Record
import com.github.antoshka77.inet.dns.RecordFactory
import com.google.auto.service.AutoService
import kotlinx.serialization.Serializable

@Serializable
class NS(val name: String) : Record() {
    @AutoService(RecordFactory::class)
    companion object : RecordFactory(2, "NS") {
        override fun decode(decoder: DNSDecoder) = NS(decoder.decodeName())
    }
}
