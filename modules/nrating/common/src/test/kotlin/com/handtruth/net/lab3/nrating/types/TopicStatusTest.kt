package com.handtruth.net.lab3.nrating.types

import io.ktor.utils.io.core.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TopicStatusTest {
    @Test
    fun synchronousEncodingAndDecoding() {
        val topic = Topic(1, "What should I choose?")
        val rating = listOf(
            RatingItem(10, "Blue pill", 45, 0.45),
            RatingItem(11, "Red pill", 55, 0.55)
        )
        val topicStatus = TopicStatus(topic, true, rating)
        val bytes = buildPacket {
            writeTopicStatus(topicStatus)
        }
        assertEquals(topicStatus, bytes.readTopicStatus())
        bytes.close()
    }
}