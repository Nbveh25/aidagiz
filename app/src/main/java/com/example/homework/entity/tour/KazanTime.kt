package com.example.homework.entity.tour

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object KazanTime {
    private val zone = ZoneId.of("Europe/Moscow")
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")

    fun nowIso(): String = ZonedDateTime.now(zone).format(formatter)

    fun formatClock(iso: String?): String? {
        if (iso.isNullOrBlank()) return null
        val time = iso.dropWhile { it != 'T' }.drop(1).take(5)
        return time.takeIf { it.length == 5 && time[2] == ':' }
    }
}
