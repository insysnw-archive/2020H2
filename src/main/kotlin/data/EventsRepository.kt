package data

import Event

class EventsRepository() {
    private val apiRepository = ApiRepository(byteArrayOf())

    fun getEventsList(): List<Event>? = apiRepository.getEventsList()

    fun addEvent(event: Event) = apiRepository.addEvent(event)

    fun deleteEvent(id: Int) = apiRepository.deleteEvent(id)

    fun subscribe(id: Int) = apiRepository.subscribe(id)

    fun unsubscribe(id: Int) = apiRepository.unsubscribe(id)

    fun notification(): Event? = apiRepository.notification()

    fun registerForNotifications() = apiRepository.registerForNotifications()

    fun register(credentials: String): ByteArray? =
        apiRepository.register(credentials)?.token

    fun setToken(token: ByteArray) {
        apiRepository.authToken = token
    }

    fun onDestroy() {
        apiRepository.onDestroy()
    }
}