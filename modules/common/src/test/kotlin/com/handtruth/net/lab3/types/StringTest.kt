package com.handtruth.net.lab3.types

import com.handtruth.net.lab3.util.MessageFormatException
import io.ktor.test.dispatcher.*
import io.ktor.utils.io.core.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StringTest {
    @Test
    fun synchronousEncodingAndDecoding() {
        val testString = "test!"
        val bytes = buildPacket {
            writeString(testString, 8)
        }
        assertEquals(8, bytes.remaining)
        assertEquals(testString, bytes.readString(8))
        bytes.close()
    }

    @Test
    fun errors() = testSuspend {
        val message = assertFailsWith<MessageFormatException> {
            buildPacket {
                writeString("Тест!", 8) // Кириллица кодируется 2 байтами на символ
            }
        }.message
        assertEquals("String is too big for given size", message)
    }
}