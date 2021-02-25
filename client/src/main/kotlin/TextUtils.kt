import protocol.Message

const val SEPARATOR = "->"

fun String.parse(): Message = with(split(SEPARATOR)) {
    return if (size == 1)
        Message("*", this@parse)
    else
        Message(first(), subList(1, size).joinToString(SEPARATOR))
}