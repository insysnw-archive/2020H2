package com.github.antoshka77.inet.dns

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlin.reflect.full.companionObjectInstance

@Serializable
abstract class Record {
    @Transient val type: RecordFactory = this::class.companionObjectInstance as RecordFactory

    private companion object {
        val json = Json {}
    }

    val typeId get() = type.typeId

    final override fun toString() = "${type.typeName}\t${json.encodeToString(type.serializer(), this)}"
}
