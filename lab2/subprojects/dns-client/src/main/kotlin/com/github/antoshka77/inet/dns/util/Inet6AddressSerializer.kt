package com.github.antoshka77.inet.dns.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.Inet6Address

object Inet6AddressSerializer : KSerializer<Inet6Address> {
    override val descriptor = PrimitiveSerialDescriptor("Inet6Address", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder) = Inet6Address.getByName(decoder.decodeString()) as Inet6Address
    override fun serialize(encoder: Encoder, value: Inet6Address) = encoder.encodeString(value.hostAddress)
}
