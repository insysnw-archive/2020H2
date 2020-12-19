package com.handtruth.net.lab3.nrating.types

import com.handtruth.net.lab3.types.readVarInt
import com.handtruth.net.lab3.types.readVarString
import com.handtruth.net.lab3.types.writeVarInt
import com.handtruth.net.lab3.types.writeVarString
import io.ktor.utils.io.core.*


data class RatingItem(val altId: Int, val altName: String, val votesAbs: Int, val votesRel: Double)

/**
 * Декодирует строчку рейтинга.
 * @receiver синхронный поток данных, откуда следует считать строчку рейтинга.
 * @return декодированная строчка рейтинга.
 */
fun Input.readRatingItem(): RatingItem {
    val altId = readVarInt()
    val altName = readVarString()
    val votesAbs = readInt()
    val votesRel = readDouble()

    return RatingItem(altId, altName, votesAbs, votesRel)
}

/**
 * Кодирует строчку рейтинга.
 * @receiver синхронный поток, куда следует записать строчку рейтинга.
 * @param ratingItem строчка рейтинга, которую надо закодировать.
 */
fun Output.writeRatingItem(ratingItem: RatingItem) {
    writeVarInt(ratingItem.altId)
    writeVarString(ratingItem.altName)
    writeInt(ratingItem.votesAbs)
    writeDouble(ratingItem.votesRel)
}
