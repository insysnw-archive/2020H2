package com.handtruth.net.lab3.nrating.options

import com.google.auto.service.AutoService
import com.handtruth.net.lab3.options.Option
import com.handtruth.net.lab3.options.OptionCompanion
import io.ktor.utils.io.core.*

data class AllowMultipleVotesOption(val maxVotes: Short) : Option() {
    override fun write(output: Output) {
        output.writeShort(maxVotes)
    }

    @AutoService(OptionCompanion::class)
    companion object : OptionCompanion(0x06) {
        override fun read(input: ByteReadPacket) = with(input) {
            AllowMultipleVotesOption(readShort())
        }
    }
}