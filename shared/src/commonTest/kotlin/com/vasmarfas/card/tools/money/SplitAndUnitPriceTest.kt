package com.vasmarfas.card.tools.money

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SplitAndUnitPriceTest {
    @Test
    fun unitPrice() {
        assertEquals(5.0, UnitPrice.perBaseUnit(2.5, 500.0, QuantityUnit.G))
        assertEquals(2.0, UnitPrice.perBaseUnit(1.0, 500.0, QuantityUnit.ML))
        assertNull(UnitPrice.perBaseUnit(1.0, 0.0, QuantityUnit.PCS))
        val results = listOf(
            UnitPriceResult(0, 6.0, Dimension.MASS),
            UnitPriceResult(1, 5.0, Dimension.MASS),
            UnitPriceResult(2, 1.0, Dimension.COUNT),
        )
        assertEquals(1, UnitPrice.cheapest(results)[Dimension.MASS]?.index)
        assertTrue(abs(UnitPrice.extraPercent(6.0, 5.0) - 20.0) < 1e-9)
    }

    @Test
    fun settleTransfers() {
        val transfers = Split.settle(listOf("A" to 20.0, "B" to -10.0, "C" to -10.0))
        assertEquals(2, transfers.size)
        assertTrue(transfers.all { it.to == "A" && it.amount == 10.0 })
        assertTrue(Split.settle(listOf("A" to 0.0, "B" to 0.0)).isEmpty())
        val people = listOf(SplitPerson("Ann", "30"), SplitPerson("Bob", ""))
        assertEquals(people, Split.decode(Split.encode(people)))
    }
}
