package com.handtruth.net.lab3.sevent.message

import com.google.auto.service.AutoService
import com.handtruth.net.lab3.message.Message
import com.handtruth.net.lab3.message.MessageCompanion
import com.handtruth.net.lab3.options.Option
import com.handtruth.net.lab3.options.toOptions
import com.handtruth.net.lab3.sevent.options.*
import com.handtruth.net.lab3.sevent.types.*
import com.handtruth.net.lab3.types.readString
import com.handtruth.net.lab3.types.readTimeInstant
import com.handtruth.net.lab3.types.writeString
import com.handtruth.net.lab3.types.writeTime
import io.ktor.utils.io.core.*
import kotlinx.datetime.Instant

/**
 * Сообщение с пустым телом.
 */
abstract class EmptyMessage : Message() {

    final override fun writeBody(output: Output) = Unit
}

/**
 * Сообщение, которое содержит параметры события в теле.
 */
abstract class WithEventMessage : Message() {

    /**
     * общие параметры события в поле этого сообщения
     */
    abstract val event: Event

    /**
     * Дополнить параметры события и список типов события в этом сообщении до полного описания события.
     *
     * @param id числовой идентификатор события
     * @param lastUpdate время создания или последнего обновления события
     */
    fun getFullEvent(id: Int, lastUpdate: Instant): FullEvent {
        return event.describe(id, getEventTypes(), lastUpdate)
    }
}

/**
 * Сообщение является ответом на запросы клиента к серверу для тех случаев,
 * когда запрос выполнить либо не возможно, либо не вышло.
 *
 * @property lastId октет с кодом сообщения запроса, на которое не удалось ответить успешно
 * @property errorCode код ошибки из перечисления типов ошибок
 * @property message текстовое пояснение ошибки
 */
data class Error(val lastId: Byte, val errorCode: Codes, val message: String) : Message() {

    override fun writeBody(output: Output) = with(output) {
        writeByte(lastId)
        writeByte(errorCode.ordinal.toByte())
        writeString(message)
    }

    /**
     * Возможные коды ошибок.
     */
    enum class Codes {
        /**
         * Ошибки не было. Этот код не используется. Оставлен для того, чтобы занять чем-то код 0.
         */
        Success,

        /**
         * Ошибка формата сообщения, полученного от клиента.
         */
        FormatError,

        /**
         * Неверное значение в одном из полей запроса.
         */
        InvalidProperty,

        /**
         * Внутренняя ошибка сервера.
         */
        InternalError,

        /**
         * Событие не существует.
         */
        EventNotExists,

        /**
         * На этот тип сообщений сервер не отвечает.
         */
        WrongMessage
    }

    @AutoService(MessageCompanion::class)
    companion object : MessageCompanion(1) {

        override fun read(input: ByteReadPacket, options: Map<Byte, Option>) = with(input) {
            Error(readByte(), Codes.values()[readByte().toInt()], readString(input.remaining.toInt()))
        }
    }
}

/**
 * Отвечает за регистрация события в системе его может
 * послать клиент в любой момент.
 *
 * Сервер реагирует на опции [EventType] и [EventTypes] для установки
 * типов события. Можно применить несколько раз эти опции в любом сочетании.
 * Сервер должен считать их все и извлечь имена типов события из всех
 * предоставленных опций.
 *
 * @property event данные добавляемого события
 */
data class RegisterEvent(override val options: Map<Byte, Option>, override val event: Event) : WithEventMessage() {

    override fun writeBody(output: Output) = with(output) {
        writeEvent(event)
    }

    /**
     * список имён типов события
     */
    val types get() = getEventTypes()

    @AutoService(MessageCompanion::class)
    companion object : MessageCompanion(2) {

        override fun read(input: ByteReadPacket, options: Map<Byte, Option>) = RegisterEvent(options, input.readEvent())

        operator fun invoke(types: List<String>, event: Event) = RegisterEvent(eventTypesToOptions(types), event)
    }
}

/**
 * Приходит в ответ на запрос [RegisterEvent] в
 * случае успеха. Данное сообщение содержит дополнительные данные о
 * зарегистрированном событии.
 *
 * Это сообщение всегда включает в себя опцию [EventID]
 * с идентификатором зарегистрированного события в ответ на регистрацию
 * которого и было сформировано сообщение [EventRegistration].
 *
 * @property timestamp время создания события секундной точности
 */
data class EventRegistration(override val options: Map<Byte, Option>, val timestamp: Instant) : Message() {

    constructor(event: FullEvent) : this(toOptions(EventID(event.id)), event.lastUpdate)

    override fun writeBody(output: Output) {
        output.writeTime(timestamp)
    }

    /**
     * числовой идентификатор зарегистрированного события
     */
    val eventId get() = getEventId()

    @AutoService(MessageCompanion::class)
    companion object : MessageCompanion(3) {

        override fun read(input: ByteReadPacket, options: Map<Byte, Option>) =
            EventRegistration(options, input.readTimeInstant())
    }
}

/**
 * Запрос на получение списка событий в системе.
 *
 * В ответ на это сообщение сервер отправит список идентификаторов событий, которые
 * присутствуют в системе на данный момент.
 *
 * В сообщение могут включаться опции [EventType] и(или) [EventTypes] в любой
 * комбинации в любом количестве. В этом случае их значения будут использованы
 * для выборки событий по перечисленным в этих опциях типам событий. События не
 * входящие в эту выборку не будут включены в ответное сообщение от сервера.

 * При отсутствии опций [EventType] и(или) [EventTypes] будут выбраны все
 * присутствующие в системе события.
 *
 * Ответом на сообщение [ListEvents] будет сообщение [ListedEvents].
 */
data class ListEvents(override val options: Map<Byte, Option>) : EmptyMessage() {

    @AutoService(MessageCompanion::class)
    companion object : MessageCompanion(4) {

        override fun read(input: ByteReadPacket, options: Map<Byte, Option>) =
            ListEvents(options)

        operator fun invoke(types: List<String>) = ListEvents(eventTypesToOptions(types))
    }
}

/**
 * Это сообщение является ответом сервера на сообщение [ListEvents],
 * которое было послано клиентом.
 *
 * Сообщение [ListedEvents] гарантированно
 * имеет опцию [EventIDs], в которой перечислены все идентификаторы событий,
 * включённые в выборку по параметрам запроса соответствующего сообщения
 * [ListEvents].
 */
data class ListedEvents(override val options: Map<Byte, Option>) : EmptyMessage() {

    /**
     * список числовых идентификаторов событий, которые существуют в системе
     */
    val eventIds get() = getEventIDs()

    @AutoService(MessageCompanion::class)
    companion object : MessageCompanion(5) {

        override fun read(input: ByteReadPacket, options: Map<Byte, Option>) = ListedEvents(options)
    }
}

/**
 * Сообщение запроса на удаление некоторых событий из системы.
 *
 * Сообщение [DeleteEvent] может включать в себя опции [EventID], [EventIDs],
 * [EventType] и(или) [EventTypes] в любой комбинации в любом количестве.
 *
 * Все события, типы которых перечислены в опциях [EventType] и [EventTypes] будут
 * удалены из системы, а из их идентификаторов будет сделан список `list1`.
 *
 * Все события, идентификаторы которых перечислены в опциях [EventID] и [EventIDs]
 * будут удалены из системы, а из идентификаторов удалённых событий будет
 * сделан список `list2`. Если событие с указанным идентификатором не было
 * найдено, то для данного идентификатора не будет сделано операции удаления.
 *
 * В результате успешного выполнения операции по запросу DeleteEvent в ответ
 * сервер вышлет сообщение DeletedEvents в котором будут задействованы
 * идентификаторы из `list1` и `list2`.
 */
data class DeleteEvent(override val options: Map<Byte, Option>) : EmptyMessage() {

    /**
     * список идентификаторов удаляемых событий
     */
    val eventIds get() = getEventIDs()

    /**
     * список типов удаляемых событий
     */
    val eventTypes get() = getEventTypes()

    @AutoService(MessageCompanion::class)
    companion object : MessageCompanion(6) {

        override fun read(input: ByteReadPacket, options: Map<Byte, Option>) = DeleteEvent(options)

        operator fun invoke(ids: List<Int>, types: List<String>) =
            DeleteEvent(eventIdsToOptions(ids) + eventTypesToOptions(types))
    }
}

/**
 * Это сообщение является ответом сервера на сообщение [DeleteEvent],
 * которое было послано клиентом.
 *
 * Сообщение гарантированно включает в себя одну опцию [EventIDs] в которой
 * перечислены идентификаторы удалённых из системы событий.
 */
data class DeletedEvents(override val options: Map<Byte, Option>) : EmptyMessage() {

    /**
     * список числовых идентификаторов удалённых событий
     */
    val eventIds get() = getEventIDs()

    @AutoService(MessageCompanion::class)
    companion object : MessageCompanion(7) {

        override fun read(input: ByteReadPacket, options: Map<Byte, Option>) = DeletedEvents(options)

        operator fun invoke(ids: List<Int>) = DeletedEvents(eventIdsToOptions(ids))
    }
}

/**
 * Запрос на расширение фильтра сессии.
 *
 * Сообщение [Subscribe] может включать в себя опции [EventID], [EventIDs],
 * [EventType] и(или) [EventTypes] в любой комбинации в любом количестве.
 * Эти опции используются для расширения фильтра событий.
 *
 * В случае успеха в ответ на сообщение [Subscribe] сервер вышлет сообщение
 * [FilterUpdated].
 */
data class Subscribe(override val options: Map<Byte, Option>) : EmptyMessage() {

    /**
     * список числовых идентификаторов событий фильтра
     */
    val eventIds get() = getEventIDs()

    /**
     * список имён типов событий фильтра
     */
    val eventTypes get() = getEventTypes()

    @AutoService(MessageCompanion::class)
    companion object : MessageCompanion(8) {

        override fun read(input: ByteReadPacket, options: Map<Byte, Option>) = Subscribe(options)

        operator fun invoke(ids: List<Int>, types: List<String>) =
            Subscribe(eventIdsToOptions(ids) + eventTypesToOptions(types))
    }
}

/**
 * Запрос на умаление фильтра сессии.
 *
 * Сообщение [Unsubscribe] может включать в себя опции [EventID], [EventIDs],
 * [EventType] и(или) [EventTypes] в любой комбинации в любом количестве.
 * Значения в этих опциях будут убраны из текучего фильтра.
 *
 * В случае успеха в ответ на сообщение Subscribe сервер вышлет сообщение
 * [FilterUpdated].
 */
data class Unsubscribe(override val options: Map<Byte, Option>) : EmptyMessage() {

    /**
     * список числовых идентификаторов событий фильтра
     */
    val eventIds get() = getEventIDs()

    /**
     * список имён типов событий фильтра
     */
    val eventTypes get() = getEventTypes()

    @AutoService(MessageCompanion::class)
    companion object : MessageCompanion(9) {

        override fun read(input: ByteReadPacket, options: Map<Byte, Option>) = Unsubscribe(options)

        operator fun invoke(ids: List<Int>, types: List<String>) =
            Unsubscribe(eventIdsToOptions(ids) + eventTypesToOptions(types))
    }
}

/**
 * Данное сообщение будет выслано в случае успеха в ответ на запросы [Subscribe],
 * [Unsubscribe] и [Filter].
 */
data class FilterUpdated(override val options: Map<Byte, Option> = emptyMap()) : EmptyMessage() {

    @AutoService(MessageCompanion::class)
    companion object : MessageCompanion(10) {

        override fun read(input: ByteReadPacket, options: Map<Byte, Option>) = FilterUpdated(options)
    }
}

/**
 * Запрос на получение состояния текущего фильтра сессии.
 *
 * При получении сообщения GetFilter сервер отправляет сообщение [Filter] с
 * информацией о фильтре данной сессии.
 */
data class GetFilter(override val options: Map<Byte, Option> = emptyMap()) : EmptyMessage() {

    @AutoService(MessageCompanion::class)
    companion object : MessageCompanion(11) {

        override fun read(input: ByteReadPacket, options: Map<Byte, Option>) = GetFilter(options)
    }
}

/**
 * Сообщение, несущее в себе информацию о фильтре событий.
 *
 * Может быть как запросом так и ответом.
 *
 * Сообщение [Filter] может включать в себя опции [EventID], [EventIDs],
 * [EventType] и(или) [EventTypes] в любой комбинации в любом количестве при
 * отправке от клиента.
 *
 * Сообщение [Filter] гарантированно включает в себя по одной опции типов
 * [EventTypes] и [EventIDs] в случае отправки со стороны сервера.
 */
data class Filter(override val options: Map<Byte, Option>) : EmptyMessage() {

    /**
     * список числовых идентификаторов событий фильтра
     */
    val eventIds get() = getEventIDs()

    /**
     * список имён типов событий фильтра
     */
    val eventTypes get() = getEventTypes()

    @AutoService(MessageCompanion::class)
    companion object : MessageCompanion(12) {

        override fun read(input: ByteReadPacket, options: Map<Byte, Option>) = Filter(options)

        operator fun invoke(ids: List<Int>, types: List<String>) = Filter(toOptions(EventIDs(ids), EventTypes(types)))
    }
}

/**
 * Запрос на получение полной информации о событии в системе по его числовому идентификатору.
 *
 * Сообщение [GetEvent] должно включать в себя одну опцию [EventID].
 *
 * Сообщение [GetEvent] отправляется клиентом в целях получения подробной
 * информации о конкретном событии в системе. В опции [EventID] следует указать
 * идентификатор запрашиваемого события.
 *
 * Если события с указанным идентификатором не существует, то сервер ответит
 * сообщением [Error] с кодом ошибки [Error.Codes.EventNotExists].
 *
 * В случае успеха сервер отправит в ответ сообщение [EventInfo] с информацией о
 * запрашиваемом событии.
 */
data class GetEvent(override val options: Map<Byte, Option>) : EmptyMessage() {

    constructor(eventId: Int) : this(toOptions(EventID(eventId)))

    /**
     * числовой идентификатор запрашиваемого события
     */
    val eventId get() = getEventId()

    @AutoService(MessageCompanion::class)
    companion object : MessageCompanion(13) {

        override fun read(input: ByteReadPacket, options: Map<Byte, Option>) = GetEvent(options)
    }
}

/**
 * Сообщение, которое содержит полную информацию о событии.
 */
abstract class WithFullEventMessage : WithEventMessage() {

    /**
     * поле сообщения с временем создания или последнего обновления данных события
     */
    abstract val timestamp: Instant

    final override fun writeBody(output: Output) = with(output) {
        writeEvent(event)
        writeTime(timestamp)
    }

    /**
     * числовой идентификатор события в сообщении
     */
    val eventId get() = getEventId()

    /**
     * список имён типов события в этом сообщении
     */
    val eventTypes get() = getEventTypes()

    /**
     * полное событие, составленное на основе данных в этом сообщении
     */
    val fullEvent get() = getFullEvent(eventId, timestamp)
}

/**
 * Полная информация о событии по запросу.
 *
 * Сообщение [EventInfo] является ответом на успешный запрос [GetEvent].
 *
 * Сообщение [EventInfo] гарантированно имеет опцию [EventID], в которой
 * хранится идентификатор, присутствовавший в такой же опции в запросе [GetEvent].
 *
 * Сообщение [EventInfo] гарантированно имеет опцию [EventTypes], в которой
 * перечислены все типы, к которым относится событие, данные которого
 * представлены в этом сообщении.
 */
data class EventInfo(
    override val options: Map<Byte, Option>,
    override val event: Event,
    override val timestamp: Instant
) : WithFullEventMessage() {

    constructor(eventId: Int, types: List<String>, event: Event, timestamp: Instant) :
            this(toOptions(EventID(eventId), EventTypes(types)), event, timestamp)

    constructor(fullEvent: FullEvent) : this(fullEvent.id, fullEvent.groups, fullEvent, fullEvent.lastUpdate)

    @AutoService(MessageCompanion::class)
    companion object : MessageCompanion(14) {

        override fun read(input: ByteReadPacket, options: Map<Byte, Option>) = with(input) {
            EventInfo(options, readEvent(), readTimeInstant())
        }
    }
}

/**
 * Оповещение о произошедшем событии.
 *
 * Сообщение [Notify] гарантированно имеет опцию [EventID], в которой
 * хранится идентификатор произошедшего события.
 *
 * Сообщение [Notify] гарантированно имеет опцию [EventTypes], в которой
 * перечислены все типы, к которым относится событие, данные которого
 * представлены в этом сообщении.
 */
data class Notify(
    override val options: Map<Byte, Option>,
    override val event: Event,
    override val timestamp: Instant
) : WithFullEventMessage() {

    constructor(eventId: Int, types: List<String>, event: Event, timestamp: Instant) :
            this(toOptions(EventID(eventId), EventTypes(types)), event, timestamp)

    constructor(fullEvent: FullEvent) : this(fullEvent.id, fullEvent.groups, fullEvent, fullEvent.lastUpdate)

    @AutoService(MessageCompanion::class)
    companion object : MessageCompanion(15) {

        override fun read(input: ByteReadPacket, options: Map<Byte, Option>) = with(input) {
            Notify(options, readEvent(), readTimeInstant())
        }
    }
}
