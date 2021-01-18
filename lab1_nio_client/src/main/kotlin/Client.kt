import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import kotlin.concurrent.thread


object NIOClient {

    val mapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).registerModule(KotlinModule())

    var token: ByteArray = ByteArray(32)
    var isRunning = true
    lateinit var client: SocketChannel
    lateinit var selector: Selector

    @Throws(IOException::class, InterruptedException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val addr = InetSocketAddress("localhost", 1111)
        client = SocketChannel.open(addr)
        client.configureBlocking(false)
        selector = Selector.open() // selector is open here
        client.register(
            selector, SelectionKey.OP_CONNECT or
                    SelectionKey.OP_READ or SelectionKey.OP_WRITE
        )
        listenMessages()
        log("Connecting to Server on port 1111...")
        register()
    }

    private fun startMessaging() {
        log("OK. Start messaging.")
        thread {
            while (true) {
                val message = readLine()
                if (message == "exit") {
                    sendMessage(Message(MessageType.DISCONNECT, token, ""))
                    client.close()
                } else {
                    sendMessage(Message(MessageType.MESSAGE, token, message ?: ""))
                }
            }
        }
    }

    private fun listenMessages() {
        thread {
            while (isRunning) {
                selector.select()
                val keys = selector.selectedKeys().toMutableSet()
                val iterator = keys.iterator()
                while (iterator.hasNext()) {
                    val key = iterator.next()
                    if (key.isReadable) {
                        val socketChannel = key.channel() as SocketChannel
                        val buffer = ByteBuffer.allocate(4096)
                        val res = socketChannel.read(buffer)

                        if (res == -1 || res == 0) continue
                        formatMessage(buffer)
                        iterator.remove()
                    }
                }

            }
        }
    }

    private fun formatMessage(buffer: ByteBuffer) {
        val messageSize =
            ByteBuffer.wrap(buffer.array().toList().subList(0, 2).toByteArray()).order(ByteOrder.LITTLE_ENDIAN).short
                .toInt()
        //println("message size: $messageSize")
        val message = String(buffer.array().toList().subList(2, 2 + messageSize).toByteArray())
        //log("message received: ${buffer.array().toList()}")
        val argsMap: Map<String, String> = mapper.readValue(message)

        if (argsMap["token"] != null) {
            val tokenDataClass: Token = mapper.readValue(message)
            token = tokenDataClass.token
            startMessaging()
            //println("token is: ${token.toList()}\nsize is: ${token.size}")
        }
        if (argsMap["message"] != null) {
            log(argsMap["message"] ?: "")
        }
        if (argsMap["error"] != null) {
            when (argsMap["error"]) {
                "Error. This username is taken." -> {
                    log("Error. This username is taken.")
                    register()
                }
                else -> {
                    log(argsMap["error"] ?: "")
                }
            }
        }
    }

    private fun register() {
        print("Enter username: ")
        val username = readLine() ?: ""
        sendMessage(
            Message(
                MessageType.REGISTRATION,
                ByteArray(32),
                mapper.writeValueAsString(mapOf("username" to username))
            )
        )
    }

    private fun sendMessage(message: Message) {
        val messageTypeBytes = message.messageType.value.toByte()
        val tokenBytes = message.token
        val messageBytes = message.message.toByteArray()
        val messageSizeBytes = messageBytes.size.toShort().toByteBuffer(2).array()
        val fullMessage = byteArrayOf(messageTypeBytes) + tokenBytes + messageSizeBytes + messageBytes
        //println("send message: ${fullMessage.toList()}")
        client.write(ByteBuffer.wrap(fullMessage))
    }

    private fun Short.toByteBuffer(capacity: Int): ByteBuffer =
        ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN).putShort(this)

    private fun log(str: String) {
        println(str)
    }
}