package com.github.antoshka77.inet.dns

import com.github.antoshka77.inet.Int16
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure

@Serializable(Question.Serializer::class)
data class Question(val name: String, val typeId: Int16, val rClass: RClass) {
    fun encode(encoder: DNSEncoder) {
        encoder.encodeName(name)
        encoder.encodeInt16(typeId)
        rClass.encode(encoder)
    }

    companion object {
        fun decode(decoder: DNSDecoder) = Question(decoder.decodeName(), decoder.decodeInt16(), RClass.decode(decoder))
    }

    object Serializer : KSerializer<Question> {
        override val descriptor = buildClassSerialDescriptor("Question") {
            element("name", String.serializer().descriptor)
            element("type", RecordFactory.serializer().descriptor)
            element("class", RClass.serializer().descriptor)
        }

        override fun deserialize(decoder: Decoder) = throw UnsupportedOperationException()

        override fun serialize(encoder: Encoder, value: Question) = encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.name)
            encodeSerializableElement(descriptor, 1, RecordFactory.serializer(), RecordFactory[value.typeId])
            encodeSerializableElement(descriptor, 2, RClass.serializer(), value.rClass)
        }
    }
}
