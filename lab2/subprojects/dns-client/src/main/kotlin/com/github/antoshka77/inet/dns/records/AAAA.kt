@file:UseSerializers(Inet6AddressSerializer::class)

package com.github.antoshka77.inet.dns.records

import com.github.antoshka77.inet.dns.DNSDecoder
import com.github.antoshka77.inet.dns.Record
import com.github.antoshka77.inet.dns.RecordFactory
import com.github.antoshka77.inet.dns.util.Inet6AddressSerializer
import com.google.auto.service.AutoService
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.net.Inet6Address

@Serializable
class AAAA(val address: Inet6Address) : Record() {
    @AutoService(RecordFactory::class)
    companion object : RecordFactory(28, "AAAA") {
        override fun decode(decoder: DNSDecoder): AAAA {
            val bytes = decoder.decodeBytes(16)
            val address = Inet6Address.getByAddress(bytes) as Inet6Address
            return AAAA(address)
        }
    }
}
