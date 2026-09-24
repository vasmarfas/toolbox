package com.vasmarfas.card.tools.sound

import kotlin.random.Random

enum class NoiseColor { WHITE, PINK, BROWN }

// pink is Paul Kellet's filter, brown a leaky integrator, both scaled to about the loudness of white.
// All three run off the same white samples, so switching while playing does not jump
class NoiseGenerator(seed: Long) {
    private val random = Random(seed)
    private var b0 = 0.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var b3 = 0.0
    private var b4 = 0.0
    private var b5 = 0.0
    private var b6 = 0.0
    private var brown = 0.0
    private var level = 0.0

    fun sample(color: NoiseColor): Double {
        val white = random.nextDouble() * 2 - 1
        b0 = 0.99886 * b0 + white * 0.0555179
        b1 = 0.99332 * b1 + white * 0.0750759
        b2 = 0.96900 * b2 + white * 0.1538520
        b3 = 0.86650 * b3 + white * 0.3104856
        b4 = 0.55000 * b4 + white * 0.5329522
        b5 = -0.7616 * b5 - white * 0.0168980
        val pink = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362) * 0.11
        b6 = white * 0.115926
        brown = (brown + 0.02 * white) / 1.02
        return when (color) {
            NoiseColor.WHITE -> white * 0.5
            NoiseColor.PINK -> pink
            NoiseColor.BROWN -> brown * 3.5
        }
    }

    fun fill(buffer: ShortArray, color: NoiseColor, volume: Double, sampleRate: Int) {
        val ease = 2.0 / sampleRate
        for (i in buffer.indices) {
            level += (volume - level) * ease
            buffer[i] = (sample(color) * level * Short.MAX_VALUE).coerceIn(Short.MIN_VALUE.toDouble(), Short.MAX_VALUE.toDouble()).toInt().toShort()
        }
    }
}
