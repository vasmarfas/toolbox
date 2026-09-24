package com.vasmarfas.card.core

import java.io.File

object Mp3Files {
    fun encode(sampleRate: Int, channels: Int, kbps: Int, pcm: ShortArray): ByteArray {
        val encoder = Mp3Encoder(sampleRate, channels, kbps)
        val body = encoder.encode(pcm) + encoder.finish()
        encoder.infoFrame().copyInto(body)
        return body
    }

    fun write(name: String, bytes: ByteArray): File = File(Ffmpeg.work, name).apply { writeBytes(bytes) }

    fun frameSamples(sampleRate: Int): Int = if (sampleRate >= 32000) 1152 else 576
}
