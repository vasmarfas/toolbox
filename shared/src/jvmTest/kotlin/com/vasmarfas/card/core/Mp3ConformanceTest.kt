package com.vasmarfas.card.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Mp3ConformanceTest {
    private val mpeg1Bitrates = listOf(32, 48, 64, 96, 128, 192, 256, 320)
    private val mpeg2Bitrates = listOf(8, 16, 24, 32, 64, 96, 128, 160)

    // deliberately not a whole number of frames long
    private fun program(rate: Int, channel: Int): DoubleArray {
        val seconds = 1.537
        val chord = listOf(261.63, 329.63, 392.0).map { Signals.sine(rate, seconds, it * (1 + 0.5 * channel), 0.12) }
        val noise = Signals.pink(rate, seconds, 0.08, 40 + channel)
        val (bursts, _) = Signals.bursts(rate, seconds, 0.5)
        return DoubleArray(chord[0].size) { i -> chord.sumOf { it[i] } + noise[i] + bursts[i] }
    }

    @Test
    fun ffmpegAcceptsEveryConfiguration() {
        val report = StringBuilder()
        for (rate in listOf(32000, 44100, 48000, 16000, 22050, 24000)) {
            val bitrates = if (rate >= 32000) mpeg1Bitrates else mpeg2Bitrates
            for (channels in 1..2) {
                val signal = Array(channels) { program(rate, it) }
                val pcm = Signals.pcm(*signal)
                val samples = signal[0].size
                for (kbps in bitrates) {
                    val name = "conformance-$rate-$channels-$kbps"
                    val bytes = Mp3Files.encode(rate, channels, kbps, pcm)
                    val frames = Mp3Stream.frames(bytes)
                    val file = Mp3Files.write("$name.mp3", bytes)
                    val (fields, probeErrors) = Ffmpeg.probe(file)
                    assertEquals("", probeErrors, name)
                    assertEquals("mp3", fields["streams.stream.0.codec_name"], name)
                    assertEquals(rate.toString(), fields["streams.stream.0.sample_rate"], name)
                    assertEquals(channels.toString(), fields["streams.stream.0.channels"], name)
                    assertEquals((kbps * 1000).toString(), fields["streams.stream.0.bit_rate"], name)

                    val spf = Mp3Files.frameSamples(rate)
                    val tagged = frames.size > 1 && String(bytes, 4 + frames[0].sideInfoSize, 4, Charsets.US_ASCII) == "Info"
                    val audioFrames = frames.size - if (tagged) 1 else 0
                    val duration = fields.getValue("format.duration").toDouble()
                    assertEquals(audioFrames * spf.toDouble() / rate, duration, 0.001, name)
                    val excess = duration - samples.toDouble() / rate
                    assertTrue(excess >= 0 && excess * rate < Mp3Encoder.DELAY + spf, "$name: $excess s longer than the input")

                    val switched = frames.drop(if (tagged) 1 else 0).flatMap { Mp3Stream.blockTypes(bytes, it) }
                    assertTrue(switched.count { it == START_BLOCK } >= 2 * channels, "$name: transients must switch to short blocks")

                    val (decoded, decodeErrors) = Ffmpeg.decode(file)
                    assertEquals("", decodeErrors, name)
                    assertEquals(audioFrames * spf * channels, decoded.size, name)
                    val snr = (0 until channels).map { ch ->
                        val reference = Signals.channel(pcm, channels, ch)
                        Signals.snr(reference, Signals.channel(decoded, channels, ch), Mp3Encoder.DELAY)
                    }
                    report.appendLine("%-26s tag=%-5b excess=%5.1f ms snr=%s".format(name, tagged, excess * 1000, snr.joinToString("/") { "%.1f".format(it) }))
                    if (kbps / channels >= 32) assertTrue(snr.all { it > 8.0 }, "$name: SNR $snr")
                }
            }
        }
        println(report)
    }

    @Test
    fun ffmpegAcceptsRandomInput() {
        val random = Random(99)
        val rates = Mp3Encoder.sampleRates.toList()
        repeat(40) { round ->
            val rate = rates[random.nextInt(rates.size)]
            val channels = random.nextInt(1, 3)
            val kbps = Mp3Encoder.bitrates(rate).let { it[random.nextInt(it.size)] }
            val frames = if (round < 4) round else random.nextInt(1, rate * 2)
            val pcm = ShortArray(frames * channels)
            var i = 0
            while (i < frames) {
                val length = minOf(frames - i, random.nextInt(1, rate / 4))
                val kind = random.nextInt(6)
                val level = if (random.nextBoolean()) 1.0 else random.nextDouble(0.001, 1.0)
                val hz = random.nextDouble(20.0, rate / 2.0)
                for (k in 0 until length) {
                    for (ch in 0 until channels) {
                        val v = when (kind) {
                            0 -> 0.0
                            1 -> level * (random.nextDouble() * 2 - 1)
                            2 -> level * sin(2 * PI * hz * k / rate + ch)
                            3 -> level * (if (ch == 0) 1 else -1)
                            4 -> if (k % 97 == 0) level else 0.0
                            else -> 3 * level * sin(2 * PI * hz * k / rate)
                        }
                        pcm[(i + k) * channels + ch] = (v * 32767).toInt().coerceIn(-32768, 32767).toShort()
                    }
                }
                i += length
            }
            val name = "random-$round-$rate-$channels-$kbps"
            val bytes = Mp3Files.encode(rate, channels, kbps, pcm)
            if (frames == 0) {
                assertEquals(0, bytes.size)
                return@repeat
            }
            Mp3Stream.frames(bytes)
            val file = Mp3Files.write("$name.mp3", bytes)
            val (_, probeErrors) = Ffmpeg.probe(file)
            assertEquals("", probeErrors, name)
            val (decoded, errors) = Ffmpeg.decode(file)
            assertEquals("", errors, name)
            assertTrue(decoded.size >= (frames + Mp3Encoder.DELAY) * channels, name)
        }
    }

    @Test
    fun gaplessFieldsTrimToTheExactInput() {
        for ((rate, channels, kbps) in listOf(Triple(44100, 2, 128), Triple(48000, 1, 96), Triple(32000, 2, 64), Triple(22050, 2, 64), Triple(16000, 1, 32))) {
            val signal = Array(channels) { program(rate, it) }
            val pcm = Signals.pcm(*signal)
            val bytes = Mp3Files.encode(rate, channels, kbps, pcm)
            val frames = Mp3Stream.frames(bytes)
            val lame = frames[0].offset + 4 + frames[0].sideInfoSize + if (frames[0].size - 4 - frames[0].sideInfoSize >= 156) 120 else 20
            "LAME".forEachIndexed { i, c -> bytes[lame + i] = c.code.toByte() }
            val crc = Mp3Stream.crc16(bytes, 0, lame + 34)
            bytes[lame + 34] = (crc ushr 8).toByte()
            bytes[lame + 35] = crc.toByte()
            val file = Mp3Files.write("gapless-$rate-$channels-$kbps.mp3", bytes)
            val (fields, _) = Ffmpeg.probe(file)
            val duration = fields.getValue("format.duration").toDouble()
            assertTrue(abs(duration - signal[0].size.toDouble() / rate) < 0.001, "$rate $channels $kbps: ffprobe says $duration s")
            val (decoded, errors) = Ffmpeg.decode(file)
            assertEquals("", errors)
            assertEquals(pcm.size, decoded.size, "$rate Hz $channels ch $kbps kbps decodes to exactly the input length")
            for (ch in 0 until channels) {
                val reference = Signals.channel(pcm, channels, ch)
                val output = Signals.channel(decoded, channels, ch)
                assertEquals(0, Signals.delay(reference, output), "trimmed output is aligned")
                assertTrue(Signals.snr(reference, output, 0) > 10.0)
            }
        }
    }
}
