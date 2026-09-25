package com.vasmarfas.card.tools.time

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StopwatchTest {
    @Test
    fun stateSurvivesTheRoundTrip() {
        val state = StopwatchState(running = true, startedAt = 1_700_000_000_000, accumulated = 12_345, laps = listOf(1_000, 2_500))
        assertEquals(state, StopwatchState.decode(state.encode()))
        assertEquals(StopwatchState(), StopwatchState.decode(StopwatchState().encode()))
        assertNull(StopwatchState.decode("garbage"))
        assertNull(StopwatchState.decode("true;1;2;x"))
    }

    @Test
    fun elapsedCountsOnlyWhileRunning() {
        assertEquals(5_000, StopwatchState(running = true, startedAt = 10_000, accumulated = 2_000).elapsed(13_000))
        assertEquals(2_000, StopwatchState(running = false, startedAt = 10_000, accumulated = 2_000).elapsed(13_000))
    }

    @Test
    fun lapsPasteAsATable() {
        val table = lapsTable(lapSplits(listOf(1_000L, 2_500L)), listOf("#", "Lap", "Total"))
        assertEquals(3, table.lines().size)
        assertEquals(listOf("2", formatStopwatch(1_500), formatStopwatch(2_500)), table.lines()[2].split('\t'))
    }
}
