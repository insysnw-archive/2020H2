package data

import Event
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import models.*

class ApiRepository(var authToken: ByteArray) {

    private val ip = "188.243.130.195"
    private val port = 1999

    val messenger = Messenger(ip, port)
    val notificationsMessenger = Messenger(ip, port)
    private val mapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).registerModule(KotlinModule())

    fun getEventsList(): List<Event>? {
        val header = Header("GET", "/events", authToken)
        return makeRequest(Message(header))
    }

    fun addEvent(event: Event) {
        val header = Header("POST", "/add-event", authToken)
        val body = mapper.writeValueAsString(event)
        makeRequest<Any>(Message(header, body))
    }

    fun deleteEvent(id: Int) {
        val header = Header("POST", "/delete-event", authToken)
        val body = mapper.writeValueAsString(mapOf("id" to id))
        makeRequest<Any>(Message(header, body))
    }

    fun subscribe(id: Int) {
        val header = Header("POST", "/subscribe", authToken)
        val body = mapper.writeValueAsString(mapOf("id" to id))
        makeRequest<Any>(Message(header, body))
    }

    fun unsubscribe(id: Int) {
        val header = Header("POST", "/unsubscribe", authToken)
        val body = mapper.writeValueAsString(mapOf("id" to id))
        makeRequest<Any>(Message(header, body))
    }

    fun notification(): Event? {
        val response = notificationsMessenger.readMessage()
        return try {
            mapper.readValue<Event>(response.body)
        } catch (e: Exception) {
            null
        }
    }

    fun registerForNotifications() {
        val header = Header("GET", "/notification", authToken)
        notificationsMessenger.sendMessage(Message(header))
        val response = notificationsMessenger.readMessage()
        receiveValue<Any>(response)
    }

    fun register(credentials: String): Token? {
        val header = Header("POST", "/register")
        val body = mapper.writeValueAsString(Credentials(credentials))
        return makeRequest(Message(header, body))
    }

    inline fun <reified T> makeRequest(message: Message): T? {
        messenger.sendMessage(message)
        val response = messenger.readMessage()
        return receiveValue<T>(response)
    }

    inline fun <reified T> receiveValue(responseMessage: ResponseMessage): T? {
        val mapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).registerModule(KotlinModule())
        return when (responseMessage.header.code) {
            in 200..299 -> {
                println("Successful")
                try {
                    val body = if (responseMessage.body.isBlank()) "{}"
                    else responseMessage.body

                    mapper.readValue<T>(body)
                } catch (e: Exception) {
                    println(e)
                    null
                }
            }
            in 300..399 -> {
                println("${responseMessage.header.code} Redirections error")
                null
            }
            in 400..499 -> {
                println("${responseMessage.header.code} Client error")
                null
            }
            else -> {
                println("Error")
                null
            }
        }
    }

    fun onDestroy() {
        messenger.onDestroy()
    }
}