package chat

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.concurrent.thread
import kotlin.system.exitProcess

val startMsgIndex = 6

var port = 9999
var host = "127.0.0.1"

if (args.isNotEmpty()) {
    host = args[0]
    if (args.size == 2) {
        port = args[1].toInt()
    }
}

val addr = InetSocketAddress(InetAddress.getByName(host), port)

print("Enter your name: ")
val bufferedReader = BufferedReader(InputStreamReader(System.`in`))
val username: String = bufferedReader.readLine()

val selector: Selector = Selector.open()
val sc: SocketChannel = SocketChannel.open().apply {
    configureBlocking(false)
    connect(addr)
    register(selector, SelectionKey.OP_CONNECT or SelectionKey.OP_READ or SelectionKey.OP_WRITE)
}
println("Welcome to the chat, $username!")

thread { write() }

while (true) {
    if (selector.select() > 0) {
        val doneStatus = processReadySet(selector.selectedKeys())
        if (doneStatus) {
            break
        }
    }
}

sc.close()

fun processReadySet(readySet: MutableSet<*>): Boolean {
    try {
        var key: SelectionKey? = null
        val iterator = readySet.iterator()
        while (iterator.hasNext()) {
            key = iterator.next() as SelectionKey?
            iterator.remove()
        }
        if (key!!.isConnectable) {
            val connected = processConnect(key)
            if (!connected) {
                return true
            }
        }
        if (key.isReadable) {
            val sc = key.channel() as SocketChannel
            val buf = ByteBuffer.allocate(1024)
            sc.read(buf)
            val msgLength = buf.array().first()
            val msg = String(buf.array().slice(startMsgIndex until startMsgIndex + msgLength).toByteArray())
            val time = String(buf.array().slice(1 until startMsgIndex).toByteArray())
            val nameLength = buf[startMsgIndex + msgLength]
            val name =
                String(
                    buf.array().slice(startMsgIndex + msgLength + 1..startMsgIndex + msgLength + nameLength)
                        .toByteArray()
                )
            println("$time [$name]: $msg")
        }
        return false
    } catch (e: IOException) {
        println("Server is not responding...\nWe are leaving this place...")
        exitProcess(0)
    }
}

fun write() {
    val reader = BufferedReader(InputStreamReader(System.`in`))
    while (true) {
        val msg = reader.readLine()
        if (msg == "quit") {
            sc.close()
            exitProcess(0)
        } else {
            if (msg.isNotEmpty() && msg != "^C") {
                send(msg)
            }
        }
    }
}

fun send(msg: String) {
    var mMsg = msg
    val msgLength = mMsg.length

    if (msgLength > Byte.MAX_VALUE) {
        mMsg = mMsg.dropLast(msgLength - Byte.MAX_VALUE)
    }

    val msgLengthByte = mMsg.length.toByte()
    val timeString = SimpleDateFormat("HH:mm").format(Date())

    val resByteArray =
        byteArrayOf(msgLengthByte) + timeString.toByteArray() + mMsg.toByteArray() + byteArrayOf(username.length.toByte()) + username.toByteArray()
    selector.select()
    selector.selectedKeys().forEach {
        val sc = it.channel() as SocketChannel
        val bb = ByteBuffer.wrap(resByteArray)
        sc.write(bb)
    }
}

fun processConnect(key: SelectionKey?): Boolean {
    val sc = key!!.channel() as SocketChannel
    try {
        while (sc.isConnectionPending) {
            sc.finishConnect()
        }
    } catch (e: IOException) {
        println("Server is not responding")
        key.cancel()
        exitProcess(0)
    }
    return true
}
