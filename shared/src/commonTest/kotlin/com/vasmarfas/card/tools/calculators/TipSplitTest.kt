package com.vasmarfas.card.tools.calculators

import kotlin.test.Test
import kotlin.test.assertEquals

class TipSplitTest {
    @Test
    fun split() {
        val result = TipSplit.compute(100.0, 10.0, 4, roundUp = false)
        assertEquals(10.0, result.tip, 1e-9)
        assertEquals(110.0, result.total, 1e-9)
        assertEquals(27.5, result.perPerson, 1e-9)
        assertEquals(2.5, result.tipPerPerson, 1e-9)
    }

    @Test
    fun roundUp() {
        val result = TipSplit.compute(100.0, 10.0, 4, roundUp = true)
        assertEquals(28.0, result.perPerson, 1e-9)
        assertEquals(112.0, result.total, 1e-9)
        assertEquals(12.0, result.tip, 1e-9)
    }
}
