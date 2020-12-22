package com.handtruth.net.lab3.sevent

import com.handtruth.kommon.Log
import com.handtruth.kommon.default
import com.handtruth.net.lab3.sevent.types.FullEvent
import com.handtruth.net.lab3.util.ConcurrentMap
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlin.coroutines.CoroutineContext

/**
 * Объект, осуществляющий хранение и обработку событий.
 *
 * @param coroutineContext контекст, в котором следует запускать задачи ожидания оповещения события
 * @param events изначальный список событий при старте системы (получен из загруженного состояния)
 */
class Repository(
    coroutineContext: CoroutineContext,
    events: List<FullEvent>
): CoroutineScope {
    private val log = Log.default("sevent/server/repository")

    override val coroutineContext =
        coroutineContext + CoroutineName("sevent/server/repository") + Job(coroutineContext[Job]) + log

    private val _eventBus = MutableSharedFlow<FullEvent>()
    val eventBus: SharedFlow<FullEvent> get() = _eventBus

    /**
     * Мьютекс управляющий секциями изменения состояния в объекте репозитория
     * (создание, удаление и обновление событий).
     */
    private val mutex = Mutex()

    private val eventGroups = ConcurrentMap<String, List<FullEvent>>(mutex)
    private val eventById = ConcurrentMap<Int, FullEvent>(mutex)

    private val eventJobs = mutableMapOf<Int, Job>()

    /**
     * список всех событий в системе
     */
    val events: Collection<FullEvent> get() = eventById.values

    /**
     * Получить событие по числовому идентификатору, если событие
     * с таким идентификатором существует в системе.
     *
     * @param id числовой идентификатор события
     * @return полное событие или null
     */
    fun getOrNull(id: Int) = eventById[id]

    /**
     * Получить событие по числовому идентификатору, если событие
     * с таким идентификатором существует в системе иначе выбросить исключение [EventNotExistsException].
     *
     * @param id числовой идентификатор события
     * @return полное событие
     * @throws EventNotExistsException
     */
    operator fun get(id: Int) = getOrNull(id) ?: throw EventNotExistsException("no event with id #$id")

    /**
     * Получить все события, которые принадлежат указанному типу.
     *
     * @param group имя типа события
     * @return список событий, принадлежащих этому типу
     */
    operator fun get(group: String) = eventGroups[group] ?: emptyList()

    private fun registerEvent(event: FullEvent) {
        if (eventById.modifyMapUnsafe{ it.put(event.id, event) } != null) {
            log.fatal { "event with such id already registered" }
        }
        for (groupName in event.groups) {
            val group = eventGroups[groupName]
            val newGroup = if (group == null) {
                listOf(event)
            } else {
                group + event
            }
            eventGroups.modifyMapUnsafe { it[groupName] = newGroup }
        }
        eventJobs[event.id] = launch {
            val time = event.nextUpdate - Clock.System.now()
            if (time.isPositive()) {
                delay(time)
            }
            _eventBus.emit(event)
            val next = event.fire()
            withContext(NonCancellable) {
                if (next == null) {
                    delete(event)
                } else {
                    update(next)
                }
            }
        }
    }

    private fun deleteEvent(event: FullEvent) {
        val oldEvent = eventById.modifyMapUnsafe { it.remove(event.id) }
        if (oldEvent == null) {
            log.error { "this event already removed" }
            return
        }
        if (event !== oldEvent) {
            log.fatal { "removed event is not an expected one" }
        }
        for (groupName in event.groups) {
            val group = eventGroups[groupName] ?: log.fatal { "event group does not exists" }
            if (group.size == 1) {
                if (event !in group) {
                    log.fatal { "there is no such event in group $groupName" }
                }
                eventGroups.modifyMapUnsafe { it.remove(groupName) }
            } else {
                val newGroup = group - event
                if (newGroup.size == group.size) {
                    log.fatal { "there is no such event in group $groupName" }
                }
                eventGroups.modifyMapUnsafe { it[groupName] = newGroup }
            }
        }
        val job = eventJobs.remove(event.id) ?: log.fatal { "no job associated with the event" }
        job.cancel()
    }

    /**
     * Создать новое событие в системе.
     *
     * @param event все параметры нового события
     */
    suspend fun register(event: FullEvent) = mutex.withLock {
        registerEvent(event)
    }

    private suspend fun delete(event: FullEvent) = mutex.withLock {
        deleteEvent(event)
    }

    private suspend fun update(event: FullEvent) = mutex.withLock {
        val oldEvent = eventById[event.id] ?: log.fatal { "no such event" }
        deleteEvent(oldEvent)
        registerEvent(event)
    }

    /**
     * Удалить список событий из системы
     *
     * @param events список удаляемых событий
     */
    suspend fun delete(events: Iterable<FullEvent>) = mutex.withLock {
        events.asSequence()
            .mapNotNull { getOrNull(it.id) } // Проверка, не пропало ли к этому моменту одно из событий
            .forEach { deleteEvent(it) }
    }

    init {
        // Инициализация. В процессе реализуется последовательное обновление
        // события до текущего системного времени. Не исключено, что таким образом
        // некоторые события могут пропасть из системы, так как repeat стал равным 0.
        val now = Clock.System.now()
        for (event in events) {
            var current: FullEvent? = event
            while (current != null && current.nextUpdate < now) {
                log.verbose { "drop event $event" }
                current = current.fire()
            }
            if (current != null) {
                registerEvent(current)
            }
        }
    }
}
