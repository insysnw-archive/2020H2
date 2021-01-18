package com.dekinci.uni.net.first

import java.time.Instant
import java.time.ZoneId

class Announcement(val ts: Instant, val text: String) {
    companion object : Parser<Announcement> {
        override fun parse(map: Map<String, String>) = Announcement(Instant.parse(map["ts"]!!), map["text"]!!)
    }

    override fun toString(): String {
        val time = ts.atZone(ZoneId.systemDefault())
        return "<${time.hour}:${time.minute}:${time.second}> *** $text ***"
    }
}

class Kick(val ts: Instant, val text: String) {
    companion object : Parser<Kick> {
        override fun parse(map: Map<String, String>) = Kick(Instant.parse(map["ts"]!!), map["text"]!!)
    }

    override fun toString(): String {
        val time = ts.atZone(ZoneId.systemDefault())
        return "<${time.hour}:${time.minute}:${time.second}> !!! $text !!!"
    }
}

class MessageUpdate(val name: String, val ts: Instant, val text: String) {
    companion object : Parser<MessageUpdate> {
        override fun parse(map: Map<String, String>) = MessageUpdate(map["name"]!!, Instant.parse(map["ts"]!!), map["text"]!!)
    }

    override fun toString(): String {
        val time = ts.atZone(ZoneId.systemDefault())
        return "<${time.hour}:${time.minute}:${time.second}> $name: $text"
    }
}

class Message(val text: String) {
    companion object : Parser<Message> {
        override fun parse(map: Map<String, String>) = Message(map["text"]!!)
    }
}

class Handshake(val name: String) {
    companion object : Parser<Handshake> {
        override fun parse(map: Map<String, String>) = Handshake(map["name"]!!)
    }
}
