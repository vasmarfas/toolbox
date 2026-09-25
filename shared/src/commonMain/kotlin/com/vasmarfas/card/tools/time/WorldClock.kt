package com.vasmarfas.card.tools.time

import com.vasmarfas.card.core.Lang
import com.vasmarfas.card.core.Net
import kotlin.math.abs
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

class ClockCity(val en: String, val ru: String, val zone: String) {
    fun name(lang: Lang): String = if (lang == Lang.RU) ru else en
}

object Cities {
    fun parse(text: String): List<ClockCity> = Net.json.parseToJsonElement(text).jsonArray.map { row ->
        val cells = row.jsonArray.map { it.jsonPrimitive.content }
        ClockCity(cells[0], cells[1], cells[2])
    }

    private fun normalized(text: String) = text.lowercase().replace('ё', 'е').replace('-', ' ')

    fun search(cities: List<ClockCity>, query: String): List<ClockCity> {
        val q = normalized(query.trim())
        if (q.length < 2) return emptyList()
        return cities
            .filter { normalized(it.en).contains(q) || normalized(it.ru).contains(q) }
            .sortedBy { if (normalized(it.en).startsWith(q) || normalized(it.ru).startsWith(q)) 0 else 1 }
    }
}

object WorldClock {
    const val PREF_KEY = "worldclock.zones"

    val defaultZones = listOf("UTC", "Europe/Moscow", "Europe/London", "America/New_York", "Asia/Tokyo", "Asia/Dubai", "Asia/Shanghai")

    fun formatRelative(diffSeconds: Int): String {
        if (diffSeconds == 0) return "±0"
        val a = abs(diffSeconds)
        val sign = if (diffSeconds < 0) "−" else "+"
        val h = a / 3600
        val m = (a % 3600) / 60
        return if (m == 0) "$sign$h" else "$sign$h:${m.pad2()}"
    }

    fun isDaytime(hour: Int): Boolean = hour in 6..17

    fun wallTime(epochSeconds: Long, offsetSeconds: Int): LocalDateTime =
        Instant.fromEpochSeconds(epochSeconds + offsetSeconds).toLocalDateTime(TimeZone.UTC)

    fun shortName(zoneId: String): String = zoneId.substringAfterLast('/').replace('_', ' ')

    fun zoneOf(entry: String): String = entry.substringBefore('|')

    fun cityOf(entry: String): String? = entry.substringAfter('|', "").ifEmpty { null }

    fun encode(zones: List<String>): String = zones.joinToString(",")

    fun decode(text: String?): List<String>? =
        text?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.takeIf { it.isNotEmpty() }
}
