package com.handtruth.net.lab3.nrating.options

import com.google.auto.service.AutoService
import com.handtruth.net.lab3.nrating.types.Topic
import com.handtruth.net.lab3.nrating.types.readTopic
import com.handtruth.net.lab3.nrating.types.writeTopic
import com.handtruth.net.lab3.options.Option
import com.handtruth.net.lab3.options.OptionCompanion
import com.handtruth.net.lab3.types.readVarList
import com.handtruth.net.lab3.types.writeVarList
import io.ktor.utils.io.core.*

data class TopicListOption(val topics: List<Topic>) : Option() {
    override fun write(output: Output) {
        output.writeVarList(topics) { output.writeTopic(it) }
    }

    @AutoService(OptionCompanion::class)
    companion object : OptionCompanion(0x04) {
        override fun read(input: ByteReadPacket) = with(input) {
            TopicListOption(readVarList { readTopic() })
        }
    }
}
