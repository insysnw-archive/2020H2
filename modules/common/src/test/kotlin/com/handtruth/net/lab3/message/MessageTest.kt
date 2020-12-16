package com.handtruth.net.lab3.message

import com.google.auto.service.AutoService
import com.handtruth.net.lab3.options.*
import com.handtruth.net.lab3.types.readVarInt
import com.handtruth.net.lab3.types.writeVarInt
import io.ktor.test.dispatcher.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

data class MessageWithOptions(val integer: Int, override val options: List<Option>) : Message() {

    override fun writeBody(output: Output) {
        output.writeVarInt(integer)
    }

    @AutoService(MessageCompanion::class)
    companion object : MessageCompanion(12) {
        override fun read(input: ByteReadPacket, options: List<Option>) = MessageWithOptions(input.readVarInt(), options)
    }
}

data class MessageWithoutOptions(val integer: Int) : Message() {

    override fun writeBody(output: Output) {
        output.writeVarInt(integer)
    }

    @AutoService(MessageCompanion::class)
    companion object : MessageCompanion(13) {
        override fun read(input: ByteReadPacket, options: List<Option>) = MessageWithoutOptions(input.readVarInt())
    }
}

class MessageTest {

    @Test
    fun asynchronousEncodingDecoding() = testSuspend {
        assertEquals(listOf(MessageWithOptions, MessageWithoutOptions), allMessages.values.toList())

        val channel = ByteChannel()
        val options = listOf(TestOption(42))

        channel.writeMessage(MessageWithOptions(24, options))
        channel.flush()

        val received = channel.readMessage()
        assertEquals(12, received.id)
        assertEquals(options, received.options)
        assertEquals(24, (received as MessageWithOptions).integer)

        channel.writeMessage(MessageWithoutOptions(333))
        channel.flush()

        val receivedSecond = channel.readMessage()
        assertEquals(13, receivedSecond.id)
        assertEquals(0, receivedSecond.options.size)
        assertEquals(333, (receivedSecond as MessageWithoutOptions).integer)
    }
}