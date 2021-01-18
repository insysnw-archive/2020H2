package network

data class Header(val serviceInformation: Map<String, String> = emptyMap(),
                  val time: String = "",
                  var nickname: String = "") {
    companion object {
        fun emptyHeader() = Header(time = "", nickname = "")
    }

    override fun toString(): String {
        return "${serviceInformation.toList().joinToString(",") { "${it.first}:${it.second}" }} $time $nickname"
    }
}

data class Message(val header: Header, val body: String = "") {
    companion object {
        fun emptyMessage() = Message(Header.emptyHeader(), "")
    }

    fun getFormattedMessage() = "(${header.time}) ${header.nickname}: $body"

    override fun toString(): String {
        return "$header $body"
    }
}