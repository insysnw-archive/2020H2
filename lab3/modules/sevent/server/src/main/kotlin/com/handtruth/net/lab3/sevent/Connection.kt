package com.handtruth.net.lab3.sevent

import com.handtruth.kommon.Log
import com.handtruth.kommon.default
import com.handtruth.kommon.getLog
import com.handtruth.net.lab3.message.Message
import com.handtruth.net.lab3.message.transmitter
import com.handtruth.net.lab3.options.toOptions
import com.handtruth.net.lab3.sevent.message.*
import com.handtruth.net.lab3.sevent.options.EventIDs
import com.handtruth.net.lab3.sevent.options.EventType
import com.handtruth.net.lab3.sevent.options.EventTypes
import com.handtruth.net.lab3.sevent.options.getEventTypes
import com.handtruth.net.lab3.sevent.types.FullEvent
import com.handtruth.net.lab3.util.ConcurrentSet
import com.handtruth.net.lab3.util.MessageFormatException
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

/**
 * генератор идентификаторов клиентов для удобного отслеживания действий отдельного клиента в логе
 */
private val clientIds = IdGenerator()

/**
 * Класс фильтра событий сессии
 */
private class FilterInfo {
    private val mutex = Mutex()

    private val idsSet = ConcurrentSet<Int>(mutex)
    private val typesSet = ConcurrentSet<String>(mutex)

    /**
     * Фильтрация событий происходит здесь
     */
    operator fun invoke(event: FullEvent): Boolean {
        return event.id in idsSet || event.groups.any { it in typesSet }
    }

    suspend fun forget(event: FullEvent) = idsSet.remove(event.id)

    suspend fun subscribe(events: Collection<FullEvent>, types: Collection<String>) = mutex.withLock {
        idsSet.modifySetUnsafe {
            for (event in events) {
                it += event.id
            }
        }
        typesSet.modifySetUnsafe { it += types }
    }

    suspend fun unsubscribe(events: Iterable<FullEvent>, types: Collection<String>) = mutex.withLock {
        idsSet.modifySetUnsafe {
            for (event in events) {
                it -= event.id
            }
        }
        typesSet.modifySetUnsafe { it -= types }
    }

    suspend fun reset(events: Iterable<FullEvent>, types: Collection<String>) = mutex.withLock {
        idsSet.modifySetUnsafe {
            it.clear()
            for (event in events) {
                it += event.id
            }
        }
        typesSet.modifySetUnsafe {
            it.clear()
            it += types
        }
    }

    val types: Set<String> get() = typesSet

    val ids: Set<Int> get() = idsSet
}

/**
 * Обработка отдельного подключения происходит в этом подпроцессе. Подключение привязано к
 * асинхронным потокам, а не к сокету, что позволяет с удобством тестировать работу
 * сервера до написания клиента.
 *
 * @param recvChannel входящий поток данных от клиента к серверу
 * @param sendChannel исходящий поток данных от сервера к клиенту
 * @param endpoint некая строка с адресом клиента для лога
 */
suspend fun connection(recvChannel: ByteReadChannel, sendChannel: ByteWriteChannel, endpoint: String) {
    val clientId = clientIds.next()
    val coroutineName = "sevent/server/connection#$clientId"
    val log = Log.default(coroutineName)
    try {
        log.info { "connected from $endpoint" }
        val filter = FilterInfo()
        withContext(CoroutineName(coroutineName) + log) {
            // Работа клиента происходит в трёх задачах ниже
            val (receiver, sender) = transmitter(recvChannel, sendChannel)
            notificator(sender, filter)
            respondent(receiver, sender, filter)
        }
    } catch (e: CancellationException) {
        // К сожалению операция отмены это всё тот же Exception и его приходится
        // обрабатывать отдельно, несмотря на то, что это не ошибка
        throw e
    } catch (e: ClosedReceiveChannelException) {
        // Нормальная ситуация при отключении клиента
        log.info { "disconnecting" }
    } catch (e: Exception) {
        // Ошибка, не позволяющая дальнейшую работу
        log.error(e) { "client error, disconnecting..." }
    }
}

// Задача, которая отсылает клиенту уведомления о произошедших событиях
private fun CoroutineScope.notificator(sender: SendChannel<Message>, filter: FilterInfo) = launch {
    val log = getLog()
    val repository = getServerContext().repository
    repository.eventBus.collect { event ->
        if (filter(event)) {
            sender.send(Notify(event))
        }
        if (event.repeat == 1 && filter.forget(event)) {
            log.verbose { "excluded event $event from filter" }
        }
    }
}

// Задача, которая отвечает на запросы клиента
private fun CoroutineScope.respondent(
    receiver: ReceiveChannel<Message>,
    sender: SendChannel<Message>,
    filter: FilterInfo
) = launch {
    val log = getLog()
    val (idGenerator, repository, minimalPeriod) = getServerContext()
    for (message in receiver) {
        log.info { "request: $message" }
        val response: Message = try {
            when (message) {
                is RegisterEvent -> {
                    require(minimalPeriod < message.event.period) { "period value is too low" }
                    val event = message.getFullEvent(idGenerator.next(), Clock.System.now())
                    if (event.repeat != 0) {
                        repository.register(event)
                    }
                    EventRegistration(event)
                }
                is ListEvents -> {
                    val cond = message.getOptionOrNull<EventType>() == null &&
                            message.getOptionOrNull<EventTypes>() == null
                    val response = if (cond) {
                        repository.events.map { it.id }
                    } else {
                        val groups = message.getEventTypes()
                        val set = mutableSetOf<Int>()
                        for (groupName in groups) {
                            val group = repository[groupName]
                            for (event in group) {
                                set += event.id
                            }
                        }
                        set.toList()
                    }
                    ListedEvents(toOptions(EventIDs(response)))
                }
                is DeleteEvent -> {
                    val ids = message.eventIds
                    val groups = message.eventTypes
                    val toRemove = mutableSetOf<FullEvent>()
                    for (id in ids) {
                        repository.getOrNull(id)?.let { toRemove += it }
                    }
                    for (group in groups) {
                        toRemove += repository[group]
                    }
                    repository.delete(toRemove)
                    DeletedEvents(toOptions(EventIDs(toRemove.map { it.id })))
                }
                is Subscribe -> {
                    val ids = message.eventIds
                    val groups = message.eventTypes
                    filter.subscribe(ids.map { repository[it] }, groups)
                    FilterUpdated()
                }
                is Unsubscribe -> {
                    val ids = message.eventIds
                    val groups = message.eventTypes
                    filter.unsubscribe(ids.map { repository[it] }, groups)
                    FilterUpdated()
                }
                is GetFilter -> {
                    Filter(filter.ids.toList(), filter.types.toList())
                }
                is Filter -> {
                    val ids = message.eventIds
                    val groups = message.eventTypes
                    filter.reset(ids.map { repository[it] }, groups)
                    FilterUpdated()
                }
                is GetEvent -> {
                    val event = repository[message.eventId]
                    EventInfo(event)
                }
                else -> {
                    Error(message.id, Error.Codes.WrongMessage, "wrong message")
                }
            }
        } catch (e: IllegalArgumentException) {
            log.error(e)
            Error(message.id, Error.Codes.InvalidProperty, e.message ?: "invalid property")
        } catch (e: MessageFormatException) {
            log.error(e)
            Error(message.id, Error.Codes.FormatError, e.message ?: "format error")
        } catch (e: EventNotExistsException) {
            log.error(e)
            Error(message.id, Error.Codes.EventNotExists, e.message ?: "event not exists")
        } catch (e: CancellationException) {
            // do nothing
            throw e
        } catch (e: Exception) {
            log.error(e)
            Error(message.id, Error.Codes.InternalError, "internal error")
        }
        log.info { "response: $response" }
        sender.send(response)
    }
}
