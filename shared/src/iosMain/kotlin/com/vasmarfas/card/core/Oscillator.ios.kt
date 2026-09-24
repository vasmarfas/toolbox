package com.vasmarfas.card.core

import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.sampleRate

private val state = OscillatorState()

private var player: PcmPlayer? = null

actual fun toneSampleRate(): Int = AVAudioSession.sharedInstance().sampleRate.toInt().takeIf { it > 0 } ?: 48_000

actual fun startTone(frequencyHz: Double, waveform: Waveform, volume: Float) {
    state.set(frequencyHz, waveform, volume)
    if (player != null) return
    val rate = toneSampleRate()
    player = PcmPlayer().also { output ->
        output.start(rate, 1) { buffer ->
            state.fill(buffer, rate)
            buffer.size
        }
    }
}

actual fun stopTone() {
    player?.stop()
    player = null
}
