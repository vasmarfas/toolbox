package com.vasmarfas.card.tools.time

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class CalendarMathTest {
    @Test
    fun gridStartsOnMondayAndCoversMonth() {
        val grid = CalendarMath.monthGrid(2026, 9)
        assertEquals(5, grid.size)
        assertEquals(LocalDate(2026, 8, 31), grid.first().first())
        assertEquals(LocalDate(2026, 10, 4), grid.last().last())
        assertEquals(4, CalendarMath.monthGrid(2021, 2).size)
    }

    @Test
    fun monthShiftWrapsYears() {
        assertEquals(2027 to 1, CalendarMath.shiftMonth(2026, 12, 1))
        assertEquals(2025 to 12, CalendarMath.shiftMonth(2026, 1, -1))
    }
}
