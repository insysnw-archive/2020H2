package com.handtruth.net.lab3.types

import io.ktor.utils.io.core.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class VarListTest {
    @Test
    fun synchronousEncodingAndDecoding() {
        val bytes = buildPacket {
            writeVarList(listOf(1, 2, 3)) { writeInt(it) }
        }
        assertEquals(listOf(1, 2, 3), bytes.readVarList { bytes.readInt() })
        bytes.close()
    }
}