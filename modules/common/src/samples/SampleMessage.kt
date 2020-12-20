package com.handtruth.net.lab3.sample

import com.google.auto.service.AutoService
import com.handtruth.net.lab3.options.*
import com.handtruth.net.lab3.types.*
import io.ktor.utils.io.core.*

fun sampleMessage() {
    // Пример использования классов Message и MessageCompanion
    data class SampleMessage(val integer: Int, override val options: Map<Byte, Option>) : Message() {

        override fun writeBody(output: Output) {
            output.writeVarInt(integer)
        }

        @AutoService(MessageCompanion::class)
        companion object : MessageCompanion(42) {
            override fun read(input: ByteReadPacket, options: Map<Byte, Option>) = SampleMessage(input.readVarInt(), options)
        }
    }
}