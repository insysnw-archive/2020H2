package com.github.antoshka77.inet.dns

import com.github.antoshka77.inet.Int16
import com.github.antoshka77.inet.dns.records.Unsupported
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(RecordFactory.Serializer::class)
abstract class RecordFactory(val typeId: Int16, val typeName: String) {
    abstract fun decode(decoder: DNSDecoder): Record

    abstract fun serializer(): KSerializer<Record>

    companion object {
        val list: List<RecordFactory> by lazy { loadObjects() }
        private val mapById by lazy { list.associateBy { it.typeId } }
        private val mapByName by lazy { list.associateBy { it.typeName } }

        operator fun get(id: Int16): RecordFactory = mapById[id] ?: Unsupported
        operator fun get(name: String): RecordFactory = mapByName[name] ?: error("record type \"$name\" not found")
    }

    object Serializer : KSerializer<RecordFactory> {
        override val descriptor = PrimitiveSerialDescriptor("RecordFactory", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder) = get(decoder.decodeString())
        override fun serialize(encoder: Encoder, value: RecordFactory) = encoder.encodeString(value.typeName)
    }
}
