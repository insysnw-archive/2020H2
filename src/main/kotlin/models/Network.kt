package models


data class Message(val header: Header, val body: String = "") {
    override fun toString(): String =
        "${header}\n$body"

    fun toStringForLogging(): String = if (header.method == "GET") "${header}$body"
    else this.toString()


}

data class Header(val method: String, val uri: String) {
    companion object {
        fun badRequest(text: String = ""): Header =
            Header("400", "Bad request: $text")

        fun ok(): Header =
            Header("200", "OK")

        fun notFound(): Header =
            Header("404", "Not found")

    }

    override fun toString(): String {
        return "$method $uri"
    }
}