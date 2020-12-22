package com.handtruth.net.lab3.nrating

import com.handtruth.net.lab3.nrating.messages.QueryMessage
import com.handtruth.net.lab3.nrating.messages.QueryResponseMessage
import com.handtruth.net.lab3.nrating.options.*
import com.handtruth.net.lab3.nrating.types.*
import com.handtruth.net.lab3.options.toOptions
import com.handtruth.net.lab3.util.ConcurrentMap
import kotlinx.coroutines.runBlocking
import java.lang.UnsupportedOperationException

data class ServerState(
    val topics: ConcurrentMap<Int, TopicInternal> = ConcurrentMap(),
    var lastTopicId: Int = 0, var lastAlternativeId: Int = 0
)

fun errorResponse(query: QueryMessage, message: String): QueryResponseMessage = QueryResponseMessage(
    query.method, QueryStatus.FAILED, query.topic, query.alternative,
    toOptions(ErrorMessageOption(message))
)

fun handleQueryMessage(serverState: ServerState, query: QueryMessage): QueryResponseMessage {
    return when (query.method) {
        QueryMethod.GET -> handleGetQuery(serverState, query)
        QueryMethod.ADD -> handleAddQuery(serverState, query)
        QueryMethod.DEL -> handleDelQuery(serverState, query)
        else -> throw UnsupportedOperationException("Query type #${query.method.code} is not supported yet!")
    }
}

fun handleGetQuery(serverState: ServerState, query: QueryMessage): QueryResponseMessage {
    if (query.topic == 0 && query.alternative == 0) {
        // Get Topic List Query
        val topicList = serverState.topics.mapValues { (k, v) -> Topic(k, v.name) }.values.toList()
        return QueryResponseMessage(query.method, QueryStatus.OK, 0, 0, toOptions(TopicListOption(topicList)))

    } else if (query.topic > 0 && query.alternative == 0) {
        // Get Topic Query
        val topic = serverState.topics[query.topic]
            ?: return errorResponse(query, "A topic with id ${query.topic} is not found.")

        var totalVotes = 0
        topic.alternatives.forEach { totalVotes += it.value.votes }
        val topicStatus = TopicStatus(
            Topic(query.topic, topic.name),
            topic.isOpen,
            topic.alternatives
                .map { (k, v) -> RatingItem(k, v.name, v.votes, v.votes.toDouble() / totalVotes) }
                .sortedByDescending { it.votesAbs }
        )
        return QueryResponseMessage(
            query.method, QueryStatus.OK, query.topic, 0, toOptions(TopicStatusOption(topicStatus))
        )
    } else {
        return errorResponse(query, "Invalid Get Query Format.")
    }
}

fun handleAddQuery(serverState: ServerState, query: QueryMessage): QueryResponseMessage {
    if (query.topic == 0) {
        // Add Topic Query
        val id = ++serverState.lastTopicId
        val maxAlternatives = query.alternative
        val topicName = query.getOptionOrNull<TopicNameOption>()
            ?: return errorResponse(query, "TopicName option is missing.")
        val maxVotes = query.getOptionOrNull<AllowMultipleVotesOption>()

        runBlocking {
            if (maxVotes != null) {
                serverState.topics.put(id, TopicInternal(topicName.name, false, maxAlternatives, maxVotes.maxVotes))
            } else {
                serverState.topics.put(id, TopicInternal(topicName.name, false, maxAlternatives))
            }
        }
        return QueryResponseMessage(query.method, QueryStatus.OK, id, 0)

    } else if (query.topic > 0 && query.alternative == 0) {
        // Add Alternative Query
        val id = ++serverState.lastAlternativeId
        val alternativeName = query.getOptionOrNull<AlternativeNameOption>()
            ?: return errorResponse(query, "AlternativeName option is missing.")

        return runBlocking {
            if (!serverState.topics.containsKey(query.topic))
                errorResponse(query, "A topic with id ${query.topic} is not found.")

            with(serverState.topics[query.topic]!!) {
                if (this.alternatives.size < this.maxAlternatives) {
                    if (!this.isOpen) {
                        this.alternatives.put(
                            id, AlternativeInternal(alternativeName.name)
                        )
                        QueryResponseMessage(query.method, QueryStatus.OK, query.topic, id)
                    } else {
                        errorResponse(query, "Vote is on! Can't add new alternatives.")
                    }
                } else {
                    errorResponse(query, "Alternatives limit is reached.")
                }
            }
        }

    } else {
        return errorResponse(query, "Invalid Add Query Format.")
    }
}

fun handleDelQuery(serverState: ServerState, query: QueryMessage): QueryResponseMessage {
    if (query.topic > 0 && query.alternative == 0) {
        // Remove Topic Query
        return runBlocking {
            if (!serverState.topics.containsKey(query.topic)) {
                errorResponse(query, "A topic with id ${query.topic} is not found.")
            } else {
                serverState.topics.remove(query.topic)
                QueryResponseMessage(query.method, QueryStatus.OK, query.topic, 0)
            }
        }
    } else if (query.topic > 0 && query.alternative > 0) {
        // Remove Alternative Query
        return runBlocking {
            if (!serverState.topics.containsKey(query.topic)) {
                errorResponse(query, "A topic with id ${query.topic} is not found.")
            } else {
                with(serverState.topics[query.topic]!!) {
                    if (!this.isOpen) {
                        if (!this.alternatives.containsKey(query.alternative))
                            errorResponse(query, "An alternative with id ${query.alternative} is not found.")
                        this.alternatives.remove(query.alternative)
                        QueryResponseMessage(query.method, QueryStatus.OK, query.topic, query.alternative)
                    } else {
                        errorResponse(query, "Vote is on! Can't remove alternatives.")
                    }
                }
            }
        }
    } else {
        return errorResponse(query, "Invalid Remove Query Format.")
    }
}