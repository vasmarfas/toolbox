package com.vasmarfas.card.tools.sound

import com.vasmarfas.card.core.SpectrumFrame
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpectrumMathTest {
    private val sampleRate = 44_100
    private val bins = 1024

    private fun frameWith(vararg peaks: Pair<Double, Float>): SpectrumFrame {
        val magnitudes = FloatArray(bins) { -120f }
        val binHz = sampleRate.toDouble() / (bins * 2)
        for ((frequency, level) in peaks) {
            val bin = (frequency / binHz).toInt()
            if (bin in magnitudes.indices) magnitudes[bin] = level
        }
        return SpectrumFrame(magnitudes, sampleRate)
    }

    @Test
    fun binWidthFollowsTheSampleRate() {
        val frame = frameWith(1000.0 to -10f)
        assertTrue(abs(frame.binHz - 21.53) < 0.01, "binHz ${frame.binHz}")
        assertTrue(abs(frame.frequencyOf(10) - 215.3) < 0.1)
    }

    @Test
    fun peakLandsOnTheToneThatIsThere() {
        val peak = SpectrumAnalysis.refinedPeak(frameWith(1000.0 to -10f))
        assertNotNull(peak)
        assertTrue(abs(peak - 1000.0) < frameWith().binHz, "peak $peak")
    }

    @Test
    fun silenceHasNoPeak() {
        assertNull(SpectrumAnalysis.refinedPeak(SpectrumFrame(FloatArray(bins) { -120f }, sampleRate)))
    }

    @Test
    fun harmonicsAreFoundAtWholeMultiples() {
        val reading = SpectrumAnalysis.analyse(
            frameWith(440.0 to -6f, 880.0 to -26f, 1320.0 to -36f),
        )
        assertNotNull(reading)
        assertTrue(abs(reading.peakHz - 440.0) < 25.0, "peak ${reading.peakHz}")

        val second = reading.harmonics.first { it.order == 2 }
        assertTrue(abs(second.relativeDb - (-20f)) < 1.5f, "second ${second.relativeDb}")
        val third = reading.harmonics.first { it.order == 3 }
        assertTrue(abs(third.relativeDb - (-30f)) < 1.5f, "third ${third.relativeDb}")
    }

    @Test
    fun harmonicsAboveNyquistAreNotInvented() {
        val reading = SpectrumAnalysis.analyse(frameWith(15_000.0 to -10f))
        assertNotNull(reading)
        assertTrue(reading.harmonics.all { it.frequencyHz < sampleRate / 2.0 })
    }

    @Test
    fun distortionMatchesTheHarmonicLevels() {
        // a single harmonic 20 dB down is a tenth of the fundamental, so THD is 10 %
        val thd = SpectrumAnalysis.totalHarmonicDistortion(
            listOf(Harmonic(2, 880.0, -26f, -20f)),
        )
        assertNotNull(thd)
        assertTrue(abs(thd - 10.0) < 0.1, "thd $thd")
        assertNull(SpectrumAnalysis.totalHarmonicDistortion(emptyList()))
    }

    @Test
    fun notesMatchConcertPitch() {
        assertEquals("A4" to 0, SpectrumAnalysis.note(440.0))
        assertEquals("A5" to 0, SpectrumAnalysis.note(880.0))
        assertEquals("A3" to 0, SpectrumAnalysis.note(220.0))
        assertEquals("C4" to 0, SpectrumAnalysis.note(261.6256))
        assertEquals("E4" to 0, SpectrumAnalysis.note(329.6276))

        val (name, cents) = SpectrumAnalysis.note(445.0)!!
        assertEquals("A4", name)
        assertTrue(cents in 15..25, "cents $cents")
        assertNull(SpectrumAnalysis.note(5.0))
    }
}
