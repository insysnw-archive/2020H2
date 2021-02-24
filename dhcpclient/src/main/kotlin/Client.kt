import java.io.IOException
import java.net.*


const val SERVER_PORT = 67
const val CLIENT_PORT = 68
const val LIFETIME = 5000

//val BROADCAST_2 = byteArrayOf(192.toByte(), 168.toByte(), 0.toByte(), 255.toByte())
val BROADCAST = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())

fun main() {
    println("client started")

    val MAC: ByteArray = NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).hardwareAddress

    try {
        val discoverBytes = buildDiscover(MAC)
        val discoverSocket = DatagramSocket(CLIENT_PORT)
        discoverSocket.send(
            DatagramPacket(
                discoverBytes,
                discoverBytes.size,
                Inet4Address.getByAddress(BROADCAST),
                SERVER_PORT
            )
        )
        println("DISCOVER sent")
        discoverSocket.close()
        while (true) {
            val requestSocket = DatagramSocket(CLIENT_PORT)
            requestSocket.soTimeout = LIFETIME
            val responseBytes = ByteArray(250)
            requestSocket.receive(DatagramPacket(responseBytes, responseBytes.size))
            when (interpret(responseBytes)) {
                Responses.OFFER -> if (checkMAC(responseBytes, MAC)) {
                    println("OFFER received")
                    println(responseBytes.getIPString())
                    requestSocket.sendRequest(responseBytes)
                    println("REQUEST sent")
                }
                Responses.ACK -> if (checkMAC(responseBytes, MAC)) {
                    println("PACK received")
                    println(responseBytes.getIPString())
                    break
                }
                else -> println("UNKNOWN response received")
            }
            requestSocket.close()
        }
    } catch (e: SocketTimeoutException) {
        println("Server is not responding")
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

private fun DatagramSocket.sendRequest(response: ByteArray) {
    val bytes = buildRequest(response, response.getIP())
    send(DatagramPacket(bytes, bytes.size, Inet4Address.getByAddress(BROADCAST), SERVER_PORT))
}

private fun checkMAC(buf: ByteArray, MAC: ByteArray): Boolean {
    for (i in 0 until HLEN) {
        if (buf[i + CHADDR_OFFSET] != MAC[i]) return false
    }
    return true
}

private fun ByteArray.getIPString(): String {
    return "IP: ${getIP().map { it.toInt() and 0xFF }.joinToString(".")}"
}

private fun ByteArray.getIP() = this.copyOfRange(YIADDR_OFFSET, YIADDR_OFFSET + 4)