package com.vasmarfas.card.tools.calculators

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OhmsLawTest {
    @Test
    fun solve() {
        val fromVr = OhmsLaw.solve(12.0, null, 4.0, null)!!
        assertEquals(3.0, fromVr.current, 1e-9)
        assertEquals(36.0, fromVr.power, 1e-9)
        val fromPr = OhmsLaw.solve(null, null, 4.0, 36.0)!!
        assertEquals(12.0, fromPr.voltage, 1e-9)
        assertEquals(3.0, fromPr.current, 1e-9)
        val fromIp = OhmsLaw.solve(null, 3.0, null, 36.0)!!
        assertEquals(12.0, fromIp.voltage, 1e-9)
        assertEquals(4.0, fromIp.resistance, 1e-9)
    }

    @Test
    fun needsExactlyTwo() {
        assertNull(OhmsLaw.solve(12.0, 3.0, 4.0, null))
        assertNull(OhmsLaw.solve(12.0, null, null, null))
    }
}
