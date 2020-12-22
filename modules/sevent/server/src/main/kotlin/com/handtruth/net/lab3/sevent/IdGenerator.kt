package com.handtruth.net.lab3.sevent

import java.util.concurrent.atomic.AtomicInteger

class IdGenerator(initial: Int = 0) {
    private val ids = AtomicInteger(initial)

    fun next() = ids.getAndIncrement()
}
