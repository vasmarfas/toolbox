package com.vasmarfas.card.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Mp3SmokeTest {
    @Test
    fun toneBecomesAPlayableFile() {
        val rate = 44100
        val tone = Signals.sine(rate, 1.0, 440.0, 0.5)
        val pcm = Signals.pcm(tone, tone)
        val encoder = Mp3Encoder(rate, 2, 128)
        val body = encoder.encode(pcm) + encoder.finish()
        encoder.infoFrame().copyInto(body)
        val file = Mp3Files.write("smoke.mp3", body)
        val (fields, probeErrors) = Ffmpeg.probe(file)
        assertEquals("", probeErrors)
        assertEquals("mp3", fields["streams.stream.0.codec_name"])
        val (decoded, errors) = Ffmpeg.decode(file)
        assertEquals("", errors)
        val left = Signals.channel(decoded, 2, 0)
        assertTrue(Signals.snr(tone, left, Mp3Encoder.DELAY) > 60)
    }
}
