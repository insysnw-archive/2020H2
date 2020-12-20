package com.handtruth.net.lab3.options

import com.handtruth.kommon.Log
import com.handtruth.kommon.default
import com.handtruth.net.lab3.types.readVarInt
import com.handtruth.net.lab3.types.writeVarInt
import com.handtruth.net.lab3.util.forever
import com.handtruth.net.lab3.util.loadObjects
import com.handtruth.net.lab3.util.moveTo
import io.ktor.utils.io.core.*

private val log = Log.default("common/options")

/**
 * Ассоциативный массив, где составлены пары **option_id** и объект
 * компаньон для каждого типа опции, которые известны приложению.
 * Для того, чтобы в этом массиве появилась опция следует применить
 * аннотацию [com.google.auto.service.AutoService] на объекте компаньоне
 * опции. Также можно вручную прописать в файле сервиса имя класса компаньона опции.
 */
val allOptions = loadObjects<OptionCompanion>().associateBy { it.optionId }

/**
 * Зарезервированный идентификатор типа опции, означающий конец области опций в сообщении.
 */
internal const val END_OF_OPTIONS: Byte = 0

/**
 * Эта функция считывает список опций из сообщения в соответствии с форматом общего протокола.
 * Информация об известных типах опций берётся из значения [allOptions]. Если из потока был
 * считан неизвестный тип опции, то в логе появится соответствующее сообщение.
 *
 * @receiver синхронный поток, из которого будут прочитаны и декодированы опции
 * @return ассоциативный массив декодированных опций
 */
fun Input.readOptions(): Map<Byte, Option> {
    val options = mutableMapOf<Byte, Option>()
    forever {
        val optionId = readByte()
        if (optionId == END_OF_OPTIONS) {
            return options
        }
        val factory = allOptions[optionId]
        val optionSize = readVarInt()
        if (factory == null) {
            discard(optionSize.toLong())
            log.warning { "option type #$optionId not found" }
        } else {
            buildPacket {
                moveTo(this, optionSize)
            }.use { optionBody ->
                options.put(optionId, factory.read(optionBody))
            }
        }
    }
}

/**
 * Эта функция кодирует и записывает переданный список опций в синхронный поток в
 * соответствии с форматом общего протокола. При этом код конца списка опций также записывается.
 *
 * @receiver синхронный поток, куда будут записаны закодированные опции
 * @param options список опций, которые будут закодированы и записаны в поток
 */
fun Output.writeOptions(options: Iterable<Option>) {
    for (option in options) {
        writeByte(option.id)
        buildPacket {
            option.write(this)
        }.use { optionBody ->
            writeVarInt(optionBody.remaining.toInt())
            optionBody.copyTo(this)
        }
    }
    writeByte(END_OF_OPTIONS)
}
