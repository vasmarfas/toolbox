package com.vasmarfas.card.tools.time

import kotlin.math.abs
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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

    fun encode(zones: List<String>): String = zones.joinToString(",")

    fun decode(text: String?): List<String>? =
        text?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.takeIf { it.isNotEmpty() }
}
