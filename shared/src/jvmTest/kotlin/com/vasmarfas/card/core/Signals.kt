package com.vasmarfas.card.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

object Signals {
    fun sine(rate: Int, seconds: Double, hz: Double, amplitude: Double): DoubleArray =
        DoubleArray((rate * seconds).toInt()) { amplitude * sin(2 * PI * hz * it / rate) }

    fun sweep(rate: Int, seconds: Double, from: Double, to: Double, amplitude: Double): DoubleArray {
        val n = (rate * seconds).toInt()
        val k = ln(to / from)
        return DoubleArray(n) { amplitude * sin(2 * PI * from * seconds / k * (exp(k * it / n) - 1)) }
    }

    fun white(rate: Int, seconds: Double, amplitude: Double, seed: Int = 1): DoubleArray {
        val random = Random(seed)
        return DoubleArray((rate * seconds).toInt()) { amplitude * (random.nextDouble() * 2 - 1) }
    }

    fun pink(rate: Int, seconds: Double, amplitude: Double, seed: Int = 2): DoubleArray {
        val n = (rate * seconds).toInt()
        var size = 1
        while (size < n) size *= 2
        val re = white(rate, size.toDouble() / rate, 1.0, seed).copyOf(size)
        val im = DoubleArray(size)
        fft(re, im, false)
        val corner = 20.0 * size / rate
        for (k in 0 until size) {
            val bin = minOf(k, size - k).toDouble()
            val gain = 1 / sqrt(maxOf(bin, corner))
            re[k] *= gain
            im[k] *= gain
        }
        fft(re, im, true)
        return normalize(re.copyOf(n), amplitude)
    }

    fun square(rate: Int, seconds: Double, hz: Double, amplitude: Double): DoubleArray =
        DoubleArray((rate * seconds).toInt()) { if ((it * hz / rate) % 1.0 < 0.5) amplitude else -amplitude }

    fun bursts(rate: Int, seconds: Double, amplitude: Double, seed: Int = 5): Pair<DoubleArray, List<Int>> {
        val random = Random(seed)
        val x = DoubleArray((rate * seconds).toInt())
        val attacks = mutableListOf<Int>()
        var t = rate / 4 + 123
        while (t < x.size - rate / 4) {
            attacks += t
            for (i in 0 until rate / 20) x[t + i] = amplitude * (random.nextDouble() * 2 - 1) * exp(-i / (rate * 0.01))
            t += rate / 3 + random.nextInt(0, 600)
        }
        return x to attacks
    }

    fun clicks(rate: Int, seconds: Double, period: Int, amplitude: Double): DoubleArray =
        DoubleArray((rate * seconds).toInt()) { if (it % period == period / 2) amplitude else 0.0 }

    fun lowpass(x: DoubleArray, rate: Int, hz: Double): DoubleArray {
        var size = 1
        while (size < x.size) size *= 2
        val re = x.copyOf(size)
        val im = DoubleArray(size)
        fft(re, im, false)
        val cut = (hz / rate * size).toInt()
        for (k in cut..size - cut) {
            re[k] = 0.0
            im[k] = 0.0
        }
        fft(re, im, true)
        return DoubleArray(x.size) { re[it] / size }
    }

    fun normalize(x: DoubleArray, peak: Double): DoubleArray {
        var max = 0.0
        for (v in x) max = maxOf(max, abs(v))
        return if (max == 0.0) x else DoubleArray(x.size) { x[it] / max * peak }
    }

    fun pcm(vararg channels: DoubleArray): ShortArray {
        val n = channels[0].size
        return ShortArray(n * channels.size) { i ->
            (channels[i % channels.size][i / channels.size] * 32767).roundToInt().coerceIn(-32768, 32767).toShort()
        }
    }

    fun channel(pcm: ShortArray, channels: Int, index: Int): DoubleArray =
        DoubleArray(pcm.size / channels) { pcm[it * channels + index] / 32768.0 }

    // samples outside decoded count as silence, so the onset and the end tell the lags of a periodic
    // signal apart
    fun delay(reference: DoubleArray, decoded: DoubleArray, range: Int = 2000): Int {
        var size = 1
        while (size < reference.size + decoded.size + range) size *= 2
        val xr = DoubleArray(size)
        val xi = DoubleArray(size)
        val yr = DoubleArray(size)
        val yi = DoubleArray(size)
        reference.copyInto(xr)
        decoded.copyInto(yr)
        fft(xr, xi, false)
        fft(yr, yi, false)
        for (k in 0 until size) {
            val re = xr[k] * yr[k] + xi[k] * yi[k]
            val im = xr[k] * yi[k] - xi[k] * yr[k]
            xr[k] = re
            xi[k] = im
        }
        fft(xr, xi, true)
        var best = 0
        for (lag in -range..range) if (xr[(lag + size) % size] > xr[(best + size) % size]) best = lag
        return best
    }

    private fun fft(re: DoubleArray, im: DoubleArray, inverse: Boolean) {
        val n = re.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                re[i] = re[j].also { re[j] = re[i] }
                im[i] = im[j].also { im[j] = im[i] }
            }
        }
        var length = 2
        while (length <= n) {
            val angle = 2 * PI / length * if (inverse) 1 else -1
            val wr = cos(angle)
            val wi = sin(angle)
            for (start in 0 until n step length) {
                var cr = 1.0
                var ci = 0.0
                for (k in 0 until length / 2) {
                    val a = start + k
                    val b = a + length / 2
                    val tr = re[b] * cr - im[b] * ci
                    val ti = re[b] * ci + im[b] * cr
                    re[b] = re[a] - tr
                    im[b] = im[a] - ti
                    re[a] += tr
                    im[a] += ti
                    val next = cr * wr - ci * wi
                    ci = cr * wi + ci * wr
                    cr = next
                }
            }
            length *= 2
        }
    }

    fun snr(reference: DoubleArray, decoded: DoubleArray, lag: Int, margin: Int = 1152): Double {
        var signal = 0.0
        var noise = 0.0
        for (i in margin until reference.size - margin) {
            val j = i + lag
            val y = if (j in decoded.indices) decoded[j] else 0.0
            signal += reference[i] * reference[i]
            noise += (reference[i] - y) * (reference[i] - y)
        }
        return if (noise == 0.0) 200.0 else 10 * log10(signal / noise)
    }

    fun level(reference: DoubleArray, x: DoubleArray, lag: Int, margin: Int = 1152): Double {
        var r = 0.0
        var e = 0.0
        for (i in margin until reference.size - margin) {
            val j = i + lag
            if (j in x.indices) e += x[j] * x[j]
            r += reference[i] * reference[i]
        }
        return 10 * log10(e / r)
    }
}
