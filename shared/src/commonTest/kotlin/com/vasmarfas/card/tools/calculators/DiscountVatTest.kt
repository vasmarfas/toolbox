package com.vasmarfas.card.tools.calculators

import kotlin.test.Test
import kotlin.test.assertEquals

class DiscountVatTest {
    @Test
    fun discount() {
        assertEquals(800.0, DiscountVat.discounted(1000.0, 20.0), 1e-9)
        assertEquals(720.0, DiscountVat.discounted(DiscountVat.discounted(1000.0, 20.0), 10.0), 1e-9)
    }

    @Test
    fun vat() {
        val added = DiscountVat.addVat(1000.0, 20.0)
        assertEquals(200.0, added.vat, 1e-9)
        assertEquals(1200.0, added.gross, 1e-9)
        val removed = DiscountVat.removeVat(1200.0, 20.0)
        assertEquals(1000.0, removed.net, 1e-9)
        assertEquals(200.0, removed.vat, 1e-9)
    }
}
