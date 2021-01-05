package com.handtruth.net.lab3.sevent

import com.handtruth.net.lab3.message.Message
import com.handtruth.net.lab3.message.transmitter
import com.handtruth.net.lab3.sevent.message.*
import com.handtruth.net.lab3.sevent.types.EventParameters
import com.handtruth.net.lab3.sevent.types.FullEvent
import io.ktor.test.dispatcher.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.datetime.Clock
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.days
import kotlin.time.hours
import kotlin.time.seconds

class ServerTest {

    class ErrorMessageException(error: Error) : RuntimeException(error.toString())

    class Correspondent(
        val recv: ReceiveChannel<Message>,
        val send: SendChannel<Message>
    ) {
        suspend inline fun <reified M : Message> request(message: Message): M {
            send.send(message)
            val response = recv.receive()
            if (response is Error) {
                throw ErrorMessageException(response)
            }
            assertTrue(response is M, "wrong response type")
            return response
        }

        suspend fun assertErrorRequest(message: Message, code: Error.Codes): String {
            send.send(message)
            val response = recv.receive()
            assertTrue(response is Error, "response is not an error message")
            assertEquals(code, response.errorCode, "wrong error code")
            assertEquals(message.id, response.lastId, "wrong request id in Error message")
            return response.message
        }
    }

    fun CoroutineScope.splitNotificationsAndResponses(
        receiver: ReceiveChannel<Message>
    ): Pair<ReceiveChannel<Notify>, ReceiveChannel<Message>> {
        val notifications = Channel<Notify>()
        val responses = Channel<Message>()

        launch {
            try {
                for (message in receiver) {
                    if (message is Notify) {
                        notifications.send(message)
                    } else {
                        responses.send(message)
                    }
                }
            } finally {
                notifications.close()
                responses.close()
            }
        }

        return notifications to responses
    }

    fun CoroutineScope.correspondent(
        readChannel: ByteReadChannel,
        writeChannel: ByteWriteChannel
    ): Pair<Flow<Notify>, Correspondent> {
        val (recv, send) = transmitter(readChannel, writeChannel)
        val (notifications, responses) = splitNotificationsAndResponses(recv)
        return notifications.consumeAsFlow() to Correspondent(responses, send)
    }

    fun CoroutineScope.testServer(readChannel: ByteReadChannel, writeChannel: ByteWriteChannel) = launch {
        connection(readChannel, writeChannel, "TESTIFICATE")
    }

    fun CoroutineScope.testClient(readChannel: ByteReadChannel, writeChannel: ByteWriteChannel) = launch {
        val (notifications, correspondent) = correspondent(readChannel, writeChannel)

        var wasThere = 0

        launch(CoroutineName("test/consumer")) {
            notifications.collect {
                wasThere++
                println(it)
            }
        }

        launch(CoroutineName("test/correspondent")) {
            // По сути все тесты тут
            var req: Message = RegisterEvent(emptyList(), EventParameters("no no", 1.seconds, -1))
            correspondent.assertErrorRequest(req, Error.Codes.InvalidProperty)
            req = RegisterEvent(emptyList(), EventParameters("no no", 2.seconds, -2))
            correspondent.assertErrorRequest(req, Error.Codes.InvalidProperty)
            req = RegisterEvent(listOf("test"), EventParameters("kek", 2.seconds, 3))
            assertEquals(1, correspondent.request<EventRegistration>(req).eventId)
            req = ListEvents(emptyMap())
            assertEquals(listOf(1), correspondent.request<ListedEvents>(req).eventIds)
            req = RegisterEvent(listOf("group"), EventParameters("popka", 10000.hours, -1))
            assertEquals(2, correspondent.request<EventRegistration>(req).eventId)
            req = RegisterEvent(listOf("group"), EventParameters("empty", 10000.hours, 0))
            assertEquals(3, correspondent.request<EventRegistration>(req).eventId)
            req = RegisterEvent(listOf("group"), EventParameters("darda", 10000.hours, 10))
            assertEquals(4, correspondent.request<EventRegistration>(req).eventId)
            req = GetEvent(emptyMap())
            correspondent.assertErrorRequest(req, Error.Codes.FormatError)
            req = GetEvent(23)
            correspondent.assertErrorRequest(req, Error.Codes.EventNotExists)
            req = GetEvent(4)
            val event = correspondent.request<EventInfo>(req).fullEvent
            assertEquals(4, event.id)
            assertEquals(listOf("group"), event.groups)
            assertEquals("darda", event.description)
            assertEquals(10, event.repeat)
            req = Subscribe(listOf(3), listOf("test", "group"))
            correspondent.assertErrorRequest(req, Error.Codes.EventNotExists)
            correspondent.assertErrorRequest(FilterUpdated(), Error.Codes.WrongMessage)
            req = Subscribe(listOf(4), listOf("test", "group"))
            correspondent.request<FilterUpdated>(req)
            req = GetFilter()
            var res = correspondent.request<Filter>(req)
            assertEquals(listOf(4), res.eventIds)
            assertEquals(listOf("group", "test"), res.eventTypes.sorted())
            req = Unsubscribe(listOf(4), emptyList())
            correspondent.request<FilterUpdated>(req)
            req = Filter(emptyList(), listOf("test"))
            correspondent.request<FilterUpdated>(req)
            req = GetFilter()
            res = correspondent.request<Filter>(req)
            assertEquals(emptyList(), res.eventIds)
            assertEquals(listOf("test"), res.eventTypes)
            req = ListEvents(listOf("lol"))
            assertEquals(emptyList(), correspondent.request<ListedEvents>(req).eventIds)
            req = ListEvents(listOf("test"))
            assertEquals(listOf(1), correspondent.request<ListedEvents>(req).eventIds)
            req = ListEvents(listOf("group"))
            assertEquals(listOf(2, 4), correspondent.request<ListedEvents>(req).eventIds)
            req = ListEvents(emptyMap())
            assertEquals(listOf(1, 2, 4), correspondent.request<ListedEvents>(req).eventIds)
            req = DeleteEvent(listOf(3), listOf("group"))
            assertEquals(listOf(2, 4), correspondent.request<DeletedEvents>(req).eventIds)
            delay(7.seconds)
            assertEquals(3, wasThere, "not the all expected notifications received")
            writeChannel.close()
        }
    }

    @Test
    fun connectionTest() = testSuspend {
        val context = ServerContext(
            IdGenerator(),
            Repository(
                coroutineContext, listOf(
                    FullEvent(
                        id = 23,
                        groups = listOf("zombie"),
                        lastUpdate = Clock.System.now() - 10.days,
                        description = "Z",
                        period = 5.seconds,
                        repeat = 10
                    )
                )
            ),
            1.5.seconds
        )

        val forward = ByteChannel()
        val backward = ByteChannel()

        try {
            withContext(context) {
                testServer(forward, backward)
                testClient(backward, forward)
            }
        } catch (e: ClosedReceiveChannelException) {

        } finally {
            coroutineContext[Job]!!.cancelChildren()
        }

        println(context.repository.events)
    }

    @Test
    fun stateSaving() {
        val file = File.createTempFile("state", ".json")
        try {
            val state0 = State.load(File("nowhere.json"))
            assertEquals(State(0, emptyList()), state0)
            val stateA = State(
                23,
                listOf(
                    FullEvent(22, listOf("a", "b", "holy"), Clock.System.now(), "never", 1.seconds, 5),
                    FullEvent(10, listOf(), Clock.System.now(), "batiskaf", 5.seconds, -1)
                )
            )
            stateA.save(file)
            println(file.readText())
            val stateB = State.load(file)
            assertEquals(stateA, stateB)
        } finally {
            file.delete()
        }
    }
}
