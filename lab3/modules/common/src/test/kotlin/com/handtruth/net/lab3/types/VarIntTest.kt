package com.handtruth.net.lab3.types

import com.handtruth.net.lab3.util.MessageFormatException
import io.ktor.test.dispatcher.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VarIntTest {

    data class Case(val value: Int, val coded: ByteArray)

    @Test
    fun synchronousEncodingAndDecoding() {
        listOf(
            Case(0, byteArrayOf(0)),
            Case(-1, byteArrayOf(-1, -1, -1, -1, 15)),
            Case(687, byteArrayOf(-81, 5)),
            Case(48677, byteArrayOf(-91, -4, 2)),
            Case(23, byteArrayOf(23))
        ).forEachIndexed { i, (value, coded) ->
            val mark = "#$i"
            val bytes = buildPacket {
                writeVarInt(value)
            }
            assertEquals(coded.size.toLong(), bytes.remaining, mark)
            val intermediate = bytes.copy().use { packet -> ByteArray(coded.size) { packet.readByte() } }
            assertEquals(coded.toList(), intermediate.toList(), mark)
            assertEquals(value, bytes.readVarInt(), mark)
            bytes.close()
        }
    }

    @Test
    fun asynchronousEncodingDecoding() = testSuspend {
        val channel = ByteChannel()
        channel.writeVarInt(235911)
        channel.flush()
        val actual = channel.readVarInt()
        assertEquals(235911, actual)
    }

    @Test
    fun errors() {
        val src = buildPacket {
            repeat(5) {
                writeByte(-1)
            }
        }
        val message = assertFailsWith<MessageFormatException> {
            src.readVarInt()
        }.message
        assertEquals("VarInt is too big", message)
    }
}
