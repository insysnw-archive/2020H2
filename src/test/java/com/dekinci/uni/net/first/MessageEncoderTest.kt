package com.dekinci.uni.net.first

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class MessageEncoderTest {

    @Test
    fun encodeSmallTest() {
        val pair1 = "key1" to "value1"
        val pair2 = "key2" to "value2"

        val key1 = pair1.first.toByteArray(Charsets.UTF_8)
        val value1 = pair1.second.toByteArray(Charsets.UTF_8)
        val key2 = pair2.first.toByteArray(Charsets.UTF_8)
        val value2 = pair2.second.toByteArray(Charsets.UTF_8)
        val data = byteArrayOf(MessageEncoder.messageType.toByte()) +
                encodeInt(2) +
                encodeInt(key1.size) + key1 +
                encodeInt(value1.size) + value1 +
                encodeInt(key2.size) + key2 +
                encodeInt(value2.size) + value2

        val encoded = MessageEncoder.encode(mapOf(pair1, pair2))
        assertArrayEquals(data, encoded[0])
    }

    @Test
    fun encodeChunksTest() {
        val pair1 = "key1" to "value1".repeat(100)
        val pair2 = "key2" to "value2".repeat(100)

        val key1 = pair1.first.toByteArray(Charsets.UTF_8)
        val value1 = pair1.second.toByteArray(Charsets.UTF_8)
        val key2 = pair2.first.toByteArray(Charsets.UTF_8)
        val value2 = pair2.second.toByteArray(Charsets.UTF_8)
        val data = listOf(
                byteArrayOf(MessageEncoder.messageType.toByte()) + encodeInt(2) + encodeInt(key1.size) + key1 + encodeInt(value1.size),
                byteArrayOf(),
                byteArrayOf() + value1.slice(0 until MessageEncoder.chunkSize),
                byteArrayOf(),
                byteArrayOf() + value1.slice(MessageEncoder.chunkSize until value1.size),
                byteArrayOf(),
                encodeInt(key2.size) + key2 + encodeInt(value2.size),
                byteArrayOf(),
                byteArrayOf() + value2.slice(0 until MessageEncoder.chunkSize),
                byteArrayOf(),
                byteArrayOf() + value2.slice(MessageEncoder.chunkSize until value1.size),
                byteArrayOf()
        )

        val encoded = MessageEncoder.encode(mapOf(pair1, pair2))
        assertEquals(data.size, encoded.size)
        for (i in data.indices) {
            assertArrayEquals(data[i], encoded[i])
        }
    }
}