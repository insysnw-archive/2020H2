package com.handtruth.net.lab3.nrating.vm

import com.handtruth.kommon.Log
import com.handtruth.kommon.default
import com.handtruth.net.lab3.nrating.*
import com.handtruth.net.lab3.nrating.types.Topic
import com.handtruth.net.lab3.nrating.types.TopicStatus
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

class ClientViewModel {
    private val log = Log.default("nrating/client/viewModel")

    private val scope = CoroutineScope(Dispatchers.Default + log + CoroutineName("nrating/client/viewModel"))

    val hostname = MutableStateFlow("localhost")

    fun setHostname(value: String) = perform {
        hostname.emit(value)
    }

    val port = MutableStateFlow(DEFAULT_PORT)

    fun setPort(value: Int) = perform {
        port.emit(value)
    }

    val state = MutableStateFlow(ClientState.Connect)

    val topics = MutableStateFlow(emptyList<Topic>())

    val error = MutableStateFlow(null as String?)

    val fatal = MutableStateFlow("")

    fun resetError() = perform {
        error.emit(null)
    }

    private fun perform(action: suspend () -> Unit) {
        scope.launch {
            try {
                action()
            } catch (e: RequestFailedException) {
                error.emit(e.message ?: "unknown error")
            } catch (e: Exception) {
                fatal.emit(e.message ?: "fatal error")
                state.emit(ClientState.Fail)
            }
        }
    }

    private val connection = scope.async(start = CoroutineStart.LAZY) {
        val socket = aSocket(selector).tcp().connect(hostname.value, port.value)
        ClientAPI(socket.openReadChannel(), socket.openWriteChannel())
    }

    fun connect() = perform {
        state.emit(ClientState.Connecting)
        connection.await()
        state.emit(ClientState.RatingList)
        fetchTopicList()
    }

    fun fetchTopicList() = perform {
        topics.emit(connection.await().getTopicList())
    }

    val currentTopic = MutableStateFlow(TopicStatus(Topic(0, ""), false, emptyList()))

    fun select(id: Int) = perform {
        state.emit(ClientState.Loading)
        try {
            val topic = connection.await().getTopic(id)
            currentTopic.emit(topic)
            state.emit(ClientState.Vote)
        } catch (e: Exception) {
            state.emit(ClientState.RatingList)
            throw e
        }
    }

    fun remove(id: Int) = perform {
        state.emit(ClientState.Loading)
        try {
            connection.await().removeTopic(id)
            topics.emit(connection.await().getTopicList())
        } finally {
            state.emit(ClientState.RatingList)
        }
    }

    fun gotoNewTopic() = perform {
        state.emit(ClientState.AddTopic)
    }

    fun gotoTopicList() = perform {
        state.emit(ClientState.RatingList)
    }

    fun newTopic(name: String, alternatives: Collection<String>) = perform {
        state.emit(ClientState.Loading)
        try {
            connection.await().addTopic(name, alternatives)
            topics.emit(connection.await().getTopicList())
        } finally {
            state.emit(ClientState.RatingList)
        }
    }

    fun open(id: Int) = perform {
        connection.await().openVote(id)
        select(id)
    }

    fun close(id: Int) = perform {
        connection.await().closeVote(id)
        select(id)
    }

    fun vote(id: Int, altId: Int) = perform {
        connection.await().vote(id, altId)
        select(id)
    }
}
