package com.vasmarfas.card.tools.calculators

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RenovationTest {
    private fun near(expected: Double, actual: Double) = assertTrue(abs(expected - actual) < 1e-6, "$expected vs $actual")

    @Test
    fun wallpaper() {
        val plan = Renovation.wallpaper(Renovation.perimeter(4.0, 3.0), 2.7, 0.53, 10.05, 0.0)!!
        assertEquals(27, plan.strips)
        assertEquals(3, plan.stripsPerRoll)
        assertEquals(9, plan.rolls)
        near(2.8, plan.stripLength)
        val patterned = Renovation.wallpaper(14.0, 2.7, 0.53, 10.05, 0.64)!!
        near(3.2, patterned.stripLength)
        assertEquals(3, patterned.stripsPerRoll)
        assertEquals(5, Renovation.wallpaper(14.0, 2.7, 1.06, 10.05, 0.0)!!.rolls)
        assertNull(Renovation.wallpaper(14.0, 12.0, 0.53, 10.05, 0.0))
    }

    @Test
    fun paint() {
        val area = Renovation.wallArea(4.0, 3.0, 2.7, 3.6)
        near(34.2, area)
        val plan = Renovation.paint(area, 2, 10.0, 2.5)!!
        near(6.84, plan.liters)
        assertEquals(3, plan.cans)
        assertEquals(1, Renovation.paint(12.0, 2, 12.0, 2.0)!!.cans)
        assertNull(Renovation.paint(0.0, 2, 10.0, 2.5))
    }

    @Test
    fun floorCoverings() {
        assertEquals(7, Renovation.packs(12.0, 2.131, 10.0))
        assertEquals(6, Renovation.packs(12.0, 2.0, 0.0))
        assertEquals(6, Renovation.skirtingPieces(14.0, 0.8, 2.5))
        assertNull(Renovation.skirtingPieces(1.0, 2.0, 2.5))
    }

    @Test
    fun tiles() {
        val plan = Renovation.tiles(12.0, 300.0, 300.0, 2.0, 8.0, 10.0, 11)!!
        assertEquals(145, plan.tiles)
        assertEquals(14, plan.boxes)
        near(2.048, plan.groutKg)
        assertNull(Renovation.tiles(12.0, 300.0, 300.0, 2.0, 8.0, 10.0, null)!!.boxes)
        assertNull(Renovation.tiles(12.0, 0.0, 300.0, 2.0, 8.0, 10.0, null))
    }
}
