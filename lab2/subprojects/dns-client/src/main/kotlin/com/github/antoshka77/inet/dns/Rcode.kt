package com.github.antoshka77.inet.dns

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(Rcode.Serializer::class)
sealed class Rcode(val id: Int) {
    object NoError : Rcode(0) {
        override fun toString() = "No error"
    }
    object FormatError : Rcode(1) {
        override fun toString() = "Format error"
    }
    object ServerFailure : Rcode(2) {
        override fun toString() = "Server failure"
    }
    object NameError : Rcode(3) {
        override fun toString() = "Name error"
    }
    object NotImplemented : Rcode(4) {
        override fun toString() = "Not implemented"
    }
    object Refused : Rcode(5) {
        override fun toString() = "Refused"
    }
    class Unknown(id: Int) : Rcode(id) {
        override fun toString() = "$id"
    }

    companion object {
        fun fromId(id: Int): Rcode {
            return when (id) {
                0 -> NoError
                1 -> FormatError
                2 -> ServerFailure
                3 -> NameError
                4 -> NotImplemented
                5 -> Refused
                else -> Unknown(id)
            }
        }
    }

    object Serializer : KSerializer<Rcode> {
        override val descriptor = PrimitiveSerialDescriptor("Rcode", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder) = throw UnsupportedOperationException()

        override fun serialize(encoder: Encoder, value: Rcode) = encoder.encodeString(value.toString())
    }
}
