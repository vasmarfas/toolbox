package com.vasmarfas.card.tools.time

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SolarMathTest {
    @Test
    fun moscowSummerSolstice() {
        val t = SolarMath.compute(LocalDate(2024, 6, 21), 55.7558, 37.6173)
        val sunrise = t.sunrise!!
        val sunset = t.sunset!!
        assertTrue(sunrise + 180 in 224.0..226.0, "sunrise $sunrise")
        assertTrue(sunset + 180 in 1276.0..1280.0, "sunset $sunset")
        assertTrue(t.dayLengthMinutes!! in 1050.0..1057.0)
        assertEquals("03:44", SolarMath.formatMinutes(sunrise, 10_800))
        assertNull(t.nauticalDawn)
    }

    @Test
    fun polarDayHasNoSunset() {
        val t = SolarMath.compute(LocalDate(2024, 6, 21), 69.65, 18.96)
        assertNull(t.sunrise)
        assertTrue(t.polarDay)
        assertEquals(17 to 34, SolarMath.durationParts(1053.6))
        assertEquals("23:30", SolarMath.formatMinutes(-30.0, 0))
    }
}
