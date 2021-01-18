import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel

fun main(args: Array<String>) {
    var host = "127.0.0.1"
    var port = 8888

    if (args.size != 2) {
        println("HOST and PORT are not provided, using default")
    } else {
        host = args[0]
        port = args[1].toInt()
    }
    println("Starting server on $host:$port")
    Server(host, port).listen()

}

class Server(
        host: String,
        port: Int,
) {
    companion object {
        private const val buffSize = 1024
    }

    private val users = mutableMapOf<SocketChannel, String>()
    private val selector = Selector.open()
    private val mainChannel = ServerSocketChannel.open().apply {
        configureBlocking(false)
        bind(InetSocketAddress(host, port))
        register(selector, SelectionKey.OP_ACCEPT)
    }
    private val keys = selector.selectedKeys()

    fun listen() {
        while (true) {
            if (selector.select() <= 0) {
                continue
            }
            with(keys.iterator()) {
                while (hasNext()) {
                    val key = iterator().next()
                    remove()
                    if (key.isAcceptable) handleConnect(selector)
                    if (key.isReadable) handleRead(key)
                }
            }
        }
    }

    private fun handleConnect(selector: Selector) {
        with(mainChannel.accept()) {
            configureBlocking(false)
            register(selector, SelectionKey.OP_READ)
        }
    }

    private fun handleRead(key: SelectionKey) {
        with(key.channel() as SocketChannel) {
            val buffer = ByteBuffer.allocate(buffSize)
            try {
                read(buffer)
                val bytes = buffer.array()
                if (bytes.nonEmptyBytes()) {
                    when (ClientMessageType.fromByte(bytes.first())) {
                        ClientMessageType.CONNECT -> readConnectMessage(bytes.sliceArray(1 until bytes.size), this)
                        ClientMessageType.CHAT -> readChatMessage(bytes.sliceArray(1 until bytes.size), this)
                    }
                } else {
                    removeClient(this)
                }
            } catch (e: IOException){
                removeClient(this)
            }
        }

    }

    private fun readConnectMessage(bytes: ByteArray, client: SocketChannel) {
        val username = UsernameMessage.fromBytes(bytes).also { println("RECEIVED: $it CONNECT") }.text
        if (username in users.values) {
            handleSameUsername(username, client)
        } else {
            handleNewUsername(username, client)
        }
    }

    private fun handleSameUsername(username: String, client: SocketChannel) {
        sendToClient("name $username is already in use, srry", client)
        client.close()
        broadcastService(TextOnlyMessage("Impostor just tried to connect under $username's name"))
    }

    private fun handleNewUsername(username: String, client: SocketChannel) {
        broadcastService(TextOnlyMessage("$username just connected"), except = client)
        users[client] = username
    }


    private fun readChatMessage(bytes: ByteArray, client: SocketChannel) {
        println("RECEIVED CHAT")
        if (client in users) {
            val message = ClientMessage.fromBytes(bytes).also { println("MESSAGE: $it") }
            broadcastChat(ChatMessage(message.time, users[client]!!, message.text), client)
        } else {
            println("NOT CONNECTED")
            removeClient(client)
        }
    }

    private fun sendToClient(message: String, client: SocketChannel) {
        val buffer = ByteBuffer.wrap(ServerMessageType.SERVICE.encode() + TextOnlyMessage(message).toBytes())
        client.write(buffer)
        buffer.rewind()
    }

    private fun removeClient(client: SocketChannel) {
        client.close()
        val name = users.remove(client)
        broadcastService(TextOnlyMessage("$name disconnected"))
    }

    private fun broadcastChat(message: ChatMessage, except: SocketChannel) {
        broadcast(ServerMessageType.CHAT.encode() + message.toBytes(), except = except)
    }

    private fun broadcastService(message: TextOnlyMessage, except: SocketChannel? = null) {
        broadcast(ServerMessageType.SERVICE.encode() + message.toBytes(), except)
    }

    private fun broadcast(bytes: ByteArray, except: SocketChannel? = null) {
        val buffer = ByteBuffer.wrap(bytes)
        selector.keys().forEach {
            val channel = it.channel()
            if (it.isValid && channel is SocketChannel) {
                if (channel != except) {
                    channel.write(buffer)
                    buffer.rewind()
                }
            }
        }
    }
}