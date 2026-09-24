package com.vasmarfas.card.tools.fitness

import kotlin.test.Test
import kotlin.test.assertEquals

class SleepAndWaterTest {
    @Test
    fun sleepTimes() {
        val bed = SleepMath.bedtimes(7 * 60)
        assertEquals(6, bed.first().cycles)
        assertEquals("21:46", bed.first().time)
        assertEquals("03:44", SleepMath.wakeTimes(23 * 60).first().time)
        assertEquals(4.5, SleepMath.wakeTimes(23 * 60).first().hours)
    }

    @Test
    fun water() {
        assertEquals(2310, WaterIntake.dailyMl(70.0, WaterActivity.LOW, Climate.TEMPERATE))
        assertEquals(3510, WaterIntake.dailyMl(70.0, WaterActivity.HIGH, Climate.HOT))
        assertEquals(10, WaterIntake.glasses(2310, 250))
    }
}
