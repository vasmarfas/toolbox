package com.vasmarfas.card.tools.calculators

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AgeTest {
    private fun at(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0, second: Int = 0) =
        LocalDateTime(LocalDate(year, month, day), LocalTime(hour, minute, second))

    @Test
    fun exactAge() {
        val result = Age.lived(at(2003, 4, 23), at(2026, 9, 16))!!
        assertEquals(23, result.years)
        assertEquals(4, result.months)
        assertEquals(24, result.days)
        assertEquals(280, result.totalMonths)
        assertEquals(1221L, result.totalWeeks)
        assertEquals(8547L, result.totalDays)
        assertEquals(205_128L, result.totalHours)
        assertEquals(12_307_680L, result.totalMinutes)
        assertEquals(738_460_800L, result.totalSeconds)
    }

    @Test
    fun birthTimeShiftsTheDayCount() {
        val birth = at(2000, 1, 1, 18, 30)
        val before = Age.lived(birth, at(2000, 1, 2, 12, 0))!!
        assertEquals(0, before.days)
        assertEquals(17, before.hours)
        assertEquals(30, before.minutes)
        assertEquals(0L, before.totalDays)
        assertEquals(63_000L, before.totalSeconds)
        val after = Age.lived(birth, at(2000, 1, 2, 18, 30, 45))!!
        assertEquals(1, after.days)
        assertEquals(0, after.hours)
        assertEquals(45, after.seconds)
        assertEquals(1L, after.totalDays)
    }

    @Test
    fun secondsBetweenCrossesMidnight() {
        assertEquals(86_400L, Age.secondsBetween(at(2024, 2, 28, 23, 0), at(2024, 2, 29, 23, 0)))
        assertEquals(7_200L, Age.secondsBetween(at(2024, 1, 1, 23, 0), at(2024, 1, 2, 1, 0)))
        assertEquals(-3_600L, Age.secondsBetween(at(2024, 1, 1, 12, 0), at(2024, 1, 1, 11, 0)))
    }

    @Test
    fun countdownToNextBirthday() {
        val birth = at(2000, 5, 10, 7, 15)
        val ahead = Age.untilBirthday(birth, at(2026, 5, 9, 7, 15))
        assertEquals(LocalDate(2026, 5, 10), ahead.date)
        assertEquals(1L, ahead.days)
        assertEquals(0L, ahead.hours)
        assertEquals(26, ahead.age)
        val passed = Age.untilBirthday(birth, at(2026, 5, 10, 8, 0))
        assertEquals(LocalDate(2027, 5, 10), passed.date)
        assertEquals(364L, passed.days)
        assertEquals(23L, passed.hours)
        assertEquals(15L, passed.minutes)
        assertEquals(27, passed.age)
    }

    @Test
    fun birthdayTodayAndLeapDay() {
        assertTrue(Age.isBirthday(LocalDate(2000, 9, 16), LocalDate(2026, 9, 16)))
        assertFalse(Age.isBirthday(LocalDate(2000, 9, 16), LocalDate(2026, 9, 17)))
        assertEquals(LocalDate(2027, 2, 28), Age.nextBirthday(LocalDate(2000, 2, 29), LocalDate(2026, 3, 1)))
        assertEquals(LocalDate(2028, 2, 29), Age.nextBirthday(LocalDate(2000, 2, 29), LocalDate(2027, 3, 1)))
    }

    @Test
    fun parsing() {
        assertEquals(LocalDate(2003, 4, 23), Age.parse(" 2003-04-23 "))
        assertNull(Age.parse("2003-02-30"))
        assertNull(Age.parse("23.04.2003"))
        assertEquals(LocalTime(0, 0), Age.parseTime("", ""))
        assertEquals(LocalTime(7, 5), Age.parseTime(" 7 ", "05"))
        assertNull(Age.parseTime("24", "0"))
        assertNull(Age.parseTime("1", "60"))
        assertNull(Age.parseTime("x", "0"))
        assertNull(Age.lived(at(2030, 1, 1), at(2026, 1, 1)))
    }
}
