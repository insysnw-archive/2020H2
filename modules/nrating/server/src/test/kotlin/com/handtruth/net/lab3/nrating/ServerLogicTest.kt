package com.handtruth.net.lab3.nrating

import com.handtruth.net.lab3.nrating.messages.QueryMessage
import com.handtruth.net.lab3.nrating.types.*
import com.handtruth.net.lab3.util.MessageFormatException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ServerLogicTest {

    private lateinit var serverState: ServerState

    @BeforeEach
    fun init() {
        serverState = ServerState(mutableMapOf(
            Pair(1, TopicInternal("To be or not to be?", true, 3, mutableMapOf(
                Pair (1, AlternativeInternal("To be", 10)),
                Pair (2, AlternativeInternal("Not to be", 10)),
                Pair (3, AlternativeInternal("That is the question", 80))
            ))),
            Pair(2, TopicInternal("Who shot first?", true, 2, mutableMapOf(
                Pair (4, AlternativeInternal("Solo", 34)),
                Pair (5, AlternativeInternal("Grido", 33)),
            )))
        ), lastTopicId = 2, lastAlternativeId = 5)
    }

    @Test
    fun getTopicListQueryTest() {
        val message = QueryMessage(QueryMethod.GET, 0, 0)
        val response = handleGetQuery(serverState, message)

        assertEquals(QueryMethod.GET, response.method)
        assertEquals(QueryStatus.OK, response.status)
        assertEquals(0, response.topic)
        assertEquals(0, response.alternative)

        // TODO read options
    }

    @Test
    fun getTopicQueryTest() {
        val message = QueryMessage(QueryMethod.GET, 1, 0)
        val response = handleGetQuery(serverState, message)

        assertEquals(QueryMethod.GET, response.method)
        assertEquals(QueryStatus.OK, response.status)
        assertEquals(1, response.topic)
        assertEquals(0, response.alternative)

        // TODO read options
    }

    @Test
    fun invalidGetQueryTest() {
        val message = QueryMessage(QueryMethod.GET, 0, 10)
        val error = assertFailsWith<MessageFormatException> {
            handleGetQuery(serverState, message)
        }.message

        assertEquals("Invalid Get Query Format", error)
    }
}