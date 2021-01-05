package com.handtruth.net.lab3.types

import io.ktor.utils.io.core.*
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TimeTest {
    @Test
    fun synchronousEncodingAndDecoding() {
        val time = Clock.System.now()
        val bytes = buildPacket {
            writeTime(time)
            writeTime(time)
        }
        assertEquals(time.epochSeconds, bytes.readTimeInstant().epochSeconds)
        assertEquals(time.epochSeconds, bytes.readTime())
        bytes.close()
    }
}