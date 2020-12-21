package com.handtruth.net.lab3.message

import com.google.auto.service.AutoService
import com.handtruth.net.lab3.options.Option
import com.handtruth.net.lab3.options.TestOption
import com.handtruth.net.lab3.options.toOptions
import com.handtruth.net.lab3.types.readVarInt
import com.handtruth.net.lab3.types.writeVarInt
import com.handtruth.net.lab3.util.MessageFormatException
import io.ktor.test.dispatcher.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

data class MessageWithOptions(val integer: Int, override val options: Map<Byte, Option>) : Message() {

    override fun writeBody(output: Output) {
        output.writeVarInt(integer)
    }

    @AutoService(MessageCompanion::class)
    companion object : MessageCompanion(12) {
        override fun read(input: ByteReadPacket, options: Map<Byte, Option>) = MessageWithOptions(input.readVarInt(), options)
    }
}

data class MessageWithoutOptions(val integer: Int) : Message() {

    override fun writeBody(output: Output) {
        output.writeVarInt(integer)
    }

    @AutoService(MessageCompanion::class)
    companion object : MessageCompanion(13) {
        override fun read(input: ByteReadPacket, options: Map<Byte, Option>) = MessageWithoutOptions(input.readVarInt())
    }
}

class MessageTest {

    @Test
    fun asynchronousEncodingDecoding() = testSuspend {
        assertEquals(listOf(MessageWithOptions, MessageWithoutOptions), allMessages.values.toList())

        val channel = ByteChannel()
        val options = toOptions(TestOption(42))

        channel.writeMessage(MessageWithOptions(24, options))
        channel.flush()

        val received = channel.readMessage()
        assertEquals(12, received.id)
        assertTrue(received.options.containsKey(TestOption.optionId))
        assertEquals(options, received.options)
        assertEquals(24, (received as MessageWithOptions).integer)

        assertEquals(TestOption(42),  received.getOption<TestOption>())

        channel.writeMessage(MessageWithoutOptions(333))
        channel.flush()

        val receivedSecond = channel.readMessage()
        assertEquals(13, receivedSecond.id)
        assertEquals(0, receivedSecond.options.size)
        assertEquals(333, (receivedSecond as MessageWithoutOptions).integer)
    }

    @Test
    fun noOptionTest() {
        val message = MessageWithoutOptions(23)
        val actual = assertFailsWith<MessageFormatException> {
            message.getOption<TestOption>()
        }.message
        assertEquals("no option TestOption in message", actual)
    }

    @Test
    fun transmitterTest() = testSuspend {
        val channel = ByteChannel()
        val (recv, send) = transmitter(channel, channel)
        launch {
            try {
                send.send(MessageWithoutOptions(-455))
            } finally {
                send.close()
            }
        }

        val task = async {
            try {
                recv.receive() as MessageWithoutOptions
            } finally {
                recv.cancel()
            }
        }

        assertEquals(MessageWithoutOptions(-455), task.await())
    }
}
