package com.github.antoshka77.schat

private val nicks: List<String> by lazy {
    val stream = {}::class.java.classLoader.getResource("assets/names.txt")!!.openStream()
    val result = mutableListOf<String>()
    stream.bufferedReader().use { reader ->
        reader.forEachLine { line ->
            val sharp = line.indexOf('#')
            result += if (sharp == -1) {
                line
            } else {
                line.substring(0 until sharp)
            }.trim()
        }
    }
    result
}

internal fun randomNick() = nicks.random()
