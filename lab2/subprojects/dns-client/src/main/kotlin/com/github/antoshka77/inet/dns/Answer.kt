package com.github.antoshka77.inet.dns

import com.github.antoshka77.inet.Int32
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

@Serializable(Answer.Serializer::class)
data class Answer(
    val name: String,
    @SerialName("class")
    val rClass: RClass,
    val ttl: Int32,
    @Contextual
    val record: Record
) {
    companion object {
        fun decode(decoder: DNSDecoder): Answer {
            val name = decoder.decodeName()
            val typeId = decoder.decodeInt16()
            val factory = RecordFactory[typeId]
            val rClass = RClass.decode(decoder)
            val ttl = decoder.decodeInt32()
            val size = decoder.decodeInt16().toInt()
            val recordDecoder = decoder.decodeRecordData(size)
            val record = factory.decode(recordDecoder)
            return Answer(name, rClass, ttl, record)
        }
    }

    object Serializer : KSerializer<Answer> {
        override val descriptor = buildClassSerialDescriptor("Answer") {
            element("name", String.serializer().descriptor)
            element("type", RecordFactory.serializer().descriptor)
            element("class", RClass.serializer().descriptor)
            element("ttl", Int32.serializer().descriptor)
            element("data", buildSerialDescriptor("Record", SerialKind.CONTEXTUAL))
        }

        override fun deserialize(decoder: Decoder): Answer {
            decoder.decodeStructure(descriptor) {
                val name = decodeStringElement(descriptor, 0)
                val factory = decodeSerializableElement(descriptor, 1, RecordFactory.serializer())
                val rClass = decodeSerializableElement(descriptor, 2, RClass.serializer())
                val ttl = decodeIntElement(descriptor, 3)
                val record = decodeSerializableElement(descriptor, 4, factory.serializer())
                return Answer(name, rClass, ttl, record)
            }
        }

        override fun serialize(encoder: Encoder, value: Answer) {
            encoder.encodeStructure(descriptor) {
                encodeStringElement(descriptor, 0, value.name)
                encodeSerializableElement(descriptor, 1, RecordFactory.serializer(), value.record.type)
                encodeSerializableElement(descriptor, 2, RClass.serializer(), value.rClass)
                encodeIntElement(descriptor, 3, value.ttl)
                encodeSerializableElement(descriptor, 4, value.record.type.serializer(), value.record)
            }
        }
    }
}
