package com.github.antoshka77.inet.dns.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.Inet4Address

object Inet4AddressSerializer : KSerializer<Inet4Address> {
    override val descriptor = PrimitiveSerialDescriptor("Inet4Address", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder) = Inet4Address.getByName(decoder.decodeString()) as Inet4Address
    override fun serialize(encoder: Encoder, value: Inet4Address) = encoder.encodeString(value.hostAddress)
}
