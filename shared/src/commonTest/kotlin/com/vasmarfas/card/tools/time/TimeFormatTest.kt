package com.vasmarfas.card.tools.time

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TimeFormatTest {
    @Test
    fun stopwatchFormat() {
        assertEquals("00:00.000", formatStopwatch(0))
        assertEquals("01:05.042", formatStopwatch(65_042))
        assertEquals("1:00:00.000", formatStopwatch(3_600_000))
    }

    @Test
    fun countdownRoundsUp() {
        assertEquals("00:01", formatCountdown(1))
        assertEquals("00:00", formatCountdown(0))
        assertEquals("1:30:00", formatCountdown(5_400_000))
    }

    @Test
    fun lapSplitsAreDifferences() {
        val laps = lapSplits(listOf(1000L, 2500L, 2600L))
        assertEquals(listOf(1000L, 1500L, 100L), laps.map { it.lap })
        assertEquals(3, laps.last().index)
    }

    @Test
    fun parseTime() {
        assertEquals(9 * 60 + 30, parseHhMm("9:30"))
        assertEquals(0, parseHhMm("00:00"))
        assertNull(parseHhMm("24:00"))
        assertNull(parseHhMm("abc"))
    }
}
