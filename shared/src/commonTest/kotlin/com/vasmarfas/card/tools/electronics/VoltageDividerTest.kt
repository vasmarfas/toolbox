package com.vasmarfas.card.tools.electronics

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VoltageDividerTest {
    @Test
    fun divider() {
        assertEquals(5.0, VoltageDivider.vout(10.0, 1000.0, 1000.0), 1e-9)
        assertEquals(1000.0, VoltageDivider.r1(10.0, 5.0, 1000.0), 1e-9)
        assertEquals(1000.0, VoltageDivider.r2(10.0, 5.0, 1000.0), 1e-9)
        assertEquals(0.005, VoltageDivider.current(10.0, 1000.0, 1000.0), 1e-12)
        assertEquals(3.3, VoltageDivider.vout(5.0, 1700.0, 3300.0), 1e-9)
    }

    @Test
    fun standardPairsAreSortedByHowCloseTheyGet() {
        val pairs = VoltageDivider.pairs(12.0, 5.0, LedResistor.e12)
        assertEquals(5, pairs.size)
        assertTrue(abs(pairs.first().vout - 5.0) < 0.1)
        assertTrue(pairs.all { it.r1 + it.r2 in 10e3..100e3 })
        assertTrue(pairs.zipWithNext().all { (a, b) -> abs(a.vout - 5.0) <= abs(b.vout - 5.0) })
    }
}
