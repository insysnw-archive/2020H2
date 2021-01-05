package com.handtruth.net.lab3.sevent.options

import com.google.auto.service.AutoService
import com.handtruth.net.lab3.message.Message
import com.handtruth.net.lab3.options.Option
import com.handtruth.net.lab3.options.OptionCompanion
import com.handtruth.net.lab3.options.toOptions
import com.handtruth.net.lab3.types.*
import com.handtruth.net.lab3.util.MessageFormatException
import io.ktor.utils.io.core.*

/**
 * Идентификатор события, которое участвует в операции.
 *
 * @param eventId значение числового идентификатора
 */
data class EventID(val eventId: Int): Option() {

    override fun write(output: Output) {
        output.writeVarInt(eventId)
    }

    @AutoService(OptionCompanion::class)
    companion object : OptionCompanion(1) {
        override fun read(input: ByteReadPacket) = EventID(input.readVarInt())
    }
}

/**
 * Тип события, который участвует в операции.
 *
 * @param type имя типа события
 */
data class EventType(val type: String): Option() {

    override fun write(output: Output) {
        output.writeString(type)
    }

    @AutoService(OptionCompanion::class)
    companion object : OptionCompanion(2) {
        override fun read(input: ByteReadPacket) = EventType(input.readString(input.remaining.toInt()))
    }
}

/**
 * Идентификаторы события, которые участвуют в операции.
 *
 * @param ids список числовых идентификаторов событий
 */
data class EventIDs(val ids: List<Int>): Option() {

    override fun write(output: Output) {
        output.writeVarList(ids, output::writeVarInt)
    }

    @AutoService(OptionCompanion::class)
    companion object : OptionCompanion(3) {
        override fun read(input: ByteReadPacket) = EventIDs(input.readVarList(input::readVarInt))
    }
}

/**
 * Имена типов событий, которые участвуют в операции.
 *
 * @param types список имён типов событий
 */
data class EventTypes(val types: List<String>): Option() {

    override fun write(output: Output) {
        output.writeVarList(types, output::writeVarString)
    }

    @AutoService(OptionCompanion::class)
    companion object : OptionCompanion(4) {
        override fun read(input: ByteReadPacket) = EventTypes(input.readVarList(input::readVarString))
    }
}

/**
 * Получить id события из опций сообщения.
 *
 * @receiver сообщение, где следует искать опцию [EventID]
 * @return id события
 */
fun Message.getEventId() = getOption<EventID>().eventId

private fun <T> itemsToList(item: T?, items: List<T>?): List<T> {
    return if (item != null && items != null) {
        items + item
    } else if (item != null) {
        listOf(item)
    } else {
        items ?: emptyList()
    }
}

/**
 * Получить типы событий из опций [EventType] и [EventTypes]. Независимо
 * от наличия опций вернёт список имён из тех опций, что были найдены.
 * Если не было найдено ни одной опции, то вернёт пустой список.
 *
 * @receiver сообщение, где следует искать опции [EventType] и [EventTypes]
 * @return список имён типов событий
 */
fun Message.getEventTypes(): List<String> {
    val type = getOptionOrNull<EventType>()
    val types = getOptionOrNull<EventTypes>()
    return itemsToList(type?.type, types?.types)
}

/**
 * Получить идентификаторы событий из опций [EventID] и [EventIDs]. Независимо
 * от наличия опций вернёт список идентификаторов из тех опций, что были найдены.
 * Если не было найдено ни одной опции, то вернёт пустой список.
 *
 * @receiver сообщение, где следует искать опции [EventID] и [EventIDs]
 * @return список числовых идентификаторов событий
 */
fun Message.getEventIDs(): List<Int> {
    val id = getOptionOrNull<EventID>()
    val ids = getOptionOrNull<EventIDs>()
    return itemsToList(id?.eventId, ids?.ids)
}

/**
 * Выбирает подходящую опцию под список имён событий:
 * 1. выберет опцию [EventType], если в списке имён типов событий только 1 элемент;
 * 2. выберет опцию [EventTypes], если в списке имён типов событий 2 и более элементов;
 * 3. не выберет никакую опцию, если список имён типов событий пуст.
 *
 * @param types список имён типов событий
 * @return ассоциативный массив опций, состоящий из одной выбранной опции или
 * пустой массив, если опция не была выбрана
 */
fun eventTypesToOptions(types: List<String>) = when (types.size) {
    0 -> emptyMap()
    1 -> toOptions(EventType(types.first()))
    else -> toOptions(EventTypes(types))
}

/**
 * Выбирает подходящую опцию под список числовых идентификаторов событий:
 * 1. выберет опцию [EventID], если в списке числовых идентификаторов событий только 1 элемент;
 * 2. выберет опцию [EventIDs], если в списке числовых идентификаторов событий 2 и более элементов;
 * 3. не выберет никакую опцию, если список числовых идентификаторов событий пуст.
 *
 * @param ids список числовых идентификаторов событий
 * @return ассоциативный массив опций, состоящий из одной выбранной опции или
 * пустой массив, если опция не была выбрана
 */
fun eventIdsToOptions(ids: List<Int>) = when (ids.size) {
    0 -> emptyMap()
    1 -> toOptions(EventID(ids.first()))
    else -> toOptions(EventIDs(ids))
}
