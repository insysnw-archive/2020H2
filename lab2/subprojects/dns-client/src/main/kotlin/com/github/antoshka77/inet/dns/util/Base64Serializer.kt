package com.github.antoshka77.inet.dns.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*

class Base64Serializer : KSerializer<ByteArray> {
    private val b64Encoder = Base64.getEncoder()
    private val b64Decoder = Base64.getDecoder()

    override val descriptor = PrimitiveSerialDescriptor("Base64ByteArray", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder) = b64Decoder.decode(decoder.decodeString())
    override fun serialize(encoder: Encoder, value: ByteArray) = encoder.encodeString(b64Encoder.encodeToString(value))
}
