package com.vasmarfas.card.tools.fitness

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BreathingTest {
    private fun near(expected: Double, actual: Double) = assertTrue(abs(expected - actual) < 1e-6, "$expected vs $actual")

    @Test
    fun boxBreathingPhases() {
        val start = Breathing.at(BreathPattern.BOX, 0.0)
        assertEquals(BreathPhase.INHALE, start.phase)
        assertEquals(0f, start.fill)
        near(4.0, start.secondsLeft)
        near(0.5, Breathing.at(BreathPattern.BOX, 2.0).fill.toDouble())
        val hold = Breathing.at(BreathPattern.BOX, 5.0)
        assertEquals(BreathPhase.HOLD_IN, hold.phase)
        assertEquals(1f, hold.fill)
        near(3.0, hold.secondsLeft)
        near(0.5, Breathing.at(BreathPattern.BOX, 10.0).fill.toDouble())
        assertEquals(BreathPhase.HOLD_OUT, Breathing.at(BreathPattern.BOX, 13.0).phase)
        val next = Breathing.at(BreathPattern.BOX, 16.5)
        assertEquals(1, next.cycle)
        assertEquals(BreathPhase.INHALE, next.phase)
    }

    @Test
    fun patternsWithoutHoldsSkipThem() {
        assertEquals(listOf(BreathPhase.INHALE, BreathPhase.EXHALE), BreathPattern.COHERENT.phases.map { it.first })
        val exhale = Breathing.at(BreathPattern.COHERENT, 5.5)
        assertEquals(BreathPhase.EXHALE, exhale.phase)
        near(5.5, exhale.secondsLeft)
        near(19.0, BreathPattern.RELAX.cycleSeconds)
    }
}
