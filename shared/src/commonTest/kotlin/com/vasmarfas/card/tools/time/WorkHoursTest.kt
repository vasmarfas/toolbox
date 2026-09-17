package com.vasmarfas.card.tools.time

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WorkHoursTest {
    @Test
    fun shiftsWithBreaksAndOvernight() {
        assertEquals(480, WorkHours.shiftMinutes(9 * 60, 18 * 60, 60))
        assertEquals(420, WorkHours.shiftMinutes(22 * 60, 6 * 60, 60))
        assertEquals(0, WorkHours.shiftMinutes(9 * 60, 9 * 60 + 30, 60))
        assertEquals(510, Shift("09:00", "18:00", "30").minutes)
        assertNull(Shift("9", "18:00", "0").minutes)
        assertEquals("8:30", formatHm(510))
        assertEquals(8.5, WorkHours.decimalHours(510))
    }
}
