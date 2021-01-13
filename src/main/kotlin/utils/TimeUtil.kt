package utils

import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


class TimeUtil {
    companion object {
        fun String.toUnixMinutes(): Int {
            val pattern = "dd.MM.yyyy HH:mm"
            val formatter = DateTimeFormatter.ofPattern(pattern)
            val localDateTime = LocalDateTime.from(formatter.parse(this))
            return (Timestamp.valueOf(localDateTime).toInstant().epochSecond/60).toInt()
        }

        fun currentTime(): Int =
            ((System.currentTimeMillis()/1000)/60).toInt()

    }
}