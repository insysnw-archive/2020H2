package com.handtruth.net.lab3.nrating

import com.handtruth.net.lab3.nrating.messages.QueryMessage
import com.handtruth.net.lab3.nrating.messages.QueryResponseMessage
import com.handtruth.net.lab3.nrating.options.TopicListOption
import com.handtruth.net.lab3.nrating.options.TopicStatusOption
import com.handtruth.net.lab3.nrating.types.*
import com.handtruth.net.lab3.util.MessageFormatException
import java.lang.UnsupportedOperationException

data class ServerState(
    val topics: MutableMap<Int, TopicInternal> = hashMapOf(),
    var lastTopicId: Int = 0, var lastAlternativeId: Int = 0
)

fun handleQueryMessage(serverState: ServerState, query: QueryMessage): QueryResponseMessage {
    return when (query.method) {
        QueryMethod.GET -> handleGetQuery(serverState, query)
//        QueryMethod.ADD -> handleAddQuery(serverState, query)
//        QueryMethod.DEL -> handleDelQuery(serverState, query)
        else -> throw UnsupportedOperationException("Query type #${query.method.code} is not supported yet!")
    }
}

fun handleGetQuery(serverState: ServerState, query: QueryMessage): QueryResponseMessage {
    if (query.topic == 0 && query.alternative == 0) {
        // Get Topic List Query
        val topicList = serverState.topics.mapValues { (k, v) -> Topic(k, v.name) }.values.toList()
        return QueryResponseMessage(query.method, QueryStatus.OK, 0, 0, listOf(TopicListOption(topicList)))

    } else if (query.topic > 0 && query.alternative == 0) {
        // Get Topic Query
        val topic = serverState.topics[query.topic]
        return if (topic != null) {
            var totalVotes = 0
            topic.alternatives.forEach { totalVotes += it.value.votes }
            val topicStatus = TopicStatus(
                Topic(query.topic, topic.name),
                topic.isOpen,
                topic.alternatives
                    .map { (k, v) -> RatingItem(k, v.name, v.votes, v.votes.toDouble() / totalVotes) }
                    .sortedByDescending { it.votesAbs }
            )
            QueryResponseMessage(query.method, QueryStatus.OK, query.topic, 0, listOf(TopicStatusOption(topicStatus)))
        } else {
            QueryResponseMessage(query.method, QueryStatus.FAILED, query.topic, 0)
            // todo add option
        }
    } else {
        throw MessageFormatException("Invalid Get Query Format")
    }
}

fun handleAddQuery(serverState: ServerState, query: QueryMessage) {
    if (query.topic == 0) {
        // Add Topic Query
        // TODO
    } else if (query.topic > 0 && query.alternative == 0) {
        // Add Alternative Query
        // TODO
    } else {
        throw MessageFormatException("Invalid Add Query Format")
    }
}

fun handleDelQuery(serverState: ServerState, query: QueryMessage) {
    if (query.topic > 0 && query.alternative == 0) {
        // Remove Topic Query
        // TODO
    } else if (query.topic > 0 && query.alternative > 0) {
        // Remove Alternative Query
        // TODO
    } else {
        throw MessageFormatException("Invalid Remove Query Format")
    }
}