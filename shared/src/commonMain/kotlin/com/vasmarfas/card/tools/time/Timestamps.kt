package com.vasmarfas.card.tools.time

import kotlin.math.abs
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

enum class TimestampUnit(val label: String) {
    SECONDS("s"),
    MILLISECONDS("ms"),
    MICROSECONDS("µs"),
    NANOSECONDS("ns"),
}

object Timestamps {
    fun detectUnit(raw: Long): TimestampUnit {
        val a = abs(raw)
        return when {
            a < 100_000_000_000L -> TimestampUnit.SECONDS
            a < 100_000_000_000_000L -> TimestampUnit.MILLISECONDS
            a < 100_000_000_000_000_000L -> TimestampUnit.MICROSECONDS
            else -> TimestampUnit.NANOSECONDS
        }
    }

    fun toEpochMillis(raw: Long, unit: TimestampUnit): Long = when (unit) {
        TimestampUnit.SECONDS -> raw * 1000
        TimestampUnit.MILLISECONDS -> raw
        TimestampUnit.MICROSECONDS -> raw.floorDiv(1_000L)
        TimestampUnit.NANOSECONDS -> raw.floorDiv(1_000_000L)
    }

    fun formatOffset(seconds: Int): String {
        val a = abs(seconds)
        return (if (seconds < 0) "-" else "+") + (a / 3600).pad2() + ":" + ((a % 3600) / 60).pad2()
    }

    fun wallTime(epochMillis: Long, offsetSeconds: Int): LocalDateTime =
        Instant.fromEpochMilliseconds(epochMillis + offsetSeconds * 1000L).toLocalDateTime(TimeZone.UTC)

    fun iso8601(epochMillis: Long, offsetSeconds: Int): String {
        val t = wallTime(epochMillis, offsetSeconds)
        val millis = epochMillis.mod(1000L).toString().padStart(3, '0')
        val zone = if (offsetSeconds == 0) "Z" else formatOffset(offsetSeconds)
        return "${t.date.iso()}T${t.hour.pad2()}:${t.minute.pad2()}:${t.second.pad2()}.$millis$zone"
    }

    fun epochSecondsOf(local: LocalDateTime, offsetAt: (Long) -> Int): Long {
        val utc = local.toInstant(TimeZone.UTC).epochSeconds
        val guess = utc - offsetAt(utc)
        return utc - offsetAt(guess)
    }
}
