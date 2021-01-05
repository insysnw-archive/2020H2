package com.handtruth.net.lab3.nrating.messages

import com.google.auto.service.AutoService
import com.handtruth.net.lab3.message.Message
import com.handtruth.net.lab3.message.MessageCompanion
import com.handtruth.net.lab3.nrating.options.ErrorMessageOption
import com.handtruth.net.lab3.nrating.options.OptionType
import com.handtruth.net.lab3.options.Option
import com.handtruth.net.lab3.options.toOptions
import com.handtruth.net.lab3.util.validate
import io.ktor.utils.io.core.*

data class DisconnectMessage(val message: String) : Message() {

    override val options = toOptions(
        ErrorMessageOption(message)
    )

    override fun writeBody(output: Output) {}

    @AutoService(MessageCompanion::class)
    companion object : MessageCompanion(0x33) {
        override fun read(input: ByteReadPacket, options: Map<Byte, Option>): Message {
            validate(options.containsKey(OptionType.ERROR_MESSAGE.code)) {
                "Disconnect Message must contain an ErrorMessage option"
            }
            return DisconnectMessage((options[OptionType.ERROR_MESSAGE.code] as ErrorMessageOption).name)
        }
    }
}
