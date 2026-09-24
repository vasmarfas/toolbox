package com.vasmarfas.card.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

actual fun microphoneSpectrumFlow(fftSize: Int): Flow<SpectrumFrame> = flow {
    val window = FloatArray(fftSize)
    var collected = 0
    microphoneChunks().collect { chunk ->
        val samples = chunk.samples
        if (samples.size >= fftSize) {
            samples.copyInto(window, 0, samples.size - fftSize, samples.size)
        } else {
            window.copyInto(window, 0, samples.size, fftSize)
            samples.copyInto(window, fftSize - samples.size)
        }
        collected = minOf(fftSize, collected + samples.size)
        if (collected == fftSize) emit(SpectrumFrame(Fft.magnitudesDb(window), chunk.sampleRate))
    }
}.flowOn(Dispatchers.Default)
