import java.time.Instant

data class ClientMessage(
        val time: Instant,
        val text: String
) {

    companion object {

        fun fromBytes(bytes: ByteArray): ClientMessage {
            val time = bytes.sliceArray(0 until TIME_BYTES).decodeTime()
            val (_, name) = bytes
                    .sliceArray(TIME_BYTES until bytes.size)
                    .decodeTextBlock(dataOffset = TEXT_HEADER_BYTES, maxTextLength = TEXT_MAX_LENGTH)
            return ClientMessage(time, name)
        }

    }

    fun toBytes(): ByteArray {
        val (header, data) = text.encode(TEXT_MAX_LENGTH, TEXT_HEADER_BYTES)
        return time.encode() + header + data
    }

}