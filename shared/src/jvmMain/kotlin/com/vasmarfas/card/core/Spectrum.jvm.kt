package com.vasmarfas.card.core

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.TargetDataLine
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

actual fun microphoneSpectrumFlow(fftSize: Int): Flow<SpectrumFrame> = flow {
    val sampleRate = 44_100
    val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
    val line = runCatching { AudioSystem.getTargetDataLine(format) as TargetDataLine }.getOrNull() ?: return@flow
    line.open(format, fftSize * 4)
    line.start()
    val bytes = ByteArray(fftSize * 2)
    val samples = FloatArray(fftSize)
    try {
        while (currentCoroutineContext().isActive) {
            var filled = 0
            while (filled < bytes.size) {
                val read = line.read(bytes, filled, bytes.size - filled)
                if (read <= 0) break
                filled += read
            }
            if (filled < bytes.size) break
            for (i in 0 until fftSize) {
                val value = ((bytes[i * 2 + 1].toInt() shl 8) or (bytes[i * 2].toInt() and 0xFF)).toShort()
                samples[i] = value / 32768f
            }
            emit(SpectrumFrame(Fft.magnitudesDb(samples), sampleRate))
        }
    } finally {
        line.stop()
        line.close()
    }
}
