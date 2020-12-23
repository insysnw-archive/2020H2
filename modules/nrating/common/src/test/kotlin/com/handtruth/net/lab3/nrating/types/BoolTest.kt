package com.handtruth.net.lab3.nrating.types


import io.ktor.utils.io.core.*
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class BoolTest {
    @Test
    fun synchronousEncodingAndDecoding() {
        val bytes = buildPacket {
            writeBool(true)
        }
        assertTrue(bytes.readBool())
        bytes.close()
    }
}