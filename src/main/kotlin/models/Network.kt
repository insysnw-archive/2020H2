package models

data class Message(val header: Header = Header.empty(), val body: String = "") {
    override fun toString(): String =
        "${header}\n$body"

    fun toStringForLogging(): String = if (header.method == "GET") "${header}$body"
    else this.toString()


}

data class ResponseMessage(val header: ResponseHeader = ResponseHeader.empty(), val body: String = "")

data class Header(val method: String, val uri: String, val token: ByteArray = ByteArray(32)) {

    companion object {
        fun empty() = Header("", "")
    }

    override fun toString(): String {
        return "$method $uri ${token.toList()}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Header

        if (method != other.method) return false
        if (uri != other.uri) return false
        if (!token.contentEquals(other.token)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = method.hashCode()
        result = 31 * result + uri.hashCode()
        result = 31 * result + token.contentHashCode()
        return result
    }
}

fun ByteArray.isEqual(arr: ByteArray): Boolean {
    return try {
        if (this === arr) return true
        this.forEachIndexed { index, byte ->
            if (arr[index] != byte) return false
        }
        return true
    } catch (e: Exception) {
        false
    }
}

data class ResponseHeader(val code: Int) {
    companion object {
        fun empty() = ResponseHeader(-1)
    }

    override fun toString(): String {
        return "$code"
    }
}
