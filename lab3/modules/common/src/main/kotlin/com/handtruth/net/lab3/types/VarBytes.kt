package com.handtruth.net.lab3.types

import io.ktor.utils.io.core.*

/**
 * Декодирует последовательность байт произвольного размера.
 * Последовательность байт предваряет поле типа VarInt, которое задает размер.
 * @receiver синхронный поток данных, откуда следует считать последовательность байт
 * @return декодированный массив байт
 */
fun Input.readVarBytes(): ByteArray {
    val size = readVarInt()
    return readBytes(size)
}

/**
 * Кодирует последовательность байт в формате VarBytes.
 * @receiver синхронный поток, куда следует записать закодированную последовательность
 * @param bytes массив байт, который следует записать
 */
fun Output.writeVarBytes(bytes: ByteArray) {
    writeVarInt(bytes.size)
    writeBytes(bytes)
}