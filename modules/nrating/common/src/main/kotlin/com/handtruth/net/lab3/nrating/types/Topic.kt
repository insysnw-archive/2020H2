package com.handtruth.net.lab3.nrating.types

import com.handtruth.net.lab3.types.readVarInt
import com.handtruth.net.lab3.types.readVarString
import com.handtruth.net.lab3.types.writeVarInt
import com.handtruth.net.lab3.types.writeVarString
import io.ktor.utils.io.core.*

data class Topic(val id: Int, val name: String)

/**
 * Декодирует информацию о теме голосования.
 * @receiver синхронный поток данных, откуда следует считать информацию о теме голосования.
 * @return декодированная информация о теме голосования.
 */
fun Input.readTopic(): Topic {
    val id = readVarInt()
    val name = readVarString()
    return Topic(id, name)
}

/**
 * Кодирует информацию о теме голосования.
 * @receiver синхронный поток, куда следует записать информацию о теме голосования.
 * @param topic информация о теме голосования, которую надо закодировать.
 */
fun Output.writeTopic(topic: Topic) {
    writeVarInt(topic.id)
    writeVarString(topic.name)
}
