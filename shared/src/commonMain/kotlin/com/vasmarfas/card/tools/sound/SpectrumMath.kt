package com.vasmarfas.card.tools.sound

import com.vasmarfas.card.core.SpectrumFrame
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class Harmonic(val order: Int, val frequencyHz: Double, val levelDb: Float, val relativeDb: Float)

class SpectrumReading(
    val peakHz: Double,
    val peakDb: Float,
    val harmonics: List<Harmonic>,
    val thdPercent: Double?,
)

private const val NoiseFloorDb = -90f

object SpectrumAnalysis {
    // without it the frequency snaps to multiples of the bin width, 21 Hz at 44.1 kHz and 2048 points
    fun refinedPeak(frame: SpectrumFrame, minBin: Int = 1): Double? {
        val magnitudes = frame.magnitudesDb
        if (magnitudes.size < 3) return null
        var peak = minBin
        for (i in minBin until magnitudes.size) {
            if (magnitudes[i] > magnitudes[peak]) peak = i
        }
        if (magnitudes[peak] <= NoiseFloorDb) return null
        if (peak <= 0 || peak >= magnitudes.size - 1) return frame.frequencyOf(peak)
        val left = magnitudes[peak - 1]
        val centre = magnitudes[peak]
        val right = magnitudes[peak + 1]
        val denominator = left - 2 * centre + right
        val offset = (0.5 * (left - right) / denominator).takeIf { it.isFinite() }?.coerceIn(-0.5, 0.5) ?: 0.0
        return (peak + offset) * frame.binHz
    }

    fun levelAt(frame: SpectrumFrame, frequencyHz: Double): Float? {
        if (!frequencyHz.isFinite()) return null
        val bin = (frequencyHz / frame.binHz).roundToInt()
        if (bin !in frame.magnitudesDb.indices) return null
        // the peak can straddle two bins, so take the better of the immediate neighbours
        return (maxOf(bin - 1, 0)..minOf(bin + 1, frame.magnitudesDb.lastIndex))
            .maxOf { frame.magnitudesDb[it] }
    }

    fun analyse(frame: SpectrumFrame, maxHarmonics: Int = 8): SpectrumReading? {
        val peakHz = refinedPeak(frame) ?: return null
        val peakDb = levelAt(frame, peakHz) ?: return null
        val harmonics = (2..maxHarmonics).mapNotNull { order ->
            val frequency = peakHz * order
            if (frequency >= frame.sampleRate / 2.0) return@mapNotNull null
            val level = levelAt(frame, frequency) ?: return@mapNotNull null
            Harmonic(order, frequency, level, level - peakDb)
        }
        return SpectrumReading(peakHz, peakDb, harmonics, totalHarmonicDistortion(harmonics))
    }

    fun totalHarmonicDistortion(harmonics: List<Harmonic>): Double? {
        if (harmonics.isEmpty()) return null
        val sum = harmonics.sumOf { harmonic ->
            val ratio = 10.0.pow(harmonic.relativeDb / 20.0)
            ratio * ratio
        }
        return sqrt(sum) * 100.0
    }

    fun note(frequencyHz: Double): Pair<String, Int>? {
        if (frequencyHz < 16.0) return null
        val pitch = Tuning.nearest(frequencyHz, 440.0)
        return pitch.label to Tuning.cents(frequencyHz, pitch.frequencyHz).roundToInt()
    }

    fun isPureTone(reading: SpectrumReading): Boolean =
        reading.harmonics.all { abs(it.relativeDb) > 20f }
}
