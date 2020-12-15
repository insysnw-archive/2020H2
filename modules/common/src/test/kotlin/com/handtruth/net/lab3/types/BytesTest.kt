package com.handtruth.net.lab3.types

import com.handtruth.net.lab3.util.MessageFormatException
import io.ktor.test.dispatcher.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BytesTest {

    @Test
    fun synchronousEncodingAndDecoding() {
        val bytes = buildPacket {
            writeBytes(byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7))
        }
        assertArrayEquals(byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7), bytes.readBytes(8))
        bytes.close()
    }

    @Test
    fun errors() {
        val src = buildPacket {
            writeByte(-1)
        }

        val message = assertFailsWith<MessageFormatException> {
            src.readBytes(-333)
        }.message
        assertEquals("Size value is negative", message)
        src.close()
    }
}