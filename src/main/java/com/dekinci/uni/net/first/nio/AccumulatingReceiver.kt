package com.dekinci.uni.net.first.nio

import com.dekinci.uni.net.first.MessageEncoder
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue

class AccumulatingReceiver {
    private val messageQueue = LinkedBlockingQueue<Map<String, String>>()
    private var accumulatorInProgress: MessageAccumulator = MessageAccumulator()
    private var remainingBytes: ByteArray = ByteArray(0)

    fun dumpRemaining(buffer: ByteBuffer) {
        if (remainingBytes.isNotEmpty()) {
            buffer.put(remainingBytes)
            remainingBytes = ByteArray(0)
        }
    }

    fun update(buffer: ByteBuffer, dataWriter: DumpingWriter) {
        while (true) {
            accumulatorInProgress.read(dataWriter, buffer)
            if (accumulatorInProgress.ready) {
                when (accumulatorInProgress.type) {
                    MessageEncoder.ackType -> dataWriter.receiveAck()
                    MessageEncoder.messageType -> messageQueue.add(accumulatorInProgress.msg)
                }
                accumulatorInProgress = MessageAccumulator()
            } else {
                remainingBytes = ByteArray(buffer.remaining())
                buffer.get(remainingBytes)
                break
            }
        }
    }

    fun findMessage(): Map<String, String>? {
        return messageQueue.poll()
    }
}