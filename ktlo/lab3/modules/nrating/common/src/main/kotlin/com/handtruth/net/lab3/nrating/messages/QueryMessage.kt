package com.handtruth.net.lab3.nrating.messages

import com.google.auto.service.AutoService
import com.handtruth.net.lab3.message.Message
import com.handtruth.net.lab3.message.MessageCompanion
import com.handtruth.net.lab3.nrating.types.QueryMethod
import com.handtruth.net.lab3.nrating.types.toQueryMethod
import com.handtruth.net.lab3.options.Option
import com.handtruth.net.lab3.types.readVarInt
import com.handtruth.net.lab3.types.writeVarInt
import io.ktor.utils.io.core.*

data class QueryMessage(
    val method: QueryMethod,
    val topic: Int,
    val alternative: Int,
    override val options: Map<Byte, Option> = emptyMap()
) : Message() {

    override fun writeBody(output: Output) {
        output.writeByte(method.code)
        output.writeVarInt(topic)
        output.writeVarInt(alternative)
    }

    @AutoService(MessageCompanion::class)
    companion object : MessageCompanion(0x31) {
        override fun read(input: ByteReadPacket, options: Map<Byte, Option>): Message {
            val method = input.readByte().toQueryMethod()
            val topic = input.readVarInt()
            val alternative = input.readVarInt()
            return QueryMessage(method, topic, alternative, options)
        }
    }
}
