package com.handtruth.net.lab3.nrating.types

import io.ktor.utils.io.core.*

/**
 * Декодирует значение типа Bool.
 * @receiver синхронный поток данных, откуда следует считать Bool
 * @return декодированное значение типа Bool.
 */
fun Input.readBool(): Boolean {
    val b = readByte()
    return b != 0.toByte()
}

/**
 * Кодирует значение типа Bool.
 * @receiver синхронный поток, куда следует записать Bool
 * @param bool значение, которое надо закодировать
 */
fun Output.writeBool(bool: Boolean) = if (bool) writeByte(1) else writeByte(0)