package com.vasmarfas.card.tools.calculators

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TiresTest {
    private fun near(expected: Double, actual: Double, tolerance: Double = 1e-6) =
        assertTrue(abs(expected - actual) < tolerance, "$expected vs $actual")

    @Test
    fun geometry() {
        val tire = TireSize(205.0, 55.0, 16.0)
        near(112.75, tire.sidewallMm)
        near(631.9, tire.diameterMm)
        near(1985.18, tire.circumferenceMm, 0.01)
        near(503.73, tire.revolutionsPerKm, 0.01)
        assertEquals("205/55 R16", tire.toString())
    }

    @Test
    fun comparison() {
        val current = TireSize(205.0, 55.0, 16.0)
        val other = TireSize(225.0, 45.0, 17.0)
        near(0.3798, Tires.diameterChangePercent(current, other), 1e-4)
        near(100.3798, Tires.actualSpeed(current, other, 100.0), 1e-4)
        near(1.2, Tires.clearanceChangeMm(current, other))
        near(-0.3784, Tires.diameterChangePercent(other, current), 1e-4)
    }
}
