package com.handtruth.net.lab3.nrating

import com.handtruth.net.lab3.util.ConcurrentMap


data class TopicInternal(
    val name: String,
    var isOpen: Boolean,
    val maxAlternatives: Int,
    val alternatives: ConcurrentMap<Int, AlternativeInternal> = ConcurrentMap(),
    var isClosed: Boolean = false
)

data class AlternativeInternal(
    val name: String,
    var votes: Int = 0
)
