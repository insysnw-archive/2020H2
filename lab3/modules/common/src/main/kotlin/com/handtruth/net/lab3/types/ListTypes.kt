package com.handtruth.net.lab3.types

import com.handtruth.net.lab3.util.validate
import io.ktor.utils.io.core.*

/**
 * Декодирует список заданной длины.
 * @receiver синхронный поток данных, откуда следует считать список
 * @return декодированный список длиной [size]
 */
fun <T> Input.readList(size: Int, readItem: () -> T): List<T> {
    validate(size >= 0) { "Size value is negative" }
    val result = ArrayList<T>(size)
    for (i in 0 until size) {
        result.add(readItem())
    }
    return result
}

/**
 * Декодирует список произвольной длины в формате VarList.
 * @receiver синхронный поток данных, откуда следует считать список
 * @return декодированный список
 */
fun <T> Input.readVarList(readItem: () -> T): List<T> {
    val size = readVarInt()
    return readList(size, readItem)
}


/**
 * Кодирует список.
 * @receiver синхронный поток данных, куда следует записать список
 * @param list список, который следует записать
 */
fun <T> Output.writeList(list: List<T>, writeItem: (T) -> Unit) = list.forEach(writeItem)

/**
 * Кодирует список в формате VarList.
 * @receiver синхронный поток данных, куда следует записать список
 * @param list список, который следует записать
 */
fun <T> Output.writeVarList(list: List<T>, writeItem: (T) -> Unit) {
    writeVarInt(list.size)
    list.forEach(writeItem)
}
