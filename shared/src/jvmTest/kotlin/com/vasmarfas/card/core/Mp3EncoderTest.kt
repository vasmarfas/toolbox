package com.vasmarfas.card.core

import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class Mp3EncoderTest {
    @Test
    fun rejectsUnsupportedParameters() {
        assertFailsWith<IllegalArgumentException> { Mp3Encoder(44100, 0, 128) }
        assertFailsWith<IllegalArgumentException> { Mp3Encoder(44100, 3, 128) }
        assertFailsWith<IllegalArgumentException> { Mp3Encoder(8000, 1, 32) }
        assertFailsWith<IllegalArgumentException> { Mp3Encoder(11025, 1, 32) }
        assertFailsWith<IllegalArgumentException> { Mp3Encoder(44000, 2, 128) }
        assertFailsWith<IllegalArgumentException> { Mp3Encoder(44100, 2, 130) }
        assertFailsWith<IllegalArgumentException> { Mp3Encoder(44100, 2, 8) }
        assertFailsWith<IllegalArgumentException> { Mp3Encoder(22050, 2, 192) }
        assertFailsWith<IllegalArgumentException> { Mp3Encoder(16000, 1, 0) }
    }

    @Test
    fun listsRatesAndBitrates() {
        assertEquals(setOf(32000, 44100, 48000, 16000, 22050, 24000), Mp3Encoder.sampleRates)
        val mpeg1 = listOf(32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320)
        val mpeg2 = listOf(8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160)
        for (rate in listOf(32000, 44100, 48000)) assertEquals(mpeg1, Mp3Encoder.bitrates(rate))
        for (rate in listOf(16000, 22050, 24000)) assertEquals(mpeg2, Mp3Encoder.bitrates(rate))
        assertEquals(emptyList(), Mp3Encoder.bitrates(8000))
        for (rate in Mp3Encoder.sampleRates) {
            for (kbps in Mp3Encoder.bitrates(rate)) {
                Mp3Encoder(rate, 1, kbps)
                Mp3Encoder(rate, 2, kbps)
            }
        }
    }

    @Test
    fun checksCallOrder() {
        val encoder = Mp3Encoder(44100, 2, 128)
        assertFailsWith<IllegalArgumentException> { encoder.encode(ShortArray(3)) }
        assertFailsWith<IllegalArgumentException> { encoder.encode(ShortArray(4), 6) }
        assertFailsWith<IllegalStateException> { encoder.infoFrame() }
        encoder.encode(ShortArray(4))
        encoder.finish()
        assertFailsWith<IllegalStateException> { encoder.encode(ShortArray(4)) }
        assertFailsWith<IllegalStateException> { encoder.finish() }
    }

    @Test
    fun chunkedInputGivesTheSameBytes() {
        val random = Random(11)
        for ((rate, channels, kbps) in listOf(Triple(44100, 2, 128), Triple(22050, 1, 48), Triple(48000, 2, 32), Triple(16000, 2, 8))) {
            val music = Signals.pcm(*Array(channels) { Signals.pink(rate, 3.3, 0.4, 20 + it) })
            val whole = Mp3Encoder(rate, channels, kbps)
            val expected = whole.encode(music) + whole.finish()
            val chunked = Mp3Encoder(rate, channels, kbps)
            val parts = mutableListOf<ByteArray>()
            var position = 0
            var streamed = 0
            while (position < music.size) {
                val frames = minOf(random.nextInt(0, 5000), (music.size - position) / channels)
                val extra = minOf(random.nextInt(0, 3) * channels, music.size - position - frames * channels)
                val chunk = music.copyOfRange(position, position + frames * channels + extra)
                val bytes = chunked.encode(chunk, frames * channels)
                parts += bytes
                streamed += bytes.size
                position += frames * channels
            }
            parts += chunked.finish()
            assertTrue(streamed > 0, "bytes must come out before finish()")
            val actual = parts.fold(ByteArray(0)) { all, part -> all + part }
            assertContentEquals(expected, actual, "$rate Hz $channels ch $kbps kbps")
            assertContentEquals(whole.infoFrame(), chunked.infoFrame())
        }
    }

    @Test
    fun noInputGivesNoStream() {
        val encoder = Mp3Encoder(44100, 2, 128)
        assertEquals(0, encoder.encode(ShortArray(0)).size)
        assertEquals(0, encoder.finish().size)
        assertEquals(0, encoder.infoFrame().size)
    }

    @Test
    fun infoFrameDescribesTheStream() {
        for ((rate, channels, kbps) in listOf(Triple(44100, 2, 128), Triple(32000, 1, 64), Triple(24000, 2, 96), Triple(16000, 1, 48))) {
            val pcm = Signals.pcm(*Array(channels) { Signals.sine(rate, 0.3 + channels * 0.1, 440.0 + it * 110, 0.3) })
            val samples = pcm.size / channels
            val file = Mp3Files.encode(rate, channels, kbps, pcm)
            val frames = Mp3Stream.frames(file)
            assertTrue(frames.all { it.kbps == kbps && it.sampleRate == rate && (it.mode == 3) == (channels == 1) })
            val tag = frames.first()
            val at = tag.offset + 4 + tag.sideInfoSize
            assertEquals("Info", String(file, at, 4, Charsets.US_ASCII))
            assertEquals(0x0FL, Mp3Stream.u32(file, at + 4))
            assertEquals(frames.size - 1L, Mp3Stream.u32(file, at + 8), "frame count excludes the Info frame")
            assertEquals(file.size.toLong(), Mp3Stream.u32(file, at + 12))
            val lame = at + 120
            assertEquals("Mobitool", String(file, lame, 8, Charsets.US_ASCII))
            val delays = Mp3Stream.u32(file, lame + 20) and 0xFFFFFF
            val start = (delays ushr 12).toInt()
            val end = (delays and 0xFFF).toInt()
            val spf = Mp3Files.frameSamples(rate)
            assertEquals(Mp3Encoder.DELAY - 529, start)
            assertEquals((frames.size - 1) * spf - samples, start + end, "delay and padding leave exactly the input")
            val crc = ((file[lame + 34].toInt() and 0xFF) shl 8) or (file[lame + 35].toInt() and 0xFF)
            assertEquals(Mp3Stream.crc16(file, 0, lame + 34), crc)
            val music = ((file[lame + 32].toInt() and 0xFF) shl 8) or (file[lame + 33].toInt() and 0xFF)
            assertEquals(Mp3Stream.crc16(file, tag.size, file.size), music)
        }
    }

    @Test
    fun paddingKeepsTheAverageBitrate() {
        val rate = 44100
        val pcm = Signals.pcm(Signals.sine(rate, 10.0, 1000.0, 0.2))
        val frames = Mp3Stream.frames(Mp3Files.encode(rate, 1, 128, pcm)).drop(1)
        val bytes = frames.sumOf { it.size.toLong() }
        val expected = frames.size * 1152.0 / rate * 128_000 / 8
        assertTrue(abs(bytes - expected) < 1.0, "$bytes vs $expected")
    }
}
