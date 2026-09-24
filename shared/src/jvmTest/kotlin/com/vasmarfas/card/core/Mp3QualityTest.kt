package com.vasmarfas.card.core

import kotlin.math.log10
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Mp3QualityTest {
    private class Case(val name: String, val rate: Int, val kbps: Int, val floor: Double, val channels: List<DoubleArray>)

    private fun decode(name: String, rate: Int, kbps: Int, pcm: ShortArray, channels: Int): ShortArray {
        val file = Mp3Files.write("$name.mp3", Mp3Files.encode(rate, channels, kbps, pcm))
        val (decoded, errors) = Ffmpeg.decode(file)
        assertEquals("", errors, name)
        return decoded
    }

    private fun snr(name: String, rate: Int, kbps: Int, vararg signal: DoubleArray): List<Double> {
        val pcm = Signals.pcm(*signal)
        val decoded = decode(name, rate, kbps, pcm, signal.size)
        return signal.indices.map { ch ->
            val reference = Signals.channel(pcm, signal.size, ch)
            val output = Signals.channel(decoded, signal.size, ch)
            val lag = Signals.delay(reference, output)
            assertEquals(Mp3Encoder.DELAY, lag, "$name: decoded output lags by the documented delay")
            Signals.snr(reference, output, lag)
        }
    }

    @Test
    fun snrOfTestSignals() {
        val r = 44100
        val s = 3.0
        val sine1k = Signals.sine(r, s, 1000.0, 0.5)
        val pink = Signals.pink(r, s, 0.5)
        val cases = listOf(
            Case("sine 1 kHz -6 dB", r, 128, 30.0, listOf(sine1k, sine1k)),
            Case("sine 1 kHz -40 dB", r, 128, 30.0, listOf(Signals.sine(r, s, 1000.0, 0.01), Signals.sine(r, s, 1000.0, 0.01))),
            Case("sine 60 Hz -6 dB", r, 128, 30.0, listOf(Signals.sine(r, s, 60.0, 0.5))),
            Case("sine 5 kHz -20 dB", r, 128, 30.0, listOf(Signals.sine(r, s, 5000.0, 0.1))),
            Case("sine 15 kHz -12 dB", r, 128, 30.0, listOf(Signals.sine(r, s, 15000.0, 0.25))),
            Case("sine 1 kHz 64k", r, 64, 30.0, listOf(sine1k, sine1k)),
            Case("sine 1 kHz 22 kHz 32k", 22050, 32, 30.0, listOf(Signals.sine(22050, s, 1000.0, 0.5))),
            Case("sine 1 kHz 16 kHz 24k", 16000, 24, 25.0, listOf(Signals.sine(16000, s, 1000.0, 0.5))),
            Case("sweep", r, 128, 18.0, listOf(Signals.sweep(r, s, 20.0, 20000.0, 0.5))),
            Case("square 440 Hz", r, 128, 20.0, listOf(Signals.square(r, s, 440.0, 0.3))),
            Case("white noise mono", r, 128, 7.0, listOf(Signals.white(r, s, 0.5))),
            Case("pink noise mono", r, 128, 14.0, listOf(pink)),
            Case("pink noise mono 192k", r, 192, 17.0, listOf(pink)),
            Case("pink noise stereo", r, 128, 9.0, listOf(Signals.pink(r, s, 0.5, 3), Signals.pink(r, s, 0.5, 4))),
            Case("pink noise dual mono", r, 128, 12.0, listOf(pink, pink)),
            Case("click train", r, 128, 6.0, listOf(Signals.clicks(r, s, 4410, 0.9))),
            Case("dc offset", r, 128, 30.0, listOf(DoubleArray(sine1k.size) { 0.3 + 0.4 * sine1k[it] })),
            Case("clipped sine", r, 128, 18.0, listOf(DoubleArray(sine1k.size) { (sine1k[it] * 4).coerceIn(-0.999, 0.999) })),
            Case("full-scale square", r, 128, 15.0, listOf(Signals.square(r, s, 1000.0, 1.0), Signals.square(r, s, 1000.0, 1.0))),
            Case("full-scale noise", r, 320, 10.0, listOf(Signals.white(r, s, 1.0, 7), Signals.white(r, s, 1.0, 8))),
        )
        val report = StringBuilder("SNR, dB\n")
        for (case in cases) {
            val values = snr(case.name.replace(' ', '-'), case.rate, case.kbps, *case.channels.toTypedArray())
            report.appendLine("%-24s %5d Hz %3d kbps %s".format(case.name, case.rate, case.kbps, values.joinToString(" / ") { "%.1f".format(it) }))
            assertTrue(values.all { it >= case.floor }, "${case.name}: $values below ${case.floor}")
        }
        println(report)
    }

    @Test
    fun programMaterialFromFfmpeg() {
        val r = 44100
        val sources = listOf(
            "chord" to "aevalsrc=0.25*sin(2*PI*261.63*t)*exp(-2*mod(t\\,0.5))+0.2*sin(2*PI*329.63*t)+0.2*sin(2*PI*392*t)+0.1*sin(2*PI*523.25*t):s=$r:c=stereo",
            "noise" to "anoisesrc=color=pink:amplitude=0.4:r=$r,bandpass=f=2000:width_type=o:w=3",
            "mix" to "sine=f=220:r=$r,volume=0.3[a];anoisesrc=color=brown:amplitude=0.3:r=$r[b];[a][b]amix=inputs=2",
        )
        val report = StringBuilder("Program material, 44.1 kHz stereo 128 kbps\n")
        for ((name, graph) in sources) {
            val pcm = Ffmpeg.generate(graph, r, 2, 4.0)
            val decoded = decode("program-$name", r, 128, pcm, 2)
            val values = (0 until 2).map { ch ->
                val reference = Signals.channel(pcm, 2, ch)
                val output = Signals.channel(decoded, 2, ch)
                Signals.snr(reference, output, Signals.delay(reference, output))
            }
            report.appendLine("%-6s %s".format(name, values.joinToString(" / ") { "%.1f dB".format(it) }))
            assertTrue(values.all { it > 15.0 }, "$name: $values")
        }
        println(report)
    }

    @Test
    fun stereoChannelsStaySeparate() {
        val r = 44100
        val s = 3.0
        val tone = Signals.sine(r, s, 1000.0, 0.5)
        val silence = DoubleArray(tone.size)
        for (kbps in listOf(64, 128, 320)) {
            val pcm = Signals.pcm(tone, silence)
            val decoded = decode("left-only-$kbps", r, kbps, pcm, 2)
            val right = Signals.channel(decoded, 2, 1)
            val leak = Signals.level(tone, right, Mp3Encoder.DELAY)
            println("left-only tone at $kbps kbps: right channel at $leak dB")
            assertTrue(leak < -30, "right channel at $leak dB")
        }
        val left = Signals.sine(r, s, 440.0, 0.4)
        val right = Signals.sine(r, s, 660.0, 0.4)
        val pcm = Signals.pcm(left, right)
        val decoded = decode("two-tone", r, 128, pcm, 2)
        val outLeft = Signals.channel(decoded, 2, 0)
        val outRight = Signals.channel(decoded, 2, 1)
        val snrLeft = Signals.snr(left, outLeft, Mp3Encoder.DELAY)
        val snrRight = Signals.snr(right, outRight, Mp3Encoder.DELAY)
        println("two-tone stereo: left %.1f dB, right %.1f dB".format(snrLeft, snrRight))
        assertTrue(snrLeft > 30 && snrRight > 30)
    }

    @Test
    fun midSideHelpsCorrelatedStereo() {
        val r = 44100
        val s = 3.0
        val common = Signals.lowpass(Signals.pink(r, s, 0.4, 9), r, 12000.0)
        val a = Signals.lowpass(Signals.pink(r, s, 0.03, 10), r, 12000.0)
        val b = Signals.lowpass(Signals.pink(r, s, 0.03, 11), r, 12000.0)
        val report = StringBuilder("Mid/side at 128 kbps against two 64 kbps mono streams\n")
        for ((name, gain) in listOf("dual mono" to 4.0, "near mono" to 1.5)) {
            val left = if (name == "dual mono") common else DoubleArray(common.size) { common[it] + a[it] }
            val right = if (name == "dual mono") common else DoubleArray(common.size) { common[it] + b[it] }
            val pcm = Signals.pcm(left, right)
            val midSideFrames = Mp3Stream.frames(Mp3Files.encode(r, 2, 128, pcm)).drop(1).count { it.modeExtension == 2 }
            val joint = decode("joint-$name", r, 128, pcm, 2)
            val separateLeft = decode("separate-left-$name", r, 64, Signals.pcm(left), 1)
            val separateRight = decode("separate-right-$name", r, 64, Signals.pcm(right), 1)
            val jointSnr = listOf(
                Signals.snr(left, Signals.channel(joint, 2, 0), Mp3Encoder.DELAY),
                Signals.snr(right, Signals.channel(joint, 2, 1), Mp3Encoder.DELAY),
            )
            val separateSnr = listOf(
                Signals.snr(left, Signals.channel(separateLeft, 1, 0), Mp3Encoder.DELAY),
                Signals.snr(right, Signals.channel(separateRight, 1, 0), Mp3Encoder.DELAY),
            )
            report.appendLine(
                "%-9s mid/side in %d frames: joint %s dB, separate %s dB".format(
                    name, midSideFrames, jointSnr.joinToString(" / ") { "%.1f".format(it) }, separateSnr.joinToString(" / ") { "%.1f".format(it) },
                ),
            )
            assertTrue(midSideFrames > 0, name)
            assertTrue(jointSnr.indices.all { jointSnr[it] > separateSnr[it] + gain }, "$name: $jointSnr against $separateSnr")
        }
        println(report)
    }

    private fun blockTypes(bytes: ByteArray): List<Int> = Mp3Stream.frames(bytes).drop(1).flatMap { frame ->
        Mp3Stream.blockTypes(bytes, frame).filterIndexed { i, _ -> frame.mode == 3 || i % 2 == 0 }
    }

    @Test
    fun shortBlocksFollowTransients() {
        val r = 44100
        val (bursts, attacks) = Signals.bursts(r, 4.0, 0.6)
        for (channels in 1..2) {
            val types = blockTypes(Mp3Files.encode(r, channels, 128, Signals.pcm(*Array(channels) { bursts })))
            val expected = attacks.map { t -> ((t + 225) / 32).let { m -> m / 18 + if (m % 18 < 12) 0 else 1 } }.toSet()
            for (g in types.indices) {
                assertEquals(g in expected, types[g] == SHORT_BLOCK, "granule $g of ${types.joinToString("")}")
                if (types[g] == SHORT_BLOCK) {
                    assertTrue(types[g - 1] == START_BLOCK || types[g - 1] == SHORT_BLOCK)
                    assertTrue(types[g + 1] == STOP_BLOCK || types[g + 1] == SHORT_BLOCK)
                }
            }
        }
        val steady = listOf(
            Signals.sine(r, 3.0, 1000.0, 0.5),
            Signals.pink(r, 3.0, 0.5),
            Signals.sweep(r, 3.0, 20.0, 20000.0, 0.5),
            Signals.square(r, 3.0, 440.0, 0.3),
        )
        for (signal in steady) {
            val types = blockTypes(Mp3Files.encode(r, 1, 128, Signals.pcm(signal)))
            assertTrue(types.drop(3).none { it != NORMAL_BLOCK }, "steady signal switched blocks: ${types.joinToString("")}")
        }
    }

    // decoded energy in the silence before each burst relative to the bursts: 2-20 ms before the attack,
    // and 12-30 ms before it, beyond the reach of a short window
    @Test
    fun preEchoStaysShort() {
        val r = 44100
        val (bursts, attacks) = Signals.bursts(r, 4.0, 0.6)
        val pcm = Signals.pcm(bursts)
        val reference = Signals.channel(pcm, 1, 0)
        var burstEnergy = 0.0
        for (a in attacks) for (i in a until a + r / 50) burstEnergy += reference[i] * reference[i]
        val report = StringBuilder("Pre-echo before noise bursts, mono\n")
        for (kbps in listOf(64, 128, 192)) {
            val decoded = Signals.channel(decode("bursts-$kbps", r, kbps, pcm, 1), 1, 0)
            fun echo(fromMs: Int, toMs: Int): Double {
                var sum = 0.0
                for (a in attacks) {
                    for (i in a - r * fromMs / 1000 until a - r * toMs / 1000) sum += decoded[i + Mp3Encoder.DELAY] * decoded[i + Mp3Encoder.DELAY]
                }
                return 10 * log10(sum / burstEnergy + 1e-30)
            }
            val near = echo(20, 2)
            val far = echo(30, 12)
            report.appendLine("%3d kbps: %.1f dB within 20 ms, %.1f dB earlier than 12 ms".format(kbps, near, far))
            assertTrue(near < -28, "$kbps kbps: pre-echo $near dB")
            assertTrue(far < -60, "$kbps kbps: pre-echo reaches back, $far dB")
        }
        println(report)
    }

    @Test
    fun silenceStaysSilent() {
        for ((rate, channels, kbps) in listOf(Triple(44100, 2, 128), Triple(16000, 1, 8), Triple(48000, 2, 320))) {
            val decoded = decode("silence-$rate-$channels-$kbps", rate, kbps, ShortArray(rate * channels * 2), channels)
            assertTrue(decoded.all { it.toInt() == 0 }, "$rate Hz decodes to digital silence")
        }
    }

    @Test
    fun oneSampleMakesAValidFile() {
        for ((rate, channels) in listOf(44100 to 2, 22050 to 1)) {
            val pcm = ShortArray(channels) { 12000 }
            val file = Mp3Files.write("one-sample-$rate.mp3", Mp3Files.encode(rate, channels, 64, pcm))
            val (fields, probeErrors) = Ffmpeg.probe(file)
            assertEquals("", probeErrors)
            assertEquals("mp3", fields["streams.stream.0.codec_name"])
            val (decoded, errors) = Ffmpeg.decode(file)
            assertEquals("", errors)
            assertTrue(decoded.size >= (1 + Mp3Encoder.DELAY) * channels)
        }
    }

    @Test
    fun encodesFasterThanRealTime() {
        val r = 44100
        val seconds = 20.0
        val music = Ffmpeg.generate(
            "aevalsrc=0.2*sin(2*PI*220*t)*exp(-3*mod(t\\,0.25))+0.1*sin(2*PI*330*t)+0.05*sin(2*PI*1760*t):s=$r:c=stereo,aformat=channel_layouts=stereo[a];" +
                "anoisesrc=color=pink:amplitude=0.15:r=$r,aformat=channel_layouts=stereo[b];[a][b]amix=inputs=2",
            r, 2, seconds,
        )
        repeat(2) { Mp3Encoder(r, 2, 128).let { it.encode(music.copyOf(r * 2 * 2)) + it.finish() } }
        val report = StringBuilder("Encoding speed, best of 3\n")
        for ((kbps, channels) in listOf(128 to 2, 320 to 2, 64 to 1)) {
            val input = if (channels == 2) music else ShortArray(music.size / 2) { music[it * 2] }
            var best = Double.MAX_VALUE
            repeat(3) {
                val start = System.nanoTime()
                val encoder = Mp3Encoder(r, channels, kbps)
                encoder.encode(input)
                encoder.finish()
                best = minOf(best, (System.nanoTime() - start) / 1e9)
            }
            val speed = seconds / best
            report.appendLine("44.1 kHz %d ch %3d kbps: %.3f s for %.0f s of audio, %.0fx real time".format(channels, kbps, best, seconds, speed))
            assertTrue(speed >= 20, "$kbps kbps: only ${speed}x real time")
        }
        println(report)
    }
}
