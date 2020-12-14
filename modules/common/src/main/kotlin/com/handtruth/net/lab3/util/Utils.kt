package com.handtruth.net.lab3.util

import io.ktor.utils.io.core.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Ktlo не любит видеть в коде конструкции `while (true) {}`, поэтому сделал функцию,
 * которая это красиво оборачивает. Но смысла в этом мало, конечно же)))).
 *
 * @param block блок кода, который будет вызываться в цикле
 */
inline fun forever(block: () -> Unit): Nothing {
    contract {
        callsInPlace(block, InvocationKind.AT_LEAST_ONCE)
    }
    while (true) {
        block()
    }
}

/**
 * Библиотека ktor-io местами разочаровывает. Это пример такого разочарования.
 * Функция перемещения данных указанного размера из одного потока в другой не существует,
 * а должна. Этой функцией предпринята попытка слегка исправить ситуацию, но она
 * эффективна только в случае перемещения данных между пакетами. Если у нас будет кейс с
 * другими объектами синхронных потоков, то тогда следует провести ревизию этой функции.
 * Ну или не следует, это же всего лишь лаба по сетям. Работает и уже хорошо.
 *
 * @param output место, куда следует перенести данные
 * @param size размер перемещаемых данных
 * @receiver источник перемещаемых данных
 */
fun Input.moveTo(output: Output, size: Int) {
    if (this is ByteReadPacket && output is BytePacketBuilder) {
        output.writePacket(this, size)
    } else {
        // Плохая ситуация. Это можно сделать хорошо, но разработчики Ktor не дают мне много вариантов
        val buffer = ByteArray(size)
        readFully(buffer)
        output.writeFully(buffer)
    }
}
