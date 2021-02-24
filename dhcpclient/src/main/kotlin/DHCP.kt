import java.util.*


val MAGIC_COOKIE = byteArrayOf(99, 130.toByte(), 83, 99)

const val SECS = 2
const val FLAGS = 2
const val CIADDR = 4
const val YIADDR = 4
const val SIADDR = 4
const val GIADDR = 4
const val SNAME = 64
const val FILE = 128

const val CLIENT_OP: Byte = 1
const val HTYPE: Byte = 1
const val HLEN: Byte = 6
const val HOPS: Byte = 0

const val CHADDR_OFFSET = 28
const val YIADDR_OFFSET = 16

const val MAGIC_OFFSET = 236
const val OPTIONS_OFFSET = 240

const val OPTIONS_END = 255.toByte()

const val DHCPDISCOVER: Byte = 1
const val DHCPREQUEST: Byte = 3

fun buildDiscover(MAC: ByteArray): ByteArray {
    val xid = ByteArray(4).apply { Random().nextBytes(this) }
    val options = byteArrayOf(53, 1, DHCPDISCOVER, OPTIONS_END)
    val header = byteArrayOf(CLIENT_OP, HTYPE, HLEN, HOPS)
    val emptyBody1 = ByteArray(SECS + FLAGS + CIADDR + YIADDR + SIADDR + GIADDR)
    val emptyBody2 = ByteArray(SNAME + FILE)
    return header + xid + emptyBody1 + chaddrFromMac(MAC) + emptyBody2 + MAGIC_COOKIE + options
}

fun buildRequest(response: ByteArray, requestedIp: ByteArray): ByteArray {
    val ret = ByteArray(240)
    ret[0] = CLIENT_OP
    System.arraycopy(response, 1, ret, 1, MAGIC_OFFSET - 1)
    var j = MAGIC_OFFSET
    while (j - MAGIC_OFFSET < MAGIC_COOKIE.size) {
        ret[j] = MAGIC_COOKIE[j - MAGIC_OFFSET]
        j++
    }
    val options = byteArrayOf(53, 1, DHCPREQUEST, 50, 4) + requestedIp + OPTIONS_END
//    for (i in 0 until 2) {
//        ret[j] = response[j]
//        j++
//    }
//    ret[j] = DHCPREQUEST
//    j++
//    ret[j] = OPTIONS_END
    return ret + options
}

fun chaddrFromMac(MAC: ByteArray) = ByteArray(16).apply { System.arraycopy(MAC, 0, this, 0, MAC.size) }

fun interpret(buf: ByteArray): Responses {
    if (!hasMagicCookie(buf)) return Responses.UNKNOWN
    var i = OPTIONS_OFFSET
    while (buf[i] != 53.toByte()) {
        i++
        i += buf[i]
        i++
    }
    i += 2
    return when (buf[i].toInt()) {
        2 -> Responses.OFFER
        5 -> Responses.ACK
        else -> Responses.UNKNOWN
    }
}

enum class Responses {
    OFFER,
    ACK,
    UNKNOWN
}

private fun hasMagicCookie(buf: ByteArray): Boolean {
    for (i in MAGIC_OFFSET until OPTIONS_OFFSET) {
        if (buf[i] != MAGIC_COOKIE[i - MAGIC_OFFSET]) return false
    }
    return true
}

fun createRequest(response: ByteArray, secs: ByteArray?): ByteArray? {
    val ret = ByteArray(246)
    ret[0] = CLIENT_OP
    System.arraycopy(response, 1, ret, 1, 235)
    var j = 236
    run {
        var i = 0
        while (i < MAGIC_COOKIE.size) {
            ret[j] = MAGIC_COOKIE[i]
            i++
            j++
        }
    }
    var i = 0
    while (i < 2) {
        ret[j] = response[j]
        i++
        j++
    }
    ret[j] = 3.toByte()
    j++
    ret[j] = 255.toByte()
    return ret
}