package com.vasmarfas.card.core

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import kotlin.concurrent.thread

private val candidateRates = intArrayOf(192_000, 96_000, 48_000, 44_100)

private fun formatFor(rate: Int) = AudioFormat(rate.toFloat(), 16, 1, true, false)

private val negotiatedRate: Int by lazy {
    candidateRates.firstOrNull { rate ->
        runCatching { AudioSystem.getSourceDataLine(formatFor(rate)).also { it.close() } }.isSuccess
    } ?: 44_100
}

private val state = OscillatorState()

@Volatile
private var worker: Thread? = null

actual fun toneSampleRate(): Int = negotiatedRate

actual fun startTone(frequencyHz: Double, waveform: Waveform, volume: Float) {
    state.set(frequencyHz, waveform, volume)
    if (worker != null) return
    worker = thread(isDaemon = true, name = "oscillator") {
        runCatching {
            val rate = negotiatedRate
            val format = formatFor(rate)
            val line = AudioSystem.getSourceDataLine(format)
            line.open(format, rate / 5 * 2)
            line.start()
            val samples = ShortArray(rate / 20)
            val bytes = ByteArray(samples.size * 2)
            while (worker === Thread.currentThread()) {
                state.fill(samples, rate)
                for (i in samples.indices) {
                    bytes[i * 2] = (samples[i].toInt() and 0xFF).toByte()
                    bytes[i * 2 + 1] = ((samples[i].toInt() shr 8) and 0xFF).toByte()
                }
                line.write(bytes, 0, bytes.size)
            }
            line.stop()
            line.close()
        }
    }
}

actual fun stopTone() {
    worker = null
}
