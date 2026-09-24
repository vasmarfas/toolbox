package com.vasmarfas.card.core

import kotlin.concurrent.Volatile
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

enum class Waveform { SINE, SQUARE, TRIANGLE, SAWTOOTH, NOISE }

// highest rate the output accepted, callers cap the frequency at Nyquist. Most devices open at 48 kHz
expect fun toneSampleRate(): Int

// retunes a running oscillator without restarting the phase
expect fun startTone(frequencyHz: Double, waveform: Waveform, volume: Float)

expect fun stopTone()

// phase carries over between buffers and the amplitude is ramped, otherwise every retune and volume
// change lands as a click
internal class OscillatorState {
    @Volatile
    private var frequency = 440.0

    @Volatile
    private var waveform = Waveform.SINE

    @Volatile
    private var target = 0.6f

    private var phase = 0.0
    private var amplitude = 0.0f
    private var noise = Random(1)

    fun set(frequencyHz: Double, waveform: Waveform, volume: Float) {
        frequency = frequencyHz
        this.waveform = waveform
        target = volume
    }

    fun fill(buffer: ShortArray, sampleRate: Int) {
        val step = frequency / sampleRate
        val shape = waveform
        val wanted = target
        for (i in buffer.indices) {
            amplitude += (wanted - amplitude) * RampPerSample
            val value = when (shape) {
                Waveform.SINE -> sin(2.0 * PI * phase)
                Waveform.SQUARE -> if (phase < 0.5) 1.0 else -1.0
                Waveform.TRIANGLE -> 1.0 - 4.0 * abs(phase - 0.5)
                Waveform.SAWTOOTH -> 2.0 * phase - 1.0
                Waveform.NOISE -> noise.nextDouble() * 2.0 - 1.0
            }
            buffer[i] = (value * amplitude * Short.MAX_VALUE).toInt().toShort()
            phase += step
            if (phase >= 1.0) phase -= phase.toInt()
        }
    }

    private companion object {
        const val RampPerSample = 0.001f
    }
}
