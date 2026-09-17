package com.vasmarfas.card.tools.calculators

import kotlin.test.Test
import kotlin.test.assertEquals

class BatteryTest {
    @Test
    fun runtimeAndCharge() {
        assertEquals(8.5, Battery.runtimeHours(5000.0, 500.0, 85.0), 1e-9)
        assertEquals(3.7, Battery.runtimeHoursByPower(5000.0, 3.7, 5.0, 100.0), 1e-9)
        assertEquals(6.25, Battery.chargeHours(5000.0, 1000.0, 80.0), 1e-9)
    }

    @Test
    fun energy() {
        assertEquals(18.5, Battery.mahToWh(5000.0, 3.7), 1e-9)
        assertEquals(5000.0, Battery.whToMah(18.5, 3.7), 1e-9)
    }

    @Test
    fun formatting() {
        assertEquals("8 h 30 min", Battery.formatHours(8.5))
        assertEquals("1 d 1 h 0 min", Battery.formatHours(25.0))
        assertEquals("45 min", Battery.formatHours(0.75))
    }
}
