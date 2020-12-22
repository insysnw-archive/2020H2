package com.handtruth.net.lab3.sevent.types

import com.handtruth.net.lab3.types.readVarInt
import com.handtruth.net.lab3.types.readVarString
import com.handtruth.net.lab3.types.writeVarInt
import com.handtruth.net.lab3.types.writeVarString
import io.ktor.utils.io.core.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlin.time.Duration
import kotlin.time.seconds

/**
 * Декодирование временного интервала из синхронного потока.
 *
 * @receiver синхронный поток
 * @return временной интервал
 */
fun Input.readDuration() = readDouble().seconds

/**
 * Кодирование временного интервала в синхронный поток.
 *
 * @receiver синхронный поток
 * @param value временной интервал
 */
fun Output.writeDuration(value: Duration) = writeDouble(value.inSeconds)

/**
 * Общие параметры события.
 */
interface Event {

    /**
     * описание события
     */
    val description: String

    /**
     * время до следующего оповещения с момента последнего обновления или создания
     */
    val period: Duration

    /**
     * количество оставшихся повторов этого события или -1, если повторов бесконечно много
     */
    val repeat: Int
}

/**
 * Реализация интерфейса [Event] для задания параметров нового события.
 */
data class EventParameters(
    override val description: String,
    override val period: Duration,
    override val repeat: Int
) : Event

/**
 * Полная информация о событии в таком виде, в каком она хранится на сервере.
 *
 * @property id уникальный числовой идентификатор события
 * @property groups типы, к котором принадлежит это событие
 * @property lastUpdate время создания или последнего обновления данных этого события
 */
@Serializable(FullEvent.Serializer::class)
data class FullEvent(
    val id: Int,
    val groups: List<String>,
    val lastUpdate: Instant,
    override val description: String,
    override val period: Duration,
    override val repeat: Int
) : Event {

    init {
        require(repeat >= -1) { "repeat parameter should be >= -1" }
    }

    /**
     * время следующего оповещения о событии и его обновлении
     */
    val nextUpdate get() = lastUpdate + period

    /**
     * Обновление события и получение новой версии события.
     * Возвращает null, если количество повторов события достигло 0
     *
     * @return новая версия этого события
     */
    fun fire(): FullEvent? {
        val repeat = if (repeat == -1) -1 else repeat - 1
        if (repeat == 0)
            return null
        return copy(repeat = repeat, lastUpdate = nextUpdate)
    }

    /**
     * Вручную написанный сериализатор для [FullEvent]. Обычно такие вещи писать вручную не надо,
     * но kotlinx.serialization пока не умеет работать с inline классами, коим является [Duration].
     *
     * Этот сериализатор используется в классе State в модуле sevent-server.
     */
    object Serializer : KSerializer<FullEvent> {
        private val stringListSerializer = ListSerializer(String.serializer())

        override val descriptor = buildClassSerialDescriptor("FullEvent") {
            element("id", Int.serializer().descriptor)
            element("groups", stringListSerializer.descriptor, isOptional = true)
            element("lastUpdate", String.serializer().descriptor, isOptional = true)
            element("description", String.serializer().descriptor, isOptional = true)
            element("period", Double.serializer().descriptor)
            element("repeat", Int.serializer().descriptor, isOptional = true)
        }

        override fun deserialize(decoder: Decoder): FullEvent = decoder.decodeStructure(descriptor) {
            var id: Int? = null
            var groups: List<String> = emptyList()
            var lastUpdate = Clock.System.now()
            var description = ""
            var period: Duration? = null
            var repeat = 1

            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    DECODE_DONE -> break
                    0 -> id = decodeIntElement(descriptor, 0)
                    1 -> groups = decodeSerializableElement(descriptor, 1, stringListSerializer)
                    2 -> lastUpdate = Instant.parse(decodeStringElement(descriptor, 2))
                    3 -> description = decodeStringElement(descriptor, 3)
                    4 -> period = decodeDoubleElement(descriptor, 4).seconds
                    5 -> repeat = decodeIntElement(descriptor, 5)
                    else -> throw SerializationException("Unexpected index $index")
                }
            }
            FullEvent(
                id = id ?: throw SerializationException("id should be specified"),
                groups = groups,
                lastUpdate = lastUpdate,
                description = description,
                period = period ?: throw SerializationException("period should be specified"),
                repeat = repeat
            )
        }

        override fun serialize(encoder: Encoder, value: FullEvent) = encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.id)
            encodeSerializableElement(descriptor, 1, stringListSerializer, value.groups)
            encodeStringElement(descriptor, 2, value.lastUpdate.toString())
            encodeStringElement(descriptor, 3, value.description)
            encodeDoubleElement(descriptor, 4, value.period.inSeconds)
            encodeIntElement(descriptor, 5, value.repeat)
        }
    }
}

/**
 * Дополнить событие до [FullEvent], указав недостающие параметры.
 *
 * @param id уникальный числовой идентификатор события
 * @param groups типы, к котором принадлежит это событие
 * @param lastUpdate время создания или последнего обновления данных этого события
 */
fun Event.describe(id: Int, groups: List<String>, lastUpdate: Instant) =
    FullEvent(id, groups, lastUpdate, description, period, repeat)

/**
 * Декодирование общих параметров события из асинхронного потока.
 *
 * @receiver синхронный поток
 * @return общие параметры события
 */
fun Input.readEvent() = EventParameters(readVarString(), readDuration(), readVarInt())

/**
 * Кодирование общих параметров события в синхронный поток.
 *
 * @receiver синхронный поток
 * @param value параметры события
 */
fun Output.writeEvent(value: Event) {
    writeVarString(value.description)
    writeDuration(value.period)
    writeVarInt(value.repeat)
}
