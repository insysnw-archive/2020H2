package com.handtruth.net.lab3.nrating.options

import com.google.auto.service.AutoService
import com.handtruth.net.lab3.nrating.types.TopicStatus
import com.handtruth.net.lab3.nrating.types.readTopicStatus
import com.handtruth.net.lab3.nrating.types.writeTopicStatus
import com.handtruth.net.lab3.options.Option
import com.handtruth.net.lab3.options.OptionCompanion
import io.ktor.utils.io.core.*

data class TopicStatusOption(val topicStatus: TopicStatus) : Option() {
    override fun write(output: Output) {
        output.writeTopicStatus(topicStatus)
    }

    @AutoService(OptionCompanion::class)
    companion object : OptionCompanion(OptionType.TOPIC_STATUS.code) {
        override fun read(input: ByteReadPacket) = with(input) {
            TopicStatusOption(readTopicStatus())
        }
    }
}