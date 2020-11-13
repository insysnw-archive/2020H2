package com.github.antoshka77.schat

import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.datetime.Instant

interface Transferable {
    suspend fun write(output: ByteWriteChannel)
}

suspend inline fun ByteWriteChannel.send(transferable: Transferable) {
    transferable.write(this)
    flush()
}

interface Receivable<out T> {
    suspend fun read(input: ByteReadChannel): T
}

data class Enter(val nick: String) : Transferable {
    override suspend fun write(output: ByteWriteChannel) {
        output.putString(nick)
    }

    companion object : Receivable<Enter> {
        override suspend fun read(input: ByteReadChannel) = Enter(input.getString())
    }
}

data class Say(val message: String) : Transferable {
    override suspend fun write(output: ByteWriteChannel) {
        output.putString(message)
    }

    companion object : Receivable<Say> {
        override suspend fun read(input: ByteReadChannel) = Say(input.getString())
    }
}

data class Message(val time: Instant, val nick: String, val message: String) : Transferable {
    override suspend fun write(output: ByteWriteChannel) {
        output.writeLong(time.epochSeconds)
        output.putString(nick)
        output.putString(message)
    }

    companion object : Receivable<Message> {
        override suspend fun read(input: ByteReadChannel) = with(input) {
            Message(Instant.fromEpochSeconds(readLong()), getString(), getString())
        }
    }
}

private suspend fun ByteWriteChannel.putString(string: String) {
    val encoded = string.encodeToByteArray()
    writeInt(encoded.size)
    writeFully(encoded)
}

private suspend fun ByteReadChannel.getString(): String {
    val size = readInt()
    val decoded = ByteArray(size)
    readFully(decoded)
    return String(decoded)
}
