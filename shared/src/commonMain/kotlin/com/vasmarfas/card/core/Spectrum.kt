package com.vasmarfas.card.core

import kotlinx.coroutines.flow.Flow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

class SpectrumFrame(
    val magnitudesDb: FloatArray,
    val sampleRate: Int,
) {
    val binHz: Double get() = sampleRate.toDouble() / (magnitudesDb.size * 2)

    fun frequencyOf(bin: Int): Double = bin * binHz
}

expect fun microphoneSpectrumFlow(fftSize: Int): Flow<SpectrumFrame>

// samples between two spectra where they are computed from PCM: the window slides, it does not jump
internal const val SpectrumHop = 2048

// for the platforms that hand over PCM rather than a spectrum. Separate from the SciMark kernel,
// which is a reference port and stays untouched
internal object Fft {
    fun magnitudesDb(samples: FloatArray): FloatArray {
        val n = samples.size
        val re = DoubleArray(n)
        val im = DoubleArray(n)
        for (i in 0 until n) {
            val window = 0.5 - 0.5 * cos(2.0 * PI * i / (n - 1))
            re[i] = samples[i] * window
        }
        transform(re, im)
        val bins = n / 2
        return FloatArray(bins) { k ->
            val magnitude = sqrt(re[k] * re[k] + im[k] * im[k]) / bins
            (20.0 * log10(max(magnitude, 1e-12))).toFloat()
        }
    }

    private fun transform(re: DoubleArray, im: DoubleArray) {
        val n = re.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j or bit
            if (i < j) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }
        var length = 2
        while (length <= n) {
            val angle = -2.0 * PI / length
            val wr = cos(angle)
            val wi = sin(angle)
            var i = 0
            while (i < n) {
                var curRe = 1.0
                var curIm = 0.0
                for (k in 0 until length / 2) {
                    val ar = re[i + k]
                    val ai = im[i + k]
                    val br = re[i + k + length / 2] * curRe - im[i + k + length / 2] * curIm
                    val bi = re[i + k + length / 2] * curIm + im[i + k + length / 2] * curRe
                    re[i + k] = ar + br
                    im[i + k] = ai + bi
                    re[i + k + length / 2] = ar - br
                    im[i + k + length / 2] = ai - bi
                    val nextRe = curRe * wr - curIm * wi
                    curIm = curRe * wi + curIm * wr
                    curRe = nextRe
                }
                i += length
            }
            length = length shl 1
        }
    }
}
