package com.dekinci.uni.net.first.nio

import com.dekinci.uni.net.first.ByteWriter
import com.dekinci.uni.net.first.MessageEncoder
import com.dekinci.uni.net.first.toIntBit
import java.lang.Integer.min
import java.nio.ByteBuffer

class MessageAccumulator {
    private val message = HashMap<String, String>()

    var type: Int? = null
        private set

    private var size: Int? = null

    private var strLength: Int? = null
    private var str: ByteArray? = null
    private var strIndex = 0
    private var strAck: Int = 0

    private var keyStr: String? = null
    private var valueStr: String? = null

    var ready = false
        private set

    val msg
        get(): Map<String, String> = message

    fun read(dataWriter: ByteWriter, buffer: ByteBuffer) {
        if (ready)
            return

        if (type == null && buffer.remaining() >= 1) {
            type = buffer.get().toIntBit()
        } else if (type == null) {
            return
        }

        when (type) {
            MessageEncoder.messageType -> Unit
            else -> {
                ready = true
                return
            }
        }

        if (size == null && buffer.remaining() >= 4) {
            size = buffer.int
        } else if (size == null) {
            return
        }

        while (buffer.remaining() >= 1) {
            if (keyStr == null && buffer.remaining() >= 1) {
                keyStr = readProperty(dataWriter, buffer)
                if (keyStr != null)
                    resetProperty()
            } else if (keyStr == null) {
                return
            }

            if (valueStr == null && buffer.remaining() >= 1) {
                valueStr = readProperty(dataWriter, buffer)
                if (valueStr != null)
                    resetProperty()
            }

            if (valueStr == null) {
                return
            }

            message[keyStr!!] = valueStr!!
            keyStr = null
            valueStr = null

            if (message.size == size) {
                ready = true
                return
            }
        }
    }

    private fun readProperty(dataWriter: ByteWriter, buffer: ByteBuffer): String? {
        if (strLength == null && buffer.remaining() >= 4) {
            strLength = buffer.int
            str = ByteArray(strLength!!)

            if (strLength!! >= MessageEncoder.chunkSize) {
                dataWriter.sendAck()
            }
        } else if (strLength == null) {
            return null
        }

        val strRemaining = strLength!! - strIndex
        if (strRemaining > 0 && buffer.remaining() >= 1) {
            val toRead = min(strRemaining, buffer.remaining())
            buffer.get(str, strIndex, toRead)
            strIndex += toRead

            if (strLength!! >= MessageEncoder.chunkSize && (strIndex - strAck == MessageEncoder.chunkSize || strIndex == strLength!!)) {
                dataWriter.sendAck()
                strAck = strIndex
            }
        } else if (strRemaining > 0) {
            return null
        }

        if (strLength!! == strIndex) {
            return String(str!!)
        } else {
            return null
        }
    }

    private fun resetProperty() {
        strLength = null
        str = null
        strIndex = 0
        strAck = 0
    }

    companion object {
        const val bufferSize = 700
        private val ackBuffer = ByteBuffer.wrap(byteArrayOf(MessageEncoder.ackType.toByte()))
    }
}