package com.vasmarfas.card.tools.sound

import com.vasmarfas.card.core.SpectrumFrame
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TuningTest {
    private val sampleRate = 48_000
    private val bins = 8192
    private val binHz = sampleRate.toDouble() / (bins * 2)

    private fun frameWith(vararg peaks: Pair<Int, Float>): SpectrumFrame {
        val magnitudes = FloatArray(bins) { -100f }
        for ((bin, level) in peaks) {
            magnitudes[bin] = level
            magnitudes[bin - 1] = level - 6
            magnitudes[bin + 1] = level - 6
        }
        return SpectrumFrame(magnitudes, sampleRate)
    }

    @Test
    fun namedPitchesFollowEqualTemperament() {
        assertEquals(440.0, Tuning.pitch("A4", 440.0).frequencyHz, 1e-9)
        assertEquals(82.4069, Tuning.pitch("E2", 440.0).frequencyHz, 1e-3)
        assertEquals(277.1826, Tuning.pitch("C#4", 440.0).frequencyHz, 1e-3)
        assertEquals(432.0, Tuning.pitch("A4", 432.0).frequencyHz, 1e-9)
    }

    @Test
    fun nearestNoteAndCents() {
        val pitch = Tuning.nearest(82.5, 440.0)
        assertEquals("E2", pitch.label)
        assertEquals(1.95, Tuning.cents(82.5, pitch.frequencyHz), 0.01)
        assertEquals("C4", Tuning.nearest(261.0, 440.0).label)
        assertEquals("B3", Tuning.nearest(246.0, 440.0).label)
    }

    @Test
    fun instrumentTargetsTheClosestString() {
        assertEquals("A2", Tuning.target(108.0, Instrument.GUITAR, 440.0).label)
        assertEquals("E4", Tuning.target(335.0, Instrument.GUITAR, 440.0).label)
        assertEquals("G2", Tuning.target(95.0, Instrument.BASS, 440.0).label)
        assertEquals("F#2", Tuning.target(93.0, Instrument.CHROMATIC, 440.0).label)
    }

    @Test
    fun fundamentalWinsOverALouderSecondHarmonic() {
        val found = assertNotNull(Tuning.fundamental(frameWith(40 to -50f, 80 to -30f, 120 to -40f)))
        assertTrue(abs(found - 40 * binHz) < 0.01, "found $found")
    }

    @Test
    fun pureToneIsNotHalved() {
        val found = assertNotNull(Tuning.fundamental(frameWith(40 to -30f)))
        assertTrue(abs(found - 40 * binHz) < 0.01, "found $found")
    }

    @Test
    fun silenceHasNoPitch() {
        assertNull(Tuning.fundamental(SpectrumFrame(FloatArray(bins) { -100f }, sampleRate)))
    }
}
