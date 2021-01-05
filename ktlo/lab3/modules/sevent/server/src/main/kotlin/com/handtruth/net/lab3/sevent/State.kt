package com.handtruth.net.lab3.sevent

import com.handtruth.kommon.Log
import com.handtruth.kommon.default
import com.handtruth.net.lab3.sevent.types.FullEvent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Состояние сервера, которое сохраняется в момент остановки сервера и
 * загружается при его запуске из указанного файла. Для хранения состояния используется формат JSON.
 *
 * @property nextId следующее значение id события, которое будет присвоено новому событию
 * @property events список событий, которые существуют на момент остановки сервера
 */
@Serializable
data class State(val nextId: Int, val events: List<FullEvent>) {
    companion object {
        private val json = Json { prettyPrint = true }

        private val log = Log.default("sevent/server/state")

        /**
         * Загрузка состояния из указанного JSON файла. Если файл не был найден, то загружается пустое состояние.
         * Пустое состояние имеет значения [nextId] = 0 и [events] = [emptyList]\().
         *
         * @param file путь до файла
         * @return загруженное состояние
         */
        fun load(file: File): State {
            if (file.exists()) {
                try {
                    log.info { "found saved state in file $file, trying to load it..." }
                    return json.decodeFromString(serializer(), file.readText())
                } catch (e: Exception) {
                    log.error(e) { "failed to load saved state!" }
                }
            }
            log.info { "no saved state loaded, creating new empty state" }
            return State(0, emptyList())
        }
    }

    /**
     * Сохраняет это состояние в файл.
     *
     * @param file путь до файла
     */
    fun save(file: File) {
        try {
            val jsonString = json.encodeToString(serializer(), this)
            file.writeText(jsonString)
            log.info { "state saved successfully!" }
        } catch (e: Exception) {
            log.error(e) { "failed to save server state!" }
        }
    }
}
