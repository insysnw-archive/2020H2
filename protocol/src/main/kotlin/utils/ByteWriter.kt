package utils

interface ByteWriter {
    fun receiveAck()
    fun sendAck()
    fun sendPing()
    fun writeMessage(data: List<ByteArray>)
}