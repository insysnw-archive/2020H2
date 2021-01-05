package com.handtruth.net.lab3.sample

import com.google.auto.service.AutoService
import com.handtruth.net.lab3.options.*
import com.handtruth.net.lab3.types.*
import io.ktor.utils.io.core.*

fun sampleOption() {
    // Пример использования классов Option и OptionCompanion
    data class SampleOption(val integer: Int, val string: String) : Option() {
        override fun write(output: Output) {
            output.writeVarInt(integer)
        }

        // Эта аннотация добавляет имя класса этого companion объекта в
        // соответствующий файл в папке META-INF жарника. В рантайме все имена считываются
        // и классы загружаются текущим класслоадером приложения. Объекты OptionCompanion
        // в результате лежат в ассоциативном массиве allOptions.
        @AutoService(OptionCompanion::class)
        companion object : OptionCompanion(23) {
            override fun read(input: ByteReadPacket) = with(input) {
                SampleOption(readVarInt(), readVarString())
            }
        }
    }
}
