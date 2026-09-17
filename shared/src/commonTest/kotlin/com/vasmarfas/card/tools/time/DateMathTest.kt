package com.vasmarfas.card.tools.time

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DateMathTest {
    @Test
    fun differenceAndWorkingDays() {
        val d = DateMath.difference(LocalDate(2024, 6, 17), LocalDate(2024, 6, 24))
        assertEquals(7, d.totalDays)
        assertEquals(1, d.weeks)
        assertEquals(5, d.workingDays)
        assertEquals(10, DateMath.workingDays(LocalDate(2024, 6, 19), LocalDate(2024, 7, 3)))
        assertEquals(0, DateMath.workingDays(LocalDate(2024, 6, 22), LocalDate(2024, 6, 24)))
        val p = DateMath.difference(LocalDate(2025, 3, 10), LocalDate(2024, 1, 5))
        assertEquals(1, p.years)
        assertEquals(2, p.months)
        assertEquals(5, p.days)
    }

    @Test
    fun isoWeeks() {
        assertEquals(2024 to 25, DateMath.isoWeek(LocalDate(2024, 6, 21)))
        assertEquals(2020 to 53, DateMath.isoWeek(LocalDate(2021, 1, 1)))
        assertEquals(2025 to 1, DateMath.isoWeek(LocalDate(2024, 12, 30)))
        assertEquals(2026 to 38, DateMath.isoWeek(LocalDate(2026, 9, 16)))
    }

    @Test
    fun calendarFacts() {
        assertTrue(DateMath.isLeapYear(2024))
        assertFalse(DateMath.isLeapYear(1900))
        assertTrue(DateMath.isLeapYear(2000))
        assertEquals(3, DateMath.quarter(LocalDate(2026, 9, 16)))
        assertEquals(29, DateMath.daysInMonth(2024, 2))
        assertEquals(LocalDate(2024, 3, 31), DateMath.shift(LocalDate(2024, 1, 31), 2, DateUnit.MONTHS, false))
        assertEquals(LocalDate(2024, 6, 14), DateMath.shift(LocalDate(2024, 6, 21), 1, DateUnit.WEEKS, true))
        assertEquals("2026-09-16", LocalDate(2026, 9, 16).iso())
        assertNull(parseDate("2026-13-01"))
        assertEquals(LocalDate(2026, 9, 16), parseDate(" 2026-09-16 "))
    }
}
