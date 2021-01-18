package tftpclient

import io.ktor.utils.io.bits.*
import io.ktor.utils.io.core.*
import kotlin.text.toByteArray

enum class Operations(val code: Byte) {
    RRQ(1), WRQ(2), DATA(3), ACK(4), ERROR(5)
}

sealed class Message(val code: Byte) {
    abstract fun toByteArray(): ByteArray
}

class ReadRequest(var fileName: String, var mode: String) : Message(Operations.RRQ.code) {
    override fun toByteArray() = byteArrayOf(0, code, *fileName.toByteArray(), 0, *mode.toByteArray(), 0)
}

class WriteRequest(var fileName: String, var mode: String) : Message(Operations.WRQ.code) {
    override fun toByteArray() = byteArrayOf(0, code, *fileName.toByteArray(), 0, *mode.toByteArray(), 0)
}
class Data(var blockNumber: Short, var data: ByteArray) : Message(Operations.DATA.code) {
    @ExperimentalIoApi
    override fun toByteArray() = byteArrayOf(0, code, *blockNumber.toByteArray(), *data)
}
class Acknowledgement(var blockNumber: Short) : Message (Operations.ACK.code) {

    @ExperimentalIoApi
    override fun toByteArray() = byteArrayOf(0, code, *blockNumber.toByteArray())
}
class Error(var errorCode: Short, var message: String) : Message(Operations.ERROR.code) {
    @ExperimentalIoApi
    override fun toByteArray() = byteArrayOf(0, code, *errorCode.toByteArray(), *message.toByteArray(), 0)
}

@ExperimentalIoApi
private fun Short.toByteArray() = byteArrayOf(this.highByte, this.lowByte)

fun ByteArray.toShort() = (this[0].toInt() and 0xFF shl 8 or (this[1].toInt() and 0xFF)).toShort()