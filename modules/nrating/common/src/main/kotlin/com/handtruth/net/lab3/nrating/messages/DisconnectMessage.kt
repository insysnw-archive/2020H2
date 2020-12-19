package com.handtruth.net.lab3.nrating.messages

import com.google.auto.service.AutoService
import com.handtruth.net.lab3.message.Message
import com.handtruth.net.lab3.message.MessageCompanion
import com.handtruth.net.lab3.nrating.options.ErrorMessageOption
import com.handtruth.net.lab3.options.Option
import com.handtruth.net.lab3.util.validate
import io.ktor.utils.io.core.*

data class DisconnectMessage(val message: String) : Message() {

    override val options = listOf(
        ErrorMessageOption(message)
    )

    override fun writeBody(output: Output) {}

    @AutoService(MessageCompanion::class)
    companion object : MessageCompanion(0x33) {
        override fun read(input: ByteReadPacket, options: List<Option>): Message {
            validate(options.size == 1) { "Disconnect Message must contain a single ErrorMessage option" }
            validate(options[0].id == 0x03.toByte()) { "Disconnect Message must contain a single ErrorMessage option" }
            return DisconnectMessage((options[0] as ErrorMessageOption).name)
        }
    }
}
