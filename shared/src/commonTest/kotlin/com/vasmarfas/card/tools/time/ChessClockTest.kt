package com.vasmarfas.card.tools.time

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ChessClockTest {
    @Test
    fun firstPressStartsTheOpponent() {
        val clock = ChessClock(60_000, 2_000).press(ChessSide.BOTTOM, 1_000)
        assertEquals(ChessSide.TOP, clock.turn)
        assertTrue(clock.running)
        assertEquals(55_000, clock.remaining(ChessSide.TOP, 6_000))
        assertEquals(60_000, clock.remaining(ChessSide.BOTTOM, 6_000))
    }

    @Test
    fun moveAddsIncrementAndSwitches() {
        var clock = ChessClock(60_000, 2_000).press(ChessSide.BOTTOM, 0)
        clock = clock.press(ChessSide.TOP, 10_000)
        assertEquals(52_000, clock.top)
        assertEquals(1, clock.moves(ChessSide.TOP))
        assertEquals(ChessSide.BOTTOM, clock.turn)
        assertSame(clock, clock.press(ChessSide.TOP, 11_000))
        assertEquals(57_000, clock.remaining(ChessSide.BOTTOM, 13_000))
    }

    @Test
    fun pauseKeepsTheTime() {
        var clock = ChessClock(60_000, 0).press(ChessSide.BOTTOM, 0)
        clock = clock.pause(20_000)
        assertFalse(clock.running)
        assertEquals(40_000, clock.remaining(ChessSide.TOP, 90_000))
        assertSame(clock, clock.press(ChessSide.TOP, 90_000))
        clock = clock.resume(100_000)
        assertEquals(35_000, clock.remaining(ChessSide.TOP, 105_000))
    }

    @Test
    fun flagFallsAtZero() {
        var clock = ChessClock(3_000, 0).press(ChessSide.BOTTOM, 0)
        assertNull(clock.tick(2_999).flagged)
        clock = clock.tick(3_000)
        assertEquals(ChessSide.TOP, clock.flagged)
        assertFalse(clock.running)
        assertEquals(0, clock.top)
        assertSame(clock, clock.press(ChessSide.TOP, 4_000))
        assertSame(clock, clock.resume(4_000))
    }

    @Test
    fun timeFormat() {
        assertEquals("5:00", formatChessTime(300_000))
        assertEquals("1:05:00", formatChessTime(3_900_000))
        assertEquals("0:19.9", formatChessTime(19_950))
        assertEquals("0:00.0", formatChessTime(0))
        assertEquals("0:20", formatChessTime(20_000))
    }
}
