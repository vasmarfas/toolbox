package com.vasmarfas.card.tools.everyday

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RandomToolsTest {
    @Test
    fun sleepTimes() {
        val bed = SleepMath.bedtimes(7 * 60)
        assertEquals(6, bed.first().cycles)
        assertEquals("21:46", bed.first().time)
        assertEquals("03:44", SleepMath.wakeTimes(23 * 60).first().time)
        assertEquals(4.5, SleepMath.wakeTimes(23 * 60).first().hours)
    }

    @Test
    fun wheel() {
        assertEquals(listOf("a", "b"), DecisionWheel.parseOptions(" a \n\nb\n"))
        val steps = DecisionWheel.stepsToReach(1, 0, 4)
        assertTrue(steps >= 24)
        assertEquals(0, (1 + steps) % 4)
        val delays = DecisionWheel.delays(steps)
        assertEquals(steps, delays.size)
        assertTrue(delays.zipWithNext().all { (a, b) -> b >= a })
    }

    @Test
    fun dice() {
        val expr = Dice.parse("2d6+3")!!
        assertEquals(listOf(DiceTerm(2, 6)), expr.terms)
        assertEquals(3, expr.modifier)
        assertEquals("1d20+1d4-1", Dice.parse("D20 - 1 + d4")!!.notation)
        assertNull(Dice.parse("abc"))
        assertNull(Dice.parse("5"))
        val roll = Dice.roll(expr, Random(7))
        assertEquals(2, roll.rolls.single().size)
        assertTrue(roll.total in 5..15)
    }
}
