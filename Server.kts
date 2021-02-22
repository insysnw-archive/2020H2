package chat

import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel

var port = 9999
var host = "127.0.0.1"

if (args.isNotEmpty()) {
    host = args[0]
    if (args.size == 2) {
        port = args[1].toInt()
    }
}

val hostAddress: InetAddress = InetAddress.getByName(host)
val selector: Selector = Selector.open()
val serverSocketChannel: ServerSocketChannel = ServerSocketChannel.open().apply {
    configureBlocking(false)
    bind(InetSocketAddress(hostAddress, port))
    register(selector, SelectionKey.OP_ACCEPT)
}

println("Server started on address: $host:$port")

val selectedKeys: MutableSet<SelectionKey> = selector.selectedKeys()
while (true) {
    if (selector.select() <= 0) continue
    val iterator = selectedKeys.iterator()

    while (iterator.hasNext()) {
        val key: SelectionKey = iterator.next()
        iterator.remove()
        if (key.isAcceptable) {
            val sc = serverSocketChannel.accept()
            sc.configureBlocking(false)
            sc.register(selector, SelectionKey.OP_READ or SelectionKey.OP_WRITE)
            println("New client connected: ${sc.localAddress}...")
        }
        if (key.isReadable) {
            val clientSocketChannel = key.channel() as SocketChannel
            val msgByteBuffer = ByteBuffer.allocate(1024)
            clientSocketChannel.read(msgByteBuffer)
            if (!msgByteBuffer.array().all { it == 0.toByte() }) {
                broadcast(msgByteBuffer.array(), clientSocketChannel)
            } else {
                clientSocketChannel.close()
                println("Client left the chat")
            }
        }
    }
}

fun broadcast(msg: ByteArray, sender: SocketChannel) {
    val byteBufferMsg = ByteBuffer.wrap(msg)
    for (selectionKey in selector.keys()) {
        if (selectionKey.isValid && selectionKey.channel() is SocketChannel) {
            val socketChannel = selectionKey.channel() as SocketChannel
            if (socketChannel != sender) {
                socketChannel.write(byteBufferMsg)
                byteBufferMsg.rewind()
            }
        }
    }
}
