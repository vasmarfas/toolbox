package com.vasmarfas.card.tools.time

import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class TimestampsTest {
    @Test
    fun detectsUnits() {
        assertEquals(TimestampUnit.SECONDS, Timestamps.detectUnit(1_700_000_000L))
        assertEquals(TimestampUnit.MILLISECONDS, Timestamps.detectUnit(1_700_000_000_000L))
        assertEquals(TimestampUnit.MICROSECONDS, Timestamps.detectUnit(1_700_000_000_000_000L))
        assertEquals(TimestampUnit.NANOSECONDS, Timestamps.detectUnit(1_700_000_000_000_000_000L))
        assertEquals(1_700_000_000_000L, Timestamps.toEpochMillis(1_700_000_000_000_000L, TimestampUnit.MICROSECONDS))
    }

    @Test
    fun formatsIsoAndOffsets() {
        assertEquals("+03:00", Timestamps.formatOffset(10_800))
        assertEquals("-04:30", Timestamps.formatOffset(-16_200))
        assertEquals("1970-01-01T00:00:00.000Z", Timestamps.iso8601(0, 0))
        assertEquals("2023-11-14T22:13:20.500Z", Timestamps.iso8601(1_700_000_000_500L, 0))
        assertEquals("2023-11-15T01:13:20.500+03:00", Timestamps.iso8601(1_700_000_000_500L, 10_800))
    }

    @Test
    fun wallTimeRoundTrip() {
        val local = LocalDateTime(1970, 1, 1, 3, 0, 0)
        assertEquals(0L, Timestamps.epochSecondsOf(local) { 10_800 })
        assertEquals(local, Timestamps.wallTime(0, 10_800))
        assertEquals("+3", WorldClock.formatRelative(10_800))
        assertEquals("−5:30", WorldClock.formatRelative(-19_800))
        assertEquals(listOf("UTC", "Europe/Moscow"), WorldClock.decode(WorldClock.encode(listOf("UTC", "Europe/Moscow"))))
        assertEquals("New York", WorldClock.shortName("America/New_York"))
    }
}
