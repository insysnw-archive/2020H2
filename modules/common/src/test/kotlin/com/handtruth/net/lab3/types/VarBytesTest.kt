package com.handtruth.net.lab3.types

import io.ktor.utils.io.core.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class VarBytesTest {
    @Test
    fun synchronousEncodingAndDecoding() {
        val bytes = buildPacket {
            writeVarBytes(byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7))
        }
        Assertions.assertArrayEquals(byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7), bytes.readVarBytes())
        bytes.close()
    }
}