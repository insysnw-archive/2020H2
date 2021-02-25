package com.dekinci.uni.net.second.tftpclient

import com.dekinci.uni.net.second.dnsserver.toByteArray

enum class Operations(val code: Byte) {
    RRQ(1),
    WRQ(2),
    DATA(3),
    ACK(4),
    ERROR(5)
}

sealed class Message(val code: Byte) {
    abstract fun toByteArray(): ByteArray
}

class ReadRequest(var fileName: String, var mode: String = "octet") : Message(Operations.RRQ.code) {
    override fun toByteArray() = byteArrayOf(0, code, *fileName.toByteArray(), 0, *mode.toByteArray(), 0)
}

class WriteRequest(var fileName: String, var mode: String = "octet") : Message(Operations.WRQ.code) {
    override fun toByteArray() = byteArrayOf(0, code, *fileName.toByteArray(), 0, *mode.toByteArray(), 0)
}

class Data(var blockNumber: Short, var data: ByteArray) : Message(Operations.DATA.code) {
    override fun toByteArray() = byteArrayOf(0, code, *blockNumber.toByteArray(), *data)
}

class Acknowledgement(var blockNumber: Short) : Message(Operations.ACK.code) {
    override fun toByteArray() = byteArrayOf(0, code, *blockNumber.toByteArray())
}
