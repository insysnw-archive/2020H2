package protocol


data class Handshake(val name: String) {
    companion object : Parser<Handshake> {
        override fun parse(map: Map<String, String>) = Handshake(map["name"]!!)
    }
}

data class Message(val destination: String, val text: String) {
    companion object : Parser<Message> {
        override fun parse(map: Map<String, String>) = Message(map["destination"]!!, map["text"]!!)
    }
}

data class Update(val sender: String, val text: String){
    companion object : Parser<Update> {
        override fun parse(map: Map<String, String>) = Update(map["sender"]!!, map["text"]!!)
    }
}
