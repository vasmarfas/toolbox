package com.vasmarfas.card.tools.money

import kotlin.test.Test
import kotlin.test.assertEquals

class FuelCostTest {
    @Test
    fun trip() {
        val result = FuelCost.compute(100.0, DistanceUnit.KM, 8.0, ConsumptionUnit.L100KM, 60.0, PriceUnit.LITRE, 2, roundTrip = false)
        assertEquals(8.0, result.litres, 1e-9)
        assertEquals(480.0, result.cost, 1e-9)
        assertEquals(240.0, result.perPerson, 1e-9)
        assertEquals(480.0, result.costPer100km, 1e-9)
    }

    @Test
    fun unitsAndRoundTrip() {
        assertEquals(10.0, FuelCost.litresPer100km(10.0, ConsumptionUnit.KML), 1e-9)
        assertEquals(10.0, FuelCost.litresPer100km(23.5214583, ConsumptionUnit.MPG_US), 1e-6)
        val result = FuelCost.compute(100.0, DistanceUnit.MI, 10.0, ConsumptionUnit.KML, 3.785411784, PriceUnit.GALLON_US, 1, roundTrip = true)
        assertEquals(321.8688, result.distanceKm, 1e-6)
        assertEquals(32.18688, result.cost, 1e-6)
    }
}
