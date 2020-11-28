package com.github.antoshka77.inet.dns

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(Opcode.Serializer::class)
sealed class Opcode(val id: Int) {
    object QUERY : Opcode(0) {
        override fun toString() = "QUERY"
    }
    object IQUERY : Opcode(1) {
        override fun toString() = "IQUERY"
    }
    object STATUS : Opcode(2) {
        override fun toString() = "STATUS"
    }
    class Unknown(id: Int) : Opcode(id) {
        override fun toString() = "$id"
    }

    companion object {
        fun fromId(id: Int): Opcode {
            return when (id) {
                0 -> QUERY
                1 -> IQUERY
                2 -> STATUS
                else -> Unknown(id)
            }
        }
    }

    object Serializer : KSerializer<Opcode> {
        override val descriptor = PrimitiveSerialDescriptor("Opcode", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder) = throw UnsupportedOperationException()

        override fun serialize(encoder: Encoder, value: Opcode) = encoder.encodeString(value.toString())
    }
}
