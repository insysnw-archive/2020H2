package com.dekinci.uni.net.first.nio

import com.dekinci.uni.net.first.io.DelegateWriter
import java.nio.ByteBuffer
import java.nio.channels.WritableByteChannel

fun channelWriter(channel: WritableByteChannel) = DelegateWriter { channel.write(ByteBuffer.wrap(byteArrayOf())) }

class WritableByteChannelImpl : WritableByteChannel {
    var writes = 0
        private set

    override fun close() = Unit

    override fun isOpen() = true

    override fun write(src: ByteBuffer?): Int {
        writes++
        return 0
    }
}