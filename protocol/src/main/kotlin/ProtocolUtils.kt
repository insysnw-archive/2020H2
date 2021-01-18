import java.nio.ByteBuffer
import java.time.Instant

enum class ServerMessageType {
    CHAT,
    SERVICE;

    fun encode() = byteArrayOf(this.ordinal.toByte())

    companion object {
        fun fromByte(byte: Byte) = values()[byte.toInt()]
    }
}

enum class ClientMessageType {
    CONNECT,
    CHAT;

    fun encode() = byteArrayOf(this.ordinal.toByte())

    companion object {
        fun fromByte(byte: Byte) = values()[byte.toInt()]
    }
}

const val TIME_BYTES = 4 // UTC size before 2038

const val NAME_MAX_LENGTH = 50
const val NAME_HEADER_BYTES = 1

const val TEXT_MAX_LENGTH = 280
const val TEXT_HEADER_BYTES = 2

val CHARSET = Charsets.UTF_8

fun ByteArray.decodeInt() = ByteBuffer
        .allocate(Int.SIZE_BYTES)
        .also {
            var bufInd = Int.SIZE_BYTES - 1
            for (i in this.size - 1 downTo 0) {
                it.put(bufInd--, this[i])
            }
            it.rewind()
        }
        .int

fun ByteArray.decodeStringLength(maxLength: Int) = decodeInt().also { require(it in 1 until maxLength) }

fun ByteArray.decodeTime(): Instant = Instant.ofEpochSecond(decodeInt().toLong())

fun ByteArray.decodeString() = String(this, CHARSET)

fun ByteArray.decodeTextBlock(dataOffset: Int, maxTextLength: Int): Pair<Int, String> { //returns block length and text
    val length = this.sliceArray(0 until dataOffset).decodeStringLength(maxTextLength)
    val text = this.sliceArray(dataOffset until dataOffset + length).decodeString()
    return dataOffset + length to text
}

fun Int.encode(bytes: Int): ByteArray = ByteBuffer
        .allocate(Int.SIZE_BYTES)
        .putInt(this)
        .array()
        .sliceArray(Int.SIZE_BYTES - bytes until Int.SIZE_BYTES)

fun String.encode(maxTextLength: Int, headerBytes: Int): Pair<ByteArray, ByteArray> {
    val string = if (length > maxTextLength) dropLast(length - maxTextLength) else this
    val header = string.length.encode(headerBytes)
    return header to string.toByteArray(CHARSET)
}

fun Instant.encode() = epochSecond.toInt().encode(TIME_BYTES)

fun ByteArray.nonEmptyBytes() = any { it != 0.toByte() }