package com.vasmarfas.card.tools.time

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CountdownTimerTest {
    @Test
    fun stateSurvivesTheRoundTrip() {
        val state = TimerState(pomodoro = true, running = true, started = true, endAt = 1_700_000_060_000, total = 60_000, work = false, cycles = 3)
        assertEquals(state, TimerState.decode(state.encode()))
        assertNull(TimerState.decode("true;false"))
    }

    @Test
    fun aMinuteMoreMovesTheEndOrThePause() {
        val running = TimerState(running = true, started = true, endAt = 10_000, total = 5_000).plus(60_000)
        assertEquals(70_000, running.endAt)
        assertEquals(65_000, running.total)
        val paused = TimerState(started = true, remaining = 2_000, total = 5_000).plus(60_000)
        assertEquals(62_000, paused.left(0))
    }

    @Test
    fun everyFourthBreakIsLong() {
        assertTrue(TimerState.isLongBreak(work = false, cycles = 4, longBreakMs = 900_000))
        assertFalse(TimerState.isLongBreak(work = false, cycles = 3, longBreakMs = 900_000))
        assertFalse(TimerState.isLongBreak(work = false, cycles = 4, longBreakMs = 0))
        assertFalse(TimerState.isLongBreak(work = true, cycles = 4, longBreakMs = 900_000))
    }
}
