data class TextOnlyMessage(
        val text: String
) {

    companion object {
        fun fromBytes(bytes: ByteArray): TextOnlyMessage {
            val (_, name) = bytes.decodeTextBlock(dataOffset = TEXT_HEADER_BYTES, maxTextLength = TEXT_MAX_LENGTH)
            return TextOnlyMessage(name)
        }
    }

    fun toBytes(): ByteArray {
        val (header, data) = text.encode(TEXT_MAX_LENGTH, TEXT_HEADER_BYTES)
        return header + data
    }

}
