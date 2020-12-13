package com.handtruth.net.lab3.types

import com.handtruth.net.lab3.util.validate
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*

private inline fun readVarIntTemplate(readByte: () -> Byte): Int {
    var numRead = 0
    var result = 0
    var read: Int
    do {
        validate(numRead < 5) { "VarInt is too big" }
        read = readByte().toInt()
        val value = read and 127
        result = result or (value shl 7 * numRead)
        ++numRead
    } while (read and 128 != 0)
    return result
}

/**
 * Декодирует целое число в формате [VarInt](protocol.md#varint) с ограничением
 * на максимальный размер 32-ух битного числа.
 * @receiver синхронный поток данных, откуда следует считать число
 * @return декодированное целое 32-ух битное число
 */
fun Input.readVarInt(): Int = readVarIntTemplate { readByte() }

/**
 * Декодирует целое число в формате [VarInt](protocol.md#varint) с ограничением
 * на максимальный размер 32-ух битного числа.
 * @receiver асинхронный поток данных, откуда следует считать число
 * @return декодированное целое 32-ух битное число
 */
suspend fun ByteReadChannel.readVarInt(): Int = readVarIntTemplate { readByte() }

private inline fun writeVarIntTemplate(integer: Int, writeByte: (Byte) -> Unit) {
    var value = integer
    do {
        var temp = (value and 127)
        value = value ushr 7
        if (value != 0) {
            temp = temp or 128
        }
        writeByte(temp.toByte())
    } while (value != 0)
}

/**
 * Кодирует целое 32-ух битное число в формат [VarInt](protocol.md#varint).
 * @receiver синхронный поток, куда следует записать закодированное число
 * @param integer 32-ух битное число, которое следует записать
 */
fun Output.writeVarInt(integer: Int) = writeVarIntTemplate(integer) { writeByte(it) }

/**
 * Кодирует целое 32-ух битное число в формат [VarInt](protocol.md#varint).
 * @receiver асинхронный поток, куда следует записать закодированное число
 * @param integer 32-ух битное число, которое следует записать
 */
suspend fun ByteWriteChannel.writeVarInt(integer: Int) = writeVarIntTemplate(integer) { writeByte(it) }
