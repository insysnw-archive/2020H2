package com.handtruth.net.lab3.util

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Исключение, которое следует кидать в случае, когда есть нарушение в формате полученного сообщения.
 * @param message сообщение с пояснениями об ошибке
 */
class MessageFormatException(message: String) : IllegalStateException(message)

/**
 * Вспомогательная функция для удобной проверки формата сообщения.
 * @param value условие, которое следует проверить на истинность
 * @param message генератор сообщения, которое будет положено в [MessageFormatException] случае возникновения ошибки
 */
inline fun validate(value: Boolean, message: () -> Any) {
    contract {
        callsInPlace(message, InvocationKind.AT_MOST_ONCE)
        returns() implies value
    }
    if (!value) {
        throw MessageFormatException(message().toString())
    }
}

/**
 * Вспомогательная функция для удобной проверки формата сообщения.
 * @param value значение, которое будет проверено на отсутствие null
 * @param message генератор сообщения, которое будет положено в [MessageFormatException] случае возникновения ошибки
 * @return значение, переданное в параметр [value], но гарантированно не равное null
 */
inline fun <T : Any> validateNotNull(value: T?, message: () -> Any): T {
    contract {
        callsInPlace(message, InvocationKind.AT_MOST_ONCE)
        returns() implies (value != null)
    }
    if (value == null) {
        throw MessageFormatException(message().toString())
    } else {
        return value
    }
}
