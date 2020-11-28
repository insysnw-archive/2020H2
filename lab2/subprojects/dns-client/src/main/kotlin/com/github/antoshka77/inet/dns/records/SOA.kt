package com.github.antoshka77.inet.dns.records

import com.github.antoshka77.inet.Int32
import com.github.antoshka77.inet.dns.DNSDecoder
import com.github.antoshka77.inet.dns.Record
import com.github.antoshka77.inet.dns.RecordFactory
import com.google.auto.service.AutoService
import kotlinx.serialization.Serializable

@Serializable
class SOA(
    val origin: String,
    val admin: String,
    val serial: Int32,
    val refresh: Int32,
    val retry: Int32,
    val expire: Int32,
    val minimum: Int32
) : Record() {
    @AutoService(RecordFactory::class)
    companion object : RecordFactory(6, "SOA") {
        override fun decode(decoder: DNSDecoder) = SOA(
            origin = decoder.decodeName(),
            admin = decoder.decodeName(),
            serial = decoder.decodeInt32(),
            refresh = decoder.decodeInt32(),
            retry = decoder.decodeInt32(),
            expire = decoder.decodeInt32(),
            minimum = decoder.decodeInt32()
        )
    }
}
