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
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.text.SimpleDateFormat
import java.util.*

object NIOServer {

    val users = mutableMapOf<List<Byte>, String>()
    val mapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).registerModule(KotlinModule())
    lateinit var keys: MutableSet<SelectionKey>
    lateinit var selector: Selector

    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {

        // Selector: multiplexor of SelectableChannel objects
        selector = Selector.open() // selector is open here

        // ServerSocketChannel: selectable channel for stream-oriented listening sockets
        val socket = ServerSocketChannel.open()
        val addr = InetSocketAddress("localhost", 1111)

        // Binds the channel's socket to a local address and configures the socket to listen for connections
        socket.bind(addr)

        // Adjusts this channel's blocking mode.
        socket.configureBlocking(false)
        val ops = socket.validOps()
        val selectKy = socket.register(selector, ops, null)
        log("i'm a server and i'm waiting for new connection and buffer select...")
        // Infinite loop..
        // Keep server running
        while (true) {
            // Selects a set of keys whose corresponding channels are ready for I/O operations
            selector.select()
            // token representing the registration of a SelectableChannel with a Selector
            keys = selector.selectedKeys().toMutableSet()
            val iterator = keys.iterator()
            while (iterator.hasNext()) {
                val myKey = iterator.next()

                // Tests whether this key's channel is ready to accept a new socket connection
                if (myKey.isAcceptable) {
                    val client = socket.accept()

                    client?.let {
                        // Adjusts this channel's blocking mode to false
                        client.configureBlocking(false)

                        // Operation-set bit for read-write operations
                        client.register(selector, SelectionKey.OP_READ or SelectionKey.OP_WRITE)
                        log(
                            """
                            Connection Accepted: ${client.localAddress}
                            """.trimIndent()
                        )
                    }

                    // Tests whether this key's channel is ready for reading
                }
                if (myKey.isReadable) {
                    val client = myKey.channel() as SocketChannel
                    val buffer = ByteBuffer.allocate(4096)
                    val res = client.read(buffer)
                    if (res == -1 || res == 0) continue
                    val message = convertFromBytesToMessage(buffer)
                    log("Message received: $message")
                    commuteMessage(message, client)
                    iterator.remove()
                }
            }
        }
    }

    private fun commuteMessage(message: Message, client: SocketChannel) {
        try {
            when (message.messageType) {
                MessageType.REGISTRATION -> {
                    val argumentsMap: Map<String, String> = mapper.readValue(message.message)
                    val username = argumentsMap["username"] ?: ""
                    if (users[username.sha256()
                            .toList()] != null
                    ) sendMessage(client, mapper.writeValueAsString(mapOf("error" to "Error. This username is taken.")))
                    else {
                        users[username.sha256().toList()] = username
                        println("token is: ${username.sha256().toList()}")
                        sendMessage(client, mapper.writeValueAsString(mapOf("token" to username.sha256())))
                        sendBroadcast(
                            mapper.writeValueAsString(mapOf("message" to "New user: $username"))
                        )
                    }
                }
                MessageType.MESSAGE -> {
                    val username = users[message.token.toList()]
                    username?.let {
                        sendBroadcast(mapper.writeValueAsString(mapOf("message" to "(${getCurrentTime()}) $username: ${message.message}")))
                    } ?: sendMessage(client, mapper.writeValueAsString(mapOf("error" to "Error. Forbidden.")))
                }
                MessageType.DISCONNECT -> {
                    client.close()
                    users.remove(message.token.toList())
                }
                else -> {
                }
            }
        } catch (e: Exception) {
        }
    }

    private fun convertFromBytesToMessage(buffer: ByteBuffer): Message {
        val messageType = when (buffer[0].toInt()) {
            0 -> MessageType.REGISTRATION
            1 -> MessageType.MESSAGE
            2 -> MessageType.DISCONNECT
            else -> MessageType.UNKNOWN
        }
        val token = buffer.array().toList().subList(1, 33).toByteArray()
        val messageSize =
            ByteBuffer.wrap(buffer.array().toList().subList(33, 35).toByteArray()).order(ByteOrder.LITTLE_ENDIAN).short
                .toInt()
        val message = String(buffer.array().toList().subList(35, 35 + messageSize).toByteArray())
        return Message(messageType, token, message)
    }

    private fun sendBroadcast(message: String) {
        val iterator = keys.iterator()
        while (iterator.hasNext()) {
            val key = iterator.next()
            if (key.isWritable) {
                val client = key.channel() as SocketChannel
                sendMessage(client, message)
            }
        }
    }

    private fun sendMessage(client: SocketChannel, message: String) {
        log("Message send: $message")
        val messageBytes = message.toByteArray()
        val messageSize = messageBytes.size.toShort().toByteBuffer(2).array()
        val buffer = ByteBuffer.wrap(messageSize + messageBytes)
        //log("Message send bytes: ${buffer.array().toList()}")
        client.write(buffer)
    }

    private fun Short.toByteBuffer(capacity: Int): ByteBuffer =
        ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN).putShort(this)

    private fun log(str: String) {
        println(str)
    }

    private fun getCurrentTime(): String = SimpleDateFormat("HH:mm", Locale("ru")).format(Date())
}