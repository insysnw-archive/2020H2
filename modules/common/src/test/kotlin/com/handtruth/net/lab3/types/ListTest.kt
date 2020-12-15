package com.handtruth.net.lab3.types

import com.handtruth.net.lab3.util.MessageFormatException
import io.ktor.utils.io.core.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ListTest {
    @Test
    fun synchronousEncodingAndDecoding() {
        val bytes = buildPacket {
            writeList(listOf(1, 2, 3)) { writeInt(it) }
        }
        assertEquals(listOf(1, 2, 3), bytes.readList(3) { bytes.readInt() })
        bytes.close()
    }

    @Test
    fun errors() {
        val src = buildPacket {
            writeList(listOf(1, 2, 3)) { writeInt(it) }
        }

        val message = assertFailsWith<MessageFormatException> {
            src.readList(-3) { src.readInt() }
        }.message
        assertEquals("Size value is negative", message)
        src.close()
    }
}