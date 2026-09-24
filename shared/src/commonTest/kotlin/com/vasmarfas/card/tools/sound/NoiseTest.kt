package com.vasmarfas.card.tools.sound

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class NoiseTest {
    // energy of the first difference over the signal: 2 for white noise, far less as the highs drop
    private fun roughness(color: NoiseColor): Double {
        val generator = NoiseGenerator(7)
        var energy = 0.0
        var diff = 0.0
        var previous = generator.sample(color)
        repeat(200_000) {
            val x = generator.sample(color)
            energy += x * x
            diff += (x - previous) * (x - previous)
            previous = x
        }
        return diff / energy
    }

    @Test
    fun colorsTiltTowardsLowPitches() {
        val white = roughness(NoiseColor.WHITE)
        val pink = roughness(NoiseColor.PINK)
        val brown = roughness(NoiseColor.BROWN)
        assertTrue(abs(white - 2.0) < 0.05, "white $white")
        assertTrue(pink < white / 2, "pink $pink")
        assertTrue(brown < pink / 4, "brown $brown, pink $pink")
    }

    @Test
    fun staysWithinRange() {
        for (color in NoiseColor.entries) {
            val generator = NoiseGenerator(3)
            var peak = 0.0
            repeat(200_000) { peak = maxOf(peak, abs(generator.sample(color))) }
            assertTrue(peak in 0.3..1.2, "$color peak $peak")
        }
    }

    @Test
    fun fadesInInsteadOfClicking() {
        val generator = NoiseGenerator(1)
        val buffer = ShortArray(4410)
        generator.fill(buffer, NoiseColor.WHITE, 1.0, 44_100)
        val start = buffer.take(100).maxOf { abs(it.toInt()) }
        val end = buffer.takeLast(100).maxOf { abs(it.toInt()) }
        assertTrue(start < 1000, "start $start")
        assertTrue(end > start * 3, "end $end")
    }
}
