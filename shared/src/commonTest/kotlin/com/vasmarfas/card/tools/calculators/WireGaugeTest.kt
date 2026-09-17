package com.vasmarfas.card.tools.calculators

import kotlin.test.Test
import kotlin.test.assertEquals

class WireGaugeTest {
    @Test
    fun geometry() {
        assertEquals(2.588, WireGauge.diameterMm(10), 0.001)
        assertEquals(5.261, WireGauge.areaMm2(10), 0.001)
        assertEquals(11.684, WireGauge.diameterMm(-3), 0.001)
        assertEquals(10.0, WireGauge.awgFromDiameter(2.588), 0.01)
        assertEquals(10.0, WireGauge.awgFromArea(5.261), 0.01)
    }

    @Test
    fun table() {
        assertEquals(44, WireGauge.table.size)
        assertEquals("4/0", WireGauge.table.first().label)
        val awg14 = WireGauge.nearest(WireGauge.awgFromArea(2.0))
        assertEquals(14, awg14.awg)
        assertEquals(32.0, awg14.chassisAmps, 1e-9)
        assertEquals(8.28, awg14.ohmsPerKm, 0.05)
    }
}
