package com.handtruth.net.lab3.nrating

import com.handtruth.net.lab3.util.ConcurrentMap


data class TopicInternal(
    val name: String,
    var isOpen: Boolean,
    val maxAlternatives: Int,
    val maxVotes: Short = 1,
    val alternatives: ConcurrentMap<Int, AlternativeInternal> = ConcurrentMap()
)

data class AlternativeInternal(
    val name: String,
    var votes: Int = 0
)
