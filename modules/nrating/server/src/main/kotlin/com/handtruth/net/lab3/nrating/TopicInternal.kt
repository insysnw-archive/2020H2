package com.handtruth.net.lab3.nrating

data class TopicInternal(
    val name: String,
    var isOpen: Boolean,
    val maxAlternatives: Short,
    val alternatives: MutableMap<Int, AlternativeInternal>
)

data class AlternativeInternal(
    val name: String,
    var votes: Int
)
