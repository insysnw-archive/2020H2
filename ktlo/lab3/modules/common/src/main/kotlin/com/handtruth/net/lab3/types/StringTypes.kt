package com.handtruth.net.lab3.types

import com.handtruth.net.lab3.util.validate
import io.ktor.utils.io.core.*

/**
 * Декодирует строку заданного размера (в байтах).
 * @receiver синхронный поток данных, откуда следует считать последовательность байт
 * @return декодированная строка
 */
fun Input.readString(size: Int): String =
    String(readBytes(size).filter { it != 0x00.toByte() }.toByteArray(), Charsets.UTF_8)

/**
 * Декодирует строку произвольного размера (в формате VarString).
 * @receiver синхронный поток данных, откуда следует считать последовательность байт
 * @return декодированная строка
 */
fun Input.readVarString(): String {
    val size = readVarInt()
    return readString(size)
}

/**
 * Кодирует строку в последовательность октетов заданного размера.
 * Свободное место в конце заполняется нулями (0x00)
 * @receiver синхронный поток, куда следует записать закодированную последовательность
 * @param string строка, которую следует записать
 * @param size размер выходной последовательности байт
 */
fun Output.writeString(string: String, size: Int) {
    val stringBytes = string.toByteArray(Charsets.UTF_8)
    validate(size >= stringBytes.size) { "String is too big for given size" }
    writeBytes(stringBytes)
    if (size > stringBytes.size) {
        writeBytes(ByteArray(size - stringBytes.size) { 0x00 })
    }
}

/**
 * Кодирует строку в последовательность октетов.
 * @receiver синхронный поток, куда следует записать закодированную последовательность
 * @param string строка, которую следует записать
 */
fun Output.writeString(string: String) = writeBytes(string.toByteArray(Charsets.UTF_8))

/**
 * Кодирует строку в последовательность октетов произвольного размера (в формате VarString).
 * @receiver синхронный поток, куда следует записать закодированную последовательность
 * @param string строка, которую следует записать
 */
fun Output.writeVarString(string: String) {
    val size = string.toByteArray(Charsets.UTF_8).size
    writeVarInt(size)
    writeString(string)
}