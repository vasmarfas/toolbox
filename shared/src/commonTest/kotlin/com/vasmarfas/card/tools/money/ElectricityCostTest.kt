package com.vasmarfas.card.tools.money

import kotlin.test.Test
import kotlin.test.assertEquals

class ElectricityCostTest {
    @Test
    fun costScalesByDayMonthAndYear() {
        val use = ElectricityCost.of(watts = 100.0, hoursPerDay = 5.0, daysPerMonth = 30.0, pricePerKwh = 6.0)
        assertEquals(0.5, use.kwhPerDay, 1e-9)
        assertEquals(15.0, use.kwhPerMonth, 1e-9)
        assertEquals(180.0, use.kwhPerYear, 1e-9)
        assertEquals(3.0, use.costPerDay, 1e-9)
        assertEquals(90.0, use.costPerMonth, 1e-9)
        assertEquals(1080.0, use.costPerYear, 1e-9)
    }
}
