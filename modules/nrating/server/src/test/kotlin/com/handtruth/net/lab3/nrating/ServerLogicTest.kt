package com.handtruth.net.lab3.nrating

import com.handtruth.net.lab3.nrating.messages.QueryMessage
import com.handtruth.net.lab3.nrating.options.*
import com.handtruth.net.lab3.nrating.types.*
import com.handtruth.net.lab3.options.toOptions
import com.handtruth.net.lab3.util.ConcurrentMap
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServerLogicTest {

    private lateinit var serverState: ServerState

    @BeforeEach
    fun init() {
        serverState = ServerState(
            ConcurrentMap.wrap(
                mutableMapOf(
                    Pair(1, TopicInternal("To be or not to be?", true, 4,
                        ConcurrentMap.wrap(mutableMapOf(
                            Pair (1, AlternativeInternal("To be", 10)),
                            Pair (2, AlternativeInternal("Not to be", 10)),
                            Pair (3, AlternativeInternal("That is the question", 80))
                        )))),
                    Pair(2, TopicInternal("Who shot first?", false, 3,
                        ConcurrentMap.wrap(mutableMapOf(
                            Pair (4, AlternativeInternal("Solo")),
                            Pair (5, AlternativeInternal("Grido")),
                        ))))
                )
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

        assertTrue(response.options.containsKey(OptionType.TOPIC_LIST.code))

        response.getOption<TopicListOption>().topics.apply {
            assertEquals(2, this.size)
        }
    }

    @Test
    fun getTopicQueryTest() {
        val message = QueryMessage(QueryMethod.GET, 1, 0)
        val response = handleGetQuery(serverState, message)

        assertEquals(QueryMethod.GET, response.method)
        assertEquals(QueryStatus.OK, response.status)
        assertEquals(1, response.topic)
        assertEquals(0, response.alternative)

        assertTrue(response.options.containsKey(OptionType.TOPIC_STATUS.code))

        response.getOption<TopicStatusOption>().topicStatus.apply {
            assertEquals("To be or not to be?", this.topicData.name)
            assertTrue(this.isOpen)
            assertEquals(3, this.rating.size)
            assertEquals(3, this.rating[0].altId)
        }
    }

    @Test
    fun getMissingTopicQueryTest() {
        val message = QueryMessage(QueryMethod.GET, 3, 0)
        val response = handleGetQuery(serverState, message)

        assertEquals(QueryMethod.GET, response.method)
        assertEquals(QueryStatus.FAILED, response.status)
        assertEquals(3, response.topic)
        assertEquals(0, response.alternative)

        assertTrue(response.options.containsKey(OptionType.ERROR_MESSAGE.code))
        assertEquals("A topic with id 3 is not found.", response.getOption<ErrorMessageOption>().name)
    }

    @Test
    fun invalidGetQueryTest() {
        val message = QueryMessage(QueryMethod.GET, 0, 10)
        val response = handleGetQuery(serverState, message)

        assertEquals(QueryMethod.GET, response.method)
        assertEquals(QueryStatus.FAILED, response.status)
        assertEquals(0, response.topic)
        assertEquals(10, response.alternative)

        assertTrue(response.options.containsKey(OptionType.ERROR_MESSAGE.code))
        assertEquals("Invalid Get Query Format.", response.getOption<ErrorMessageOption>().name)
    }

    @Test
    fun addTopicQueryTest() {
        val message = QueryMessage(QueryMethod.ADD, 0, 2, toOptions(
            TopicNameOption("Pigman or Piglin???")
        ))
        val response = handleAddQuery(serverState, message)

        assertEquals(QueryMethod.ADD, response.method)
        assertEquals(QueryStatus.OK, response.status)
        assertEquals(0, response.alternative)

        assertEquals(3, serverState.topics.size)
        assertEquals("Pigman or Piglin???", serverState.topics[response.topic]?.name)
    }

    @Test
    fun addAlternativeQueryTest() {
        val message = QueryMessage(QueryMethod.ADD, 2, 0, toOptions(
            AlternativeNameOption("Jar Jar Binks!")
        ))
        val response = handleAddQuery(serverState, message)

        assertEquals(QueryMethod.ADD, response.method)
        assertEquals(QueryStatus.OK, response.status)
        assertEquals(2, response.topic)

        assertEquals(3, serverState.topics[2]!!.alternatives.size)

        // Can't add another one
        val badMessage = QueryMessage(QueryMethod.ADD, 2, 0, toOptions(
            AlternativeNameOption("Jar Jar Binks???")
        ))
        val badResponse = handleAddQuery(serverState, badMessage)

        assertEquals(QueryMethod.ADD, badResponse.method)
        assertEquals(QueryStatus.FAILED, badResponse.status)

        assertTrue(badResponse.options.containsKey(OptionType.ERROR_MESSAGE.code))
        assertEquals("Alternatives limit is reached.", badResponse.getOption<ErrorMessageOption>().name)
    }

    @Test
    fun addAlternativeWhileVoteIsOpenTest() {
        val message = QueryMessage(QueryMethod.ADD, 1, 0, toOptions(
            AlternativeNameOption("Or?")
        ))
        val response = handleAddQuery(serverState, message)

        assertEquals(QueryMethod.ADD, response.method)
        assertEquals(QueryStatus.FAILED, response.status)
        assertEquals(1, response.topic)

        assertTrue(response.options.containsKey(OptionType.ERROR_MESSAGE.code))
        assertEquals("Vote is locked! Can't add new alternatives.", response.getOption<ErrorMessageOption>().name)
    }

    @Test
    fun removeTopicQueryTest() {
        val message = QueryMessage(QueryMethod.DEL, 1, 0)
        val response = handleDelQuery(serverState, message)

        assertEquals(QueryMethod.DEL, response.method)
        assertEquals(QueryStatus.OK, response.status)
        assertEquals(1, response.topic)
        assertEquals(0, response.alternative)

        assertEquals(1, serverState.topics.size)
        assertFalse(serverState.topics.containsKey(1))
    }

    @Test
    fun removeNotExistingTopicTest() {
        val message = QueryMessage(QueryMethod.DEL, 42, 0)
        val response = handleDelQuery(serverState, message)

        assertEquals(QueryMethod.DEL, response.method)
        assertEquals(QueryStatus.FAILED, response.status)
        assertEquals(42, response.topic)
        assertEquals(0, response.alternative)

        assertEquals(2, serverState.topics.size)

        assertTrue(response.options.containsKey(OptionType.ERROR_MESSAGE.code))
        assertEquals("A topic with id 42 is not found.", response.getOption<ErrorMessageOption>().name)
    }

    @Test
    fun removeAlternativeQueryTest() {
        val message = QueryMessage(QueryMethod.DEL, 2, 4)
        val response = handleDelQuery(serverState, message)

        assertEquals(QueryMethod.DEL, response.method)
        assertEquals(QueryStatus.OK, response.status)
        assertEquals(2, response.topic)
        assertEquals(4, response.alternative)

        assertEquals(1, serverState.topics[2]!!.alternatives.size)
        assertFalse(serverState.topics[2]!!.alternatives.containsKey(4))
    }

    @Test
    fun openVoteQueryTest() {
        val message = QueryMessage(QueryMethod.OPEN, 2, 0)

        assertFalse(serverState.topics[2]!!.isOpen)
        val response = handleOpenQuery(serverState, message)

        assertEquals(QueryMethod.OPEN, response.method)
        assertEquals(QueryStatus.OK, response.status)
        assertEquals(2, response.topic)
        assertEquals(0, response.alternative)

        assertTrue(serverState.topics[2]!!.isOpen)
    }

    @Test
    fun openOpenedQueryTest() {
        val message = QueryMessage(QueryMethod.OPEN, 1, 0)

        assertTrue(serverState.topics[1]!!.isOpen)
        val response = handleOpenQuery(serverState, message)

        assertEquals(QueryMethod.OPEN, response.method)
        assertEquals(QueryStatus.FAILED, response.status)
        assertEquals(1, response.topic)
        assertEquals(0, response.alternative)

        assertTrue(response.options.containsKey(OptionType.ERROR_MESSAGE.code))
        assertEquals("A vote is already on.", response.getOption<ErrorMessageOption>().name)
    }

    @Test
    fun closeVoteQueryTest() {
        val message = QueryMessage(QueryMethod.CLOSE, 1, 0)

        assertTrue(serverState.topics[1]!!.isOpen)
        val response = handleCloseQuery(serverState, message)

        assertEquals(QueryMethod.CLOSE, response.method)
        assertEquals(QueryStatus.OK, response.status)
        assertEquals(1, response.topic)
        assertEquals(0, response.alternative)

        assertFalse(serverState.topics[1]!!.isOpen)
        assertTrue(serverState.topics[1]!!.isClosed)
    }

    @Test
    fun closeNotOpenedQueryTest() {
        val message = QueryMessage(QueryMethod.CLOSE, 2, 0)

        assertFalse(serverState.topics[2]!!.isOpen)
        assertFalse(serverState.topics[2]!!.isClosed)
        val response = handleCloseQuery(serverState, message)

        assertEquals(QueryMethod.CLOSE, response.method)
        assertEquals(QueryStatus.FAILED, response.status)
        assertEquals(2, response.topic)
        assertEquals(0, response.alternative)

        assertTrue(response.options.containsKey(OptionType.ERROR_MESSAGE.code))
        assertEquals("A vote hasn't started yet.", response.getOption<ErrorMessageOption>().name)
    }

    @Test
    fun voteQueryTest() {
        val message = QueryMessage(QueryMethod.VOTE, 1, 1)
        val response = handleVoteQuery(serverState, message)

        assertEquals(QueryMethod.VOTE, response.method)
        assertEquals(QueryStatus.OK, response.status)
        assertEquals(1, response.topic)
        assertEquals(1, response.alternative)

        assertEquals(11, serverState.topics[1]!!.alternatives[1]!!.votes)
    }

    @Test
    fun voteNotOpenedQueryTest() {
        val message = QueryMessage(QueryMethod.VOTE, 2, 3)
        val response = handleVoteQuery(serverState, message)

        assertEquals(QueryMethod.VOTE, response.method)
        assertEquals(QueryStatus.FAILED, response.status)
        assertEquals(2, response.topic)
        assertEquals(3, response.alternative)

        assertTrue(response.options.containsKey(OptionType.ERROR_MESSAGE.code))
        assertEquals("A vote hasn't started yet.", response.getOption<ErrorMessageOption>().name)
    }
}