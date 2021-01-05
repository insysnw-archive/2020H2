package com.handtruth.net.lab3.nrating.types

import com.handtruth.net.lab3.util.MessageFormatException

enum class QueryMethod(val code: Byte) {
    GET(0x01),
    ADD(0x02),
    DEL(0x03),
    OPEN(0x04),
    CLOSE(0x05),
    VOTE(0x06)
}

fun Byte.toQueryMethod(): QueryMethod = when (this.toInt()) {
    0x01 -> QueryMethod.GET
    0x02 -> QueryMethod.ADD
    0x03 -> QueryMethod.DEL
    0x04 -> QueryMethod.OPEN
    0x05 -> QueryMethod.CLOSE
    0x06 -> QueryMethod.VOTE
    else -> throw MessageFormatException("Unknown query method.")
}