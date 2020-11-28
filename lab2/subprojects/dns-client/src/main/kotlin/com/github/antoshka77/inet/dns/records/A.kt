@file:UseSerializers(Inet4AddressSerializer::class)

package com.github.antoshka77.inet.dns.records

import com.github.antoshka77.inet.dns.DNSDecoder
import com.github.antoshka77.inet.dns.Record
import com.github.antoshka77.inet.dns.RecordFactory
import com.github.antoshka77.inet.dns.util.Inet4AddressSerializer
import com.google.auto.service.AutoService
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.net.Inet4Address

@Serializable
class A(val address: Inet4Address) : Record() {
    @AutoService(RecordFactory::class)
    companion object : RecordFactory(1, "A") {
        override fun decode(decoder: DNSDecoder): A {
            val bytes = decoder.decodeBytes(4)
            val address = Inet4Address.getByAddress(bytes) as Inet4Address
            return A(address)
        }
    }
}
