package com.vasmarfas.card.tools.sound

import com.vasmarfas.card.core.SpectrumFrame
import com.vasmarfas.card.resources.*
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.StringResource

class Pitch(val name: String, val octave: Int, val frequencyHz: Double) {
    val label: String get() = "$name$octave"
}

enum class Instrument(val title: StringResource, val strings: List<String>) {
    CHROMATIC(Res.string.tuner_chromatic, emptyList()),
    GUITAR(Res.string.tuner_guitar, listOf("E2", "A2", "D3", "G3", "B3", "E4")),
    BASS(Res.string.tuner_bass, listOf("E1", "A1", "D2", "G2")),
    UKULELE(Res.string.tuner_ukulele, listOf("G4", "C4", "E4", "A4")),
    VIOLIN(Res.string.tuner_violin, listOf("G3", "D4", "A4", "E5")),
}

object Tuning {
    private val names = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    fun pitch(label: String, a4: Double): Pitch {
        val octave = label.takeLastWhile { it.isDigit() }.toInt()
        val name = label.dropLast(octave.toString().length)
        val semitones = names.indexOf(name) - 9 + (octave - 4) * 12
        return Pitch(name, octave, a4 * 2.0.pow(semitones / 12.0))
    }

    fun nearest(frequencyHz: Double, a4: Double): Pitch {
        val semitones = (12 * log2(frequencyHz / a4)).roundToInt()
        val index = ((semitones + 9) % 12 + 12) % 12
        val octave = 4 + floor((semitones + 9) / 12.0).toInt()
        return Pitch(names[index], octave, a4 * 2.0.pow(semitones / 12.0))
    }

    fun cents(frequencyHz: Double, targetHz: Double): Double = 1200 * log2(frequencyHz / targetHz)

    fun target(frequencyHz: Double, instrument: Instrument, a4: Double): Pitch =
        if (instrument.strings.isEmpty()) nearest(frequencyHz, a4)
        else instrument.strings.map { pitch(it, a4) }.minBy { abs(cents(frequencyHz, it.frequencyHz)) }

    // a plucked low string is often quieter than its 2nd or 3rd harmonic, so the strongest peak is divided
    // by the largest of 4, 3, 2 whose subharmonic still stands out of the noise
    fun fundamental(frame: SpectrumFrame, minHz: Double = 27.0): Double? {
        val minBin = (minHz / frame.binHz).toInt().coerceAtLeast(1)
        val peakHz = SpectrumAnalysis.refinedPeak(frame, minBin) ?: return null
        val peakDb = SpectrumAnalysis.levelAt(frame, peakHz) ?: return null
        val noise = frame.magnitudesDb.sorted()[frame.magnitudesDb.size / 2]
        if (peakDb < noise + SignalAboveNoiseDb) return null
        val threshold = maxOf(peakDb - SubharmonicRangeDb, noise + SignalAboveNoiseDb / 2)
        val divisor = (MaxDivisor downTo 2).firstOrNull { n ->
            val level = SpectrumAnalysis.levelAt(frame, peakHz / n)
            peakHz / n >= minHz && level != null && level > threshold
        } ?: 1
        return peakHz / divisor
    }

    private const val MaxDivisor = 4
    private const val SignalAboveNoiseDb = 24f
    private const val SubharmonicRangeDb = 30f
}
