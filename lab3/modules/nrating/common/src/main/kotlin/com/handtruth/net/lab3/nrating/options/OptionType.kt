package com.handtruth.net.lab3.nrating.options

enum class OptionType(val code: Byte) {
    TOPIC_NAME(0x01),
    ALTERNATIVE_NAME(0x02),
    ERROR_MESSAGE(0x03),
    TOPIC_LIST(0x04),
    TOPIC_STATUS(0x05),
    ALLOW_MULTIPLE_VOTES(0x06)
}