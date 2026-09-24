package com.vasmarfas.card.core

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread

actual class PcmPlayer actual constructor() {
    @Volatile
    private var worker: Thread? = null

    @Volatile
    private var track: AudioTrack? = null

    @Volatile
    private var written = 0L

    actual fun start(sampleRate: Int, channels: Int, source: (ShortArray) -> Int) {
        stop()
        val mask = if (channels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
        val minimum = AudioTrack.getMinBufferSize(sampleRate, mask, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(4096)
        val audio = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate).setChannelMask(mask).build())
            .setBufferSizeInBytes(minimum * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track = audio
        written = 0
        worker = thread(isDaemon = true, name = "pcm-player") {
            val buffer = ShortArray(minimum / 2 / channels * channels)
            audio.play()
            while (worker === Thread.currentThread()) {
                val n = source(buffer)
                if (n <= 0) break
                audio.write(buffer, 0, n)
                written += n / channels
            }
            if (worker === Thread.currentThread()) {
                while (audio.playbackHeadPosition < written && worker === Thread.currentThread()) Thread.sleep(20)
                worker = null
            }
            audio.stop()
            audio.release()
            if (track === audio) track = null
        }
    }

    actual fun stop() {
        worker = null
    }

    actual val position: Long get() = track?.playbackHeadPosition?.toLong() ?: written

    actual val playing: Boolean get() = worker != null
}
