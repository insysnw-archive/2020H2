package com.handtruth.net.lab3.types

import com.handtruth.net.lab3.util.validate
import io.ktor.utils.io.core.*

/**
 * Декодирует последовательность байт заданного размера.
 * @receiver синхронный поток данных, откуда следует считать последовательность байт
 * @return декодированный массив байт размером [size]
 */
fun Input.readBytes(size: Int): ByteArray {
    validate(size >= 0) { "Size value is negative" }
    val result = ByteArray(size)
    readFully(result)
    return result
}

/**
 * Кодирует последовательность байт.
 * @receiver синхронный поток, куда следует записать закодированную последовательность
 * @param bytes массив байт, который следует записать
 */
fun Output.writeBytes(bytes: ByteArray) {
    writeFully(bytes)
}

