package com.vasmarfas.card.tools.electronics

import kotlin.test.Test
import kotlin.test.assertEquals

class VoltageDividerTest {
    @Test
    fun divider() {
        assertEquals(5.0, VoltageDivider.vout(10.0, 1000.0, 1000.0), 1e-9)
        assertEquals(1000.0, VoltageDivider.r1(10.0, 5.0, 1000.0), 1e-9)
        assertEquals(1000.0, VoltageDivider.r2(10.0, 5.0, 1000.0), 1e-9)
        assertEquals(0.005, VoltageDivider.current(10.0, 1000.0, 1000.0), 1e-12)
        assertEquals(3.3, VoltageDivider.vout(5.0, 1700.0, 3300.0), 1e-9)
    }
}
