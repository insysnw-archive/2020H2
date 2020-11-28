package com.github.antoshka77.inet.dns

import com.github.antoshka77.inet.Int16
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(RClass.Serializer::class)
sealed class RClass(val id: Int16) {
    object IN : RClass(1) {
        override fun toString() = "IN"
    }
    object CS : RClass(2) {
        override fun toString() = "CS"
    }
    object CH : RClass(3) {
        override fun toString() = "CH"
    }
    object HS : RClass(4) {
        override fun toString() = "HS"
    }
    class Unknown(id: Int16) : RClass(id) {
        override fun toString() = id.toString()
    }

    fun encode(encoder: DNSEncoder) {
        encoder.encodeInt16(id)
    }

    companion object {
        fun fromInt(id: Int16) = when (id.toInt()) {
            1 -> IN
            2 -> CS
            3 -> CH
            4 -> HS
            else -> Unknown(id)
        }

        fun fromString(string: String) = when (string) {
            "IN" -> IN
            "CS" -> CS
            "CH" -> CH
            "HS" -> HS
            else -> fromInt(string.toShort())
        }

        fun decode(decoder: DNSDecoder) = fromInt(decoder.decodeInt16())
    }

    object Serializer : KSerializer<RClass> {
        override val descriptor = PrimitiveSerialDescriptor("RClass", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder) = fromString(decoder.decodeString())
        override fun serialize(encoder: Encoder, value: RClass) = encoder.encodeString(value.toString())
    }
}
