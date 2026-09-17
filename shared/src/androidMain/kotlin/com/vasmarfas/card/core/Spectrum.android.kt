package com.vasmarfas.card.core

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

@RequiresPermission(Manifest.permission.RECORD_AUDIO)
actual fun microphoneSpectrumFlow(fftSize: Int): Flow<SpectrumFrame> = flow {
    val sampleRate = 44_100
    val minBuffer = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
    )
    val record = try {
        AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer, fftSize * 2),
        )
    } catch (e: SecurityException) {
        return@flow
    }
    if (record.state != AudioRecord.STATE_INITIALIZED) return@flow
    record.startRecording()
    val pcm = ShortArray(fftSize)
    val samples = FloatArray(fftSize)
    try {
        while (currentCoroutineContext().isActive) {
            var filled = 0
            while (filled < fftSize) {
                val read = record.read(pcm, filled, fftSize - filled)
                if (read <= 0) break
                filled += read
            }
            if (filled < fftSize) break
            for (i in 0 until fftSize) samples[i] = pcm[i] / 32768f
            emit(SpectrumFrame(Fft.magnitudesDb(samples), sampleRate))
        }
    } finally {
        record.stop()
        record.release()
    }
}
