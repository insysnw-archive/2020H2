package com.dekinci.uni.net.first

interface ByteWriter {
    fun receiveAck()
    fun sendAck()
    fun sendPing()
    fun writeMessage(data: List<ByteArray>)
}