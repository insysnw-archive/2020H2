import java.time.Instant

data class ChatMessage(
        val time: Instant,
        val name: String,
        val text: String,
) {
    companion object {

        fun fromBytes(bytes: ByteArray): ChatMessage {
            val time = bytes.sliceArray(0 until TIME_BYTES).decodeTime()

            val (nameBlockLength, name) = bytes
                    .sliceArray(TIME_BYTES until bytes.size)
                    .decodeTextBlock(NAME_HEADER_BYTES, NAME_MAX_LENGTH)

            val (_, text) = bytes
                    .sliceArray(TIME_BYTES + nameBlockLength until bytes.size)
                    .decodeTextBlock(TEXT_HEADER_BYTES, TEXT_MAX_LENGTH)

            return ChatMessage(time, name, text)
        }

    }

    fun toBytes(): ByteArray {
        val (nameHeader, nameData) = name.encode(NAME_MAX_LENGTH, NAME_HEADER_BYTES)
        val (textHeader, textData) = text.encode(TEXT_MAX_LENGTH, TEXT_HEADER_BYTES)
        return time.encode() + nameHeader + nameData + textHeader + textData
    }

}