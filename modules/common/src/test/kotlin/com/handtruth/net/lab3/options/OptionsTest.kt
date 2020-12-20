package com.handtruth.net.lab3.options

import com.google.auto.service.AutoService
import com.handtruth.net.lab3.types.readVarInt
import com.handtruth.net.lab3.types.writeVarInt
import io.ktor.utils.io.core.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

data class TestOption(val integer: Int) : Option() {
    override fun write(output: Output) {
        output.writeVarInt(integer)
    }

    @AutoService(OptionCompanion::class)
    companion object : OptionCompanion(23) {
        override fun read(input: ByteReadPacket) = TestOption(input.readVarInt())
    }
}

class BadOption : Option() {
    override fun write(output: Output) {}
}

class BadCompanion(optionId: Byte) : OptionCompanion(optionId) {
    override fun read(input: ByteReadPacket): Nothing = throw NotImplementedError()
}

class OptionsTest {
    @Test
    fun encodingDecoding() {
        assertEquals(listOf(TestOption), allOptions.values.toList())

        val options = listOf(TestOption(42))
        val packet = buildPacket {
            // Записать несуществующую опцию
            writeByte(127)
            writeByte(0)
            // Записать реальные опции
            writeOptions(options)
        }
        assertEquals(6, packet.remaining)
        val actual = packet.use { it.readOptions() }
        assertEquals(1, actual.size)
        assertEquals(TestOption(42), actual[TestOption.optionId])
        assertEquals(TestOption, actual[TestOption.optionId]?.companion)

    }

    @Test
    fun badOptions() {
        val message1 = assertFailsWith<IllegalStateException> {
            BadOption()
        }.message
        assertEquals("option has illegal factory companion", message1)

        val message2 = assertFailsWith<IllegalArgumentException> {
            BadCompanion(0)
        }.message
        assertEquals("option id #0 is used to mark the end of options", message2)

        val message3 = assertFailsWith<IllegalArgumentException> {
            BadCompanion(-13)
        }.message
        assertEquals("forbidden option id #-13", message3)
    }
}
