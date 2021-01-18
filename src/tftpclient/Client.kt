package tftpclient

import io.ktor.utils.io.core.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import tftpclient.Operations.*
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.text.String

@ExperimentalIoApi
class Client(addr: String, private val port: Int) {
    private var datagramSocket = DatagramSocket()
    private var inetAddress = InetAddress.getByName(addr)
    private lateinit var inboundDatagramPacket: DatagramPacket

    enum class InputCommands(val command: String, val code: Operations) {
        Read("read", RRQ),
        Write("write", WRQ)
    }

    init {
        val commandsList = InputCommands.values().map { it.command }
        while (true) {
            print("Введите команду $commandsList и имя файла: ")
            val userInput = readLine()?.split(" ") ?: break
            if (userInput.size < 2)
                println("Требуется как минимум 2 параметра")

            val command = InputCommands.values().find { it.command == userInput[0] }
            if (command == null) {
                println("Допустимые команды: $commandsList")
                continue
            }
            val fileName = userInput[1]

            when (command.code) {
                RRQ -> {
                    send(ReadRequest(fileName, DEFAULT_MODE))
                    receiveFile()?.writeTo(FileOutputStream(fileName))
                }
                WRQ -> {
                    send(WriteRequest(fileName, DEFAULT_MODE))

                    if (checkAcknowledgement(0)) {
                        sendFile(File(fileName).readBytes())
                    }
                }
                else -> break
            }
        }
    }

    private fun receive(size: Int = PACKET_SIZE, port: Int = datagramSocket.localPort): ByteArray {
        val buffer = ByteArray(size)
        inboundDatagramPacket = DatagramPacket(buffer, buffer.size, inetAddress, port)
        datagramSocket.receive(inboundDatagramPacket)

        return buffer
    }

    private fun send(message: Message) {
        val bytes = message.toByteArray()
        datagramSocket.send(DatagramPacket(bytes, bytes.size, inetAddress, port))
    }

    private fun receiveFile(): ByteArrayOutputStream? {
        val bos = ByteArrayOutputStream()
        var i: Short = 1

        do {
            val buffer = receive()

            when (buffer[1]) {
                ERROR.code -> {
                    printError(buffer)
                    return null
                }
                DATA.code -> {
                    val blockNumber = buffer.copyOfRange(2, 4).toShort()
                    if (i != blockNumber)
                        continue
                    println("Получен блок $blockNumber")
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
                println("Отправлен блок $blockNumber")
            } while (!checkAcknowledgement(blockNumber))
        }
    }

    private fun checkAcknowledgement(expected: Short): Boolean {
        val buffer = receive(4, port)

        if (buffer[1] == ERROR.code)
            printError(buffer)

        return buffer.copyOfRange(0, 4).contentEquals(Acknowledgement(expected).toByteArray())
    }

    private fun printError(bufferByteArray: ByteArray) {
        val errorCode = bufferByteArray[3]
        val errorText = String(bufferByteArray, 4, inboundDatagramPacket.length - 4)
        println("Ошибка: $errorCode $errorText")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val parser = ArgParser("tftp-client")
            val address by parser.option(
                    type = ArgType.String,
                    shortName = "a",
                    description = "хост"
            ).default(DEFAULT_ADDRESS)
            val port by parser.option(
                    type = ArgType.Int,
                    shortName = "p",
                    description = "порт"
            ).default(DEFAULT_PORT)
            parser.parse(args)

            Client(address, port)
        }
    }
}