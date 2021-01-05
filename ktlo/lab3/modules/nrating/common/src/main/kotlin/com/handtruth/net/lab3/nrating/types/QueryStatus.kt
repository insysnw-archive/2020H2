package com.handtruth.net.lab3.nrating.types

import com.handtruth.net.lab3.util.MessageFormatException

enum class QueryStatus(val code: Byte) {
    OK(0x01),
    FAILED(0x02)
}

fun Byte.toQueryStatus(): QueryStatus = when (this.toInt()) {
    0x01 -> QueryStatus.OK
    0x02 -> QueryStatus.FAILED
    else -> throw MessageFormatException("Unknown query status.")
}