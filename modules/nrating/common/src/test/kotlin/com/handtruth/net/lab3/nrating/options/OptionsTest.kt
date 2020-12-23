package com.handtruth.net.lab3.nrating.options

import com.handtruth.net.lab3.nrating.types.Topic
import com.handtruth.net.lab3.nrating.types.TopicStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class OptionsTest {
    @Test
    fun optionCodesTest() {
        assertEquals(0x01, TopicNameOption("123").id)
        assertEquals(0x02, AlternativeNameOption("123").id)
        assertEquals(0x03, ErrorMessageOption("123").id)
        assertEquals(0x04, TopicListOption(emptyList()).id)
        assertEquals(
            0x05, TopicStatusOption(
                TopicStatus(
                    Topic(42, "123"),
                    true,
                    emptyList()
                )
            ).id
        )
    }
}