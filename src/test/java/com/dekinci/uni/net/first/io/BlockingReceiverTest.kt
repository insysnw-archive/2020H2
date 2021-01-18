package com.dekinci.uni.net.first.io

import com.dekinci.uni.net.first.MessageEncoder
import com.dekinci.uni.net.first.definedHugeData
import com.dekinci.uni.net.first.definedSmallData
import com.dekinci.uni.net.first.encodedStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

internal class BlockingReceiverTest {

    @Test
    fun testSmallData() {
        val map = definedSmallData().second
        val data = encodedStream(map)

        val inputStream = ByteArrayInputStream(data)

        val receiver = BlockingReceiver(inputStream, DelegateWriter { })
        receiver.blockingUpdate()
        val message = receiver.findMessage()

        assertEquals(map, message)
    }

    @Test
    fun testChunkedData() {
        val map = definedHugeData().second
        val data = encodedStream(map)

        val inputStream = ByteArrayInputStream(data)

        val receiver = BlockingReceiver(inputStream, DelegateWriter { })
        receiver.blockingUpdate()
        val message = receiver.findMessage()

        assertEquals(map, message)
    }

    @Test
    fun testChunkedRealData() {
        val map = definedHugeData().second
        val data = MessageEncoder.encode(map).filter { it.isNotEmpty() }
        val ptr = data.iterator()

        val toWrite = PipedOutputStream()
        val inputStream = PipedInputStream(toWrite)
        toWrite.write(if (ptr.hasNext()) ptr.next() else byteArrayOf())
        val receiver = BlockingReceiver(inputStream, DelegateWriter { toWrite.write(if (ptr.hasNext()) ptr.next() else byteArrayOf()) })
        receiver.blockingUpdate()
        val message = receiver.findMessage()

        assertEquals(map, message)
    }
}