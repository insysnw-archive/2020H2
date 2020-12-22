package com.handtruth.net.lab3.sevent

import com.handtruth.net.lab3.sevent.message.Error

/**
 * Исключение, соответствующее коду ошибки [Error.Codes.EventNotExists] в сообщении об ошибке [Error].
 *
 * @param message текстовое описание проблемы
 * @see Error
 */
class EventNotExistsException(message: String) : IllegalStateException(message)
