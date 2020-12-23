package com.handtruth.net.lab3.nrating.types

import com.handtruth.net.lab3.types.*
import io.ktor.utils.io.core.*

data class TopicStatus(val topicData: Topic, val isOpen: Boolean, val rating: List<RatingItem>)

/**
 * Декодирует информацию о статусе голосования.
 * @receiver синхронный поток данных, откуда следует считать информацию о статусе голосования.
 * @return декодированная информация о статусе голосования.
 */
fun Input.readTopicStatus(): TopicStatus {
    val topicData = readTopic()
    val isOpen = readBool()
    val rating = readVarList { readRatingItem() }
    return TopicStatus(topicData, isOpen, rating)
}

/**
 * Кодирует информацию о статусе голосования.
 * @receiver синхронный поток, куда следует записать информацию о статусе голосования.
 * @param topicStatus информация о статусе голосования, которую надо закодировать.
 */
fun Output.writeTopicStatus(topicStatus: TopicStatus) {
    writeTopic(topicStatus.topicData)
    writeBool(topicStatus.isOpen)
    writeVarList(topicStatus.rating) { writeRatingItem(it) }
}
