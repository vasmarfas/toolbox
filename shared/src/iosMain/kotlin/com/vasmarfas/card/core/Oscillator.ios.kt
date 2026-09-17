package com.vasmarfas.card.core

// the tone generator is not offered on iOS, so there is nothing to drive here
actual fun toneSampleRate(): Int = 48_000

actual fun startTone(frequencyHz: Double, waveform: Waveform, volume: Float) = Unit

actual fun stopTone() = Unit
