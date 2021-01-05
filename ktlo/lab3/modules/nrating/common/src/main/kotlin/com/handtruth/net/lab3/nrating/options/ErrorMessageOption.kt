package com.handtruth.net.lab3.nrating.options

import com.google.auto.service.AutoService
import com.handtruth.net.lab3.options.Option
import com.handtruth.net.lab3.options.OptionCompanion
import com.handtruth.net.lab3.types.readVarString
import com.handtruth.net.lab3.types.writeVarString
import io.ktor.utils.io.core.*

data class ErrorMessageOption(val name: String) : Option() {
    override fun write(output: Output) {
        output.writeVarString(name)
    }

    @AutoService(OptionCompanion::class)
    companion object : OptionCompanion(OptionType.ERROR_MESSAGE.code) {
        override fun read(input: ByteReadPacket) = with(input) {
            ErrorMessageOption(readVarString())
        }
    }
}
