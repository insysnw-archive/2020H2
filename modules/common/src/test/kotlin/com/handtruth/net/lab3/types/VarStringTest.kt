package com.handtruth.net.lab3.types

import io.ktor.utils.io.core.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class VarStringTest {
    @Test
    fun synchronousEncodingAndDecoding() {
        val testString = "Тест!"  // length != size
        val bytes = buildPacket {
            writeVarString(testString)
        }
        assertEquals(testString, bytes.readVarString())
        bytes.close()
    }
}