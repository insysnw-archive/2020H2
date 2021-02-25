package com.dekinci.uni.net.second.tftpclient

import com.dekinci.uni.net.second.dnsserver.toShort
import com.dekinci.uni.net.second.tftpclient.Operations.DATA
import com.dekinci.uni.net.second.tftpclient.Operations.ERROR
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class Client(private val inetAddress: InetAddress, private val port: Int) {
    private var datagramSocket = DatagramSocket()
    private lateinit var inboundDatagramPacket: DatagramPacket

    fun read(fileName: String) {
        send(ReadRequest(fileName))
        receiveFile()?.writeTo(FileOutputStream(fileName))
    }

    fun write(fileName: String) {
        send(WriteRequest(fileName))

        if (checkAck(0)) {
            sendFile(File(fileName).readBytes())
        }
    }

    private fun receive(size: Int, port: Int): ByteArray {
        val buffer = ByteArray(size)
        inboundDatagramPacket = DatagramPacket(buffer, buffer.size, inetAddress, port)
        datagramSocket.receive(inboundDatagramPacket)

        return buffer
    }

    private fun send(message: Message) {
        val bytes = message.toByteArray()
        println("Sending ${bytes.toList()}")
        datagramSocket.send(DatagramPacket(bytes, bytes.size, inetAddress, port))
    }

    private fun receiveFile(): ByteArrayOutputStream? {
        val bos = ByteArrayOutputStream()
        var i: Short = 1

        do {
            val buffer = receive(PACKET_SIZE, datagramSocket.localPort)

            when (buffer[1]) {
                ERROR.code -> {
                    handleError(buffer)
                    return null
                }
                DATA.code -> {
                    val blockNumber = buffer.copyOfRange(2, 4).toShort()
                    if (i != blockNumber)
                        continue
                    println("Received chunk $blockNumber")
                    send(Acknowledgement(blockNumber))
                    val dos = DataOutputStream(bos)
                    dos.write(inboundDatagramPacket.data, 4, inboundDatagramPacket.length - 4)
                }
            }
            i++
        } while (inboundDatagramPacket.length >= PACKET_SIZE - 4)
        return bos
    }

    private fun sendFile(bytes: ByteArray) {
        for ((i, from) in (bytes.indices).zip(bytes.indices step PACKET_SIZE)) {
            do {
                val to = with(from + PACKET_SIZE) {
                    if (this < bytes.size) this else bytes.size
                }

                val blockNumber = (i + 1).toShort()
                send(Data(blockNumber, bytes.copyOfRange(from, to)))
                println("Send chunk $blockNumber")
            } while (!checkAck(blockNumber))
        }
    }

    private fun checkAck(expected: Short): Boolean {
        val buffer = receive(4, port)

        if (buffer[1] == ERROR.code)
            handleError(buffer)

        val actual = buffer.copyOfRange(0, 4)
        val expectedArray = Acknowledgement(expected).toByteArray()
        println(actual.toList())
        println(expectedArray.toList())
        println()
        return actual.contentEquals(expectedArray)
    }

    private fun handleError(bufferByteArray: ByteArray) {
        val errorCode = bufferByteArray[3]
        val errorText = String(bufferByteArray, 4, inboundDatagramPacket.length - 4)
        throw IllegalStateException("Error $errorCode $errorText")
    }

    companion object {
        const val PACKET_SIZE = 516
    }
}

