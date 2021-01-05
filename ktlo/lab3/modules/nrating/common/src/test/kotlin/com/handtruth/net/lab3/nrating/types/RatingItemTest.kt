package com.handtruth.net.lab3.nrating.types

import io.ktor.utils.io.core.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RatingItemTest {
    @Test
    fun synchronousEncodingAndDecoding() {
        val ratingItem = RatingItem(42, "That's the answer!", 100, 1.0)
        val bytes = buildPacket {
            writeRatingItem(ratingItem)
        }
        assertEquals(ratingItem, bytes.readRatingItem())
        bytes.close()
    }
}