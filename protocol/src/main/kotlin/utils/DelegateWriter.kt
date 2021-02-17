package utils

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class DelegateWriter(
    private val writer: (ByteArray) -> Unit
) : ByteWriter {
    private val ackQueue = LinkedBlockingQueue<Boolean>()
    private val lock = ReentrantLock()

    override fun receiveAck() {
        ackQueue.add(true)
    }

    override fun sendAck() {
        lock.withLock {
            writer(byteArrayOf(MessageEncoder.ackType.toByte()))
        }
    }

    override fun sendPing() {
        lock.withLock {
            writer(byteArrayOf(MessageEncoder.pingType.toByte()))
        }
    }

    override fun writeMessage(data: List<ByteArray>) {
        lock.withLock {
            if (data.size == 1) {
                writer(data[0])
            } else {
                for (d in data) {
                    if (d.isEmpty() && !ackQueue.take())
                        error("no acknowledgement")
                    writer(d)
                }
            }
        }
    }
}