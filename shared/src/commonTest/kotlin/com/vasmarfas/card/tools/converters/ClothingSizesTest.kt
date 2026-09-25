package com.vasmarfas.card.tools.converters

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClothingSizesTest {
    @Test
    fun lookup() {
        val rows = ClothingSizes.womenClothing.lookup(1, "38")
        assertEquals(1, rows.size)
        assertEquals("44", rows[0][0])
        assertEquals("S", rows[0][4])
        val shoes = ClothingSizes.menShoes.lookup(2, "42")
        assertEquals("26.5", shoes[0][0])
    }

    @Test
    fun tables() {
        ClothingSizes.tables.forEach { table ->
            assertTrue(table.rows.all { it.size == table.columns.size })
        }
        assertTrue("M" in ClothingSizes.menClothing.values(4))
    }
}
