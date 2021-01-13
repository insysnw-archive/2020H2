package network

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import models.Header
import models.Message


fun Messenger.respondText(text: String) {
}

fun Messenger.respond(message: Message) {
    sendMessage(message)
}

fun <T> Messenger.respondObject(dataClass: T) {
    val mapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).registerModule(KotlinModule())
    val jsonDate = mapper.writeValueAsString(dataClass)
    sendMessage(Message(Header.ok(), jsonDate))
}

inline fun <reified T> Messenger.receive(): T {
    val mapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).registerModule(KotlinModule())
    return mapper.readValue(currentMessage.body)
}

fun Messenger.respondError(message: Message) {
    sendMessage(message)
}