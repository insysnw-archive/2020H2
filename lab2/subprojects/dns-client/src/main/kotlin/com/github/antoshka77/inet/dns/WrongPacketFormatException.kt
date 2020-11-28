package com.github.antoshka77.inet.dns

class WrongPacketFormatException(message: String) : RuntimeException(message)

fun formatError(message: String): Nothing = throw WrongPacketFormatException(message)
