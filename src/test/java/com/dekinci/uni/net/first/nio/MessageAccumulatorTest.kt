package com.dekinci.uni.net.first.nio

import com.dekinci.uni.net.first.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

internal class MessageAccumulatorTest {

    @Test
    fun testAck() {
        val buffer = ByteBuffer.allocate(MessageAccumulator.bufferSize)
        buffer.put(byteArrayOf(MessageEncoder.ackType.toByte()))
        val channel = WritableByteChannelImpl()

        val builder = MessageAccumulator()
        buffer.flip()
        builder.read(channelWriter(channel), buffer)

        assertEquals(MessageEncoder.ackType, builder.type)
        assertTrue(builder.ready)
        assertEquals(0, channel.writes)
    }

    @Test
    fun testSingleMessage() {
        val buffer = ByteBuffer.allocate(MessageAccumulator.bufferSize)
        val channel = WritableByteChannelImpl()

        val data = definedSmallData()

        buffer.put(data.first)

        val builder = MessageAccumulator()
        buffer.flip()
        builder.read(channelWriter(channel), buffer)

        assertEquals(MessageEncoder.messageType, builder.type)
        assertTrue(builder.ready)
        assertEquals(data.second, builder.msg)
        assertEquals(0, channel.writes)
    }

    @Test
    fun testGradualMessage() {
        val buffer = ByteBuffer.allocate(MessageAccumulator.bufferSize)
        val data = definedSmallData()
        val channel = WritableByteChannelImpl()

        val builder = MessageAccumulator()
        var extras = ByteArray(0)
        for (byte in data.first) {
            buffer.put(extras)
            buffer.put(byte)
            buffer.flip()
            builder.read(channelWriter(channel), buffer)
            extras = ByteArray(buffer.remaining())
            buffer.get(extras)
            buffer.clear()
            assertTrue(extras.size < 4)
        }

        assertEquals(MessageEncoder.messageType, builder.type)
        assertTrue(builder.ready)
        assertEquals(data.second, builder.msg)
        assertEquals(0, channel.writes)
    }

    @Test
    fun testGradualBigMessage() {
        val buffer = ByteBuffer.allocate(MessageAccumulator.bufferSize)
        val data = definedHugeData()
        val channel = WritableByteChannelImpl()

        val builder = MessageAccumulator()
        var extras = ByteArray(0)
        for (byte in data.first) {
            buffer.put(extras)
            buffer.put(byte)
            buffer.flip()
            builder.read(channelWriter(channel), buffer)
            extras = ByteArray(buffer.remaining())
            buffer.get(extras)
            buffer.clear()
            assertTrue(extras.size < 4)
        }

        assertEquals(MessageEncoder.messageType, builder.type)
        assertTrue(builder.ready)
        assertEquals(data.second, builder.msg)
        assertEquals(6, channel.writes)
    }

    @Test
    fun testEncodedSmall() {
        val buffer = ByteBuffer.allocate(MessageAccumulator.bufferSize)
        val map = definedSmallData().second
        val data = encodedStream(map)
        val channel = WritableByteChannelImpl()

        val builder = MessageAccumulator()
        var extras = ByteArray(0)
        for (byte in data) {
            buffer.put(extras)
            buffer.put(byte)
            buffer.flip()
            builder.read(channelWriter(channel), buffer)
            extras = ByteArray(buffer.remaining())
            buffer.get(extras)
            buffer.clear()
            assertTrue(extras.size < 4)
        }

        assertEquals(MessageEncoder.messageType, builder.type)
        assertTrue(builder.ready)
        assertEquals(map, builder.msg)
        assertEquals(0, channel.writes)
    }

    @Test
    fun testEncodedChunked() {
        val buffer = ByteBuffer.allocate(MessageAccumulator.bufferSize)
        val map = definedHugeData().second
        val data = encodedStream(map)
        val channel = WritableByteChannelImpl()

        val builder = MessageAccumulator()
        var extras = ByteArray(0)
        for (byte in data) {
            buffer.put(extras)
            buffer.put(byte)
            buffer.flip()
            builder.read(channelWriter(channel), buffer)
            extras = ByteArray(buffer.remaining())
            buffer.get(extras)
            buffer.clear()
            assertTrue(extras.size < 4)
        }

        assertEquals(MessageEncoder.messageType, builder.type)
        assertTrue(builder.ready)
        assertEquals(map, builder.msg)
        assertEquals(6, channel.writes)
    }

    @Test
    fun testChunkedMessage() {
        val buffer = ByteBuffer.allocate(MessageAccumulator.bufferSize)

        val pair1 = "key1" to "value1".repeat(100)
        val pair2 = "key2" to "value2".repeat(100)

        val key1 = pair1.first.toByteArray(Charsets.UTF_8)
        val value1 = pair1.second.toByteArray(Charsets.UTF_8)
        val key2 = pair2.first.toByteArray(Charsets.UTF_8)
        val value2 = pair2.second.toByteArray(Charsets.UTF_8)
        val channel = WritableByteChannelImpl()

        val builder = MessageAccumulator()
        var extras = ByteArray(0)

        buffer.put(extras)
        buffer.put(byteArrayOf(MessageEncoder.messageType.toByte()) +
                encodeInt(2) +
                encodeInt(key1.size) +
                key1 +
                encodeInt(value1.size) +
                value1.slice(0 until MessageEncoder.chunkSize)
        )
        buffer.flip()
        builder.read(channelWriter(channel), buffer)
        extras = ByteArray(buffer.remaining())
        buffer.get(extras)
        buffer.clear()
        assertTrue(extras.size < 4)
        assertEquals(2, channel.writes)

        buffer.put(extras)
        buffer.put(byteArrayOf() + value1.slice(MessageEncoder.chunkSize until value1.size))
        buffer.flip()
        builder.read(channelWriter(channel), buffer)
        extras = ByteArray(buffer.remaining())
        buffer.get(extras)
        buffer.clear()
        assertTrue(extras.size < 4)
        assertEquals(3, channel.writes)

        buffer.put(extras)
        buffer.put(encodeInt(key2.size) + key2 +
                encodeInt(value2.size) + value2.slice(0 until MessageEncoder.chunkSize))
        buffer.flip()
        builder.read(channelWriter(channel), buffer)
        extras = ByteArray(buffer.remaining())
        buffer.get(extras)
        buffer.clear()
        assertTrue(extras.size < 4)
        assertEquals(5, channel.writes)

        buffer.put(extras)
        buffer.put(byteArrayOf() + value2.slice(MessageEncoder.chunkSize until value1.size))
        buffer.flip()
        builder.read(channelWriter(channel), buffer)
        extras = ByteArray(buffer.remaining())
        buffer.get(extras)
        buffer.clear()
        assertTrue(extras.size < 4)
        assertEquals(6, channel.writes)

        assertEquals(MessageEncoder.messageType, builder.type)
        assertTrue(builder.ready)
        assertEquals(hashMapOf(pair1, pair2), builder.msg)
    }
}