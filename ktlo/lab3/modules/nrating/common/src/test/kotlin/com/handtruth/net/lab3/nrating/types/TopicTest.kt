package com.handtruth.net.lab3.nrating.types

import io.ktor.utils.io.core.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TopicTest {
    @Test
    fun synchronousEncodingAndDecoding() {
        val topic = Topic(418, "I'm a teapot")
        val bytes = buildPacket {
            writeTopic(topic)
        }
        assertEquals(topic, bytes.readTopic())
        bytes.close()
    }
}