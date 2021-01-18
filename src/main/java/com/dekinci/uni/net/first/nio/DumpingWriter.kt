package com.dekinci.uni.net.first.nio

import com.dekinci.uni.net.first.ByteWriter
import com.dekinci.uni.net.first.MessageEncoder
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class DumpingWriter() : ByteWriter {
    private val ackQueue = ConcurrentLinkedQueue<Boolean>()
    private val writeQueue = ConcurrentLinkedQueue<Queue<ByteArray>>()

    private var currentMessage: Queue<ByteArray> = LinkedList()
    private var currentChunk = ByteBuffer.allocate(0)
    private var ackRequired = false

    override fun receiveAck() {
        ackQueue.add(true)
    }

    override fun sendAck() {
        writeMessage(listOf(byteArrayOf(MessageEncoder.ackType.toByte())))
    }

    override fun sendPing() {
        writeMessage(listOf(byteArrayOf(MessageEncoder.pingType.toByte())))
    }

    override fun writeMessage(data: List<ByteArray>) {
        val q = LinkedList(data)
        writeQueue.add(q)
    }


    fun send(client: SocketChannel) {
        var hasMoreToWrite = true

        while (hasMoreToWrite) {
            if (ackRequired) {
                if (ackQueue.isNotEmpty() && ackQueue.poll())
                    ackRequired = false
                else
                    return
            }

            if (!currentChunk.hasRemaining()) {
                if (currentMessage.isEmpty()) {
                    if (writeQueue.isEmpty())
                        return

                    currentMessage = writeQueue.poll()
                }
                val nextPiece = currentMessage.poll()
                if (nextPiece.isEmpty()) {
                    ackRequired = true
                    return
                } else {
                    currentChunk = ByteBuffer.wrap(nextPiece)
                }
            }

            hasMoreToWrite = client.write(currentChunk) > 0
        }
    }
}