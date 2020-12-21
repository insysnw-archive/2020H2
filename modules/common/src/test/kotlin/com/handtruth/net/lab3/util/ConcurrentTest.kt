package com.handtruth.net.lab3.util

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class ConcurrentTest {
    @Test
    fun cowList() {
        val list = ConcurrentList<String>()
        assertEquals(emptyList<String>(), list)
        runBlocking {
            list.add("Hello!")
        }
        assertEquals(listOf("Hello!"), list)
    }
}
