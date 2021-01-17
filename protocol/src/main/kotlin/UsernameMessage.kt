data class UsernameMessage(
        val text: String
) {

    companion object {
        fun fromBytes(bytes: ByteArray): TextOnlyMessage {
            val (_, name) = bytes.decodeTextBlock(dataOffset = NAME_HEADER_BYTES, maxTextLength = NAME_MAX_LENGTH)
            return TextOnlyMessage(name)
        }
    }

    fun toBytes(): ByteArray {
        val (header, data) = text.encode(NAME_MAX_LENGTH, NAME_HEADER_BYTES)
        return header + data
    }

}