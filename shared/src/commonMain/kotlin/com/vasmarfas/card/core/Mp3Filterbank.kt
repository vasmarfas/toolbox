package com.vasmarfas.card.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal const val NORMAL_BLOCK = 0
internal const val START_BLOCK = 1
internal const val SHORT_BLOCK = 2
internal const val STOP_BLOCK = 3

private const val GRANULE = 576
private const val HISTORY = 480

private val WINDOW = DoubleArray(512) { Mp3Tables.analysisWindow[it] / 2097152.0 }

// cos((2k + 1) m pi / 64) with the 64-point matrixing of C.1.3 folded onto its 32 distinct inputs
private val MATRIX = DoubleArray(32 * 32) { cos((2 * (it / 32) + 1) * (it % 32) * PI / 64) }

// block types 0, 1 and 3, ISO/IEC 11172-3 2.4.3.4.10.2, type 2 has no long window
private val LONG_WINDOWS = Array(4) { type ->
    DoubleArray(36) { n ->
        when {
            type == START_BLOCK && n >= 30 || type == STOP_BLOCK && n < 6 -> 0.0
            type == START_BLOCK && n >= 24 -> sin(PI / 12 * (n - 18 + 0.5))
            type == START_BLOCK && n >= 18 || type == STOP_BLOCK && n in 12 until 18 -> 1.0
            type == STOP_BLOCK && n < 12 -> sin(PI / 12 * (n - 6 + 0.5))
            else -> sin(PI / 36 * (n + 0.5))
        }
    }
}

// window included, scaled by 1/9 so the standard IMDCT gives back the input
private val LONG_KERNELS = Array(4) { type ->
    DoubleArray(18 * 36) {
        val k = it / 36
        val n = it % 36
        LONG_WINDOWS[type][n] * cos(PI / 72 * (2 * n + 19) * (2 * k + 1)) / 9
    }
}

// scaled by 1/3 for the same reason
private val SHORT_KERNEL = DoubleArray(6 * 12) {
    val k = it / 12
    val n = it % 12
    sin(PI / 12 * (n + 0.5)) * cos(PI / 24 * (2 * n + 7) * (2 * k + 1)) / 3
}

// c(i), ISO/IEC 11172-3 Table B.9
private val ALIAS = doubleArrayOf(-0.6, -0.535, -0.33, -0.185, -0.095, -0.041, -0.0142, -0.0037)
private val CS = DoubleArray(8) { 1 / sqrt(1 + ALIAS[it] * ALIAS[it]) }
private val CA = DoubleArray(8) { ALIAS[it] / sqrt(1 + ALIAS[it] * ALIAS[it]) }

// output is in the scale the decoder dequantizes to: a full-scale sine carries about 1 per granule
// (per window in short blocks). gains weights each polyphase band and is the lowpass as well
internal class Mp3Filterbank(private val gains: DoubleArray, private val shortEdges: IntArray) {
    private val input = DoubleArray(HISTORY + GRANULE)
    private val partial = DoubleArray(64)
    private val folded = DoubleArray(32)
    private val span = DoubleArray(36)
    private val windows = DoubleArray(GRANULE)

    fun polyphase(samples: DoubleArray, offset: Int, subbands: DoubleArray) {
        samples.copyInto(input, HISTORY, offset, offset + GRANULE)
        for (slot in 0 until 18) {
            val newest = HISTORY + 31 + 32 * slot
            for (i in 0 until 64) {
                var sum = 0.0
                var k = i
                while (k < 512) {
                    sum += WINDOW[k] * input[newest - k]
                    k += 64
                }
                partial[i] = sum
            }
            folded[0] = partial[16]
            for (m in 1..16) folded[m] = partial[16 + m] + partial[16 - m]
            for (m in 17 until 32) folded[m] = partial[16 + m] - partial[80 - m]
            for (band in 0 until 32) {
                if (gains[band] == 0.0) {
                    subbands[band * 18 + slot] = 0.0
                    continue
                }
                val row = band * 32
                var sum = 0.0
                for (m in 0 until 32) sum += MATRIX[row + m] * folded[m]
                subbands[band * 18 + slot] = if (band and slot and 1 == 1) -sum else sum
            }
        }
        input.copyInto(input, 0, GRANULE, GRANULE + HISTORY)
    }

    // short blocks come out in Huffman order: band by band, window by window inside a band
    fun transform(previous: DoubleArray, current: DoubleArray, blockType: Int, lines: DoubleArray) {
        if (blockType == SHORT_BLOCK) {
            shortTransform(previous, current, lines)
            return
        }
        val kernel = LONG_KERNELS[blockType]
        for (band in 0 until 32) {
            val base = band * 18
            val gain = gains[band]
            if (gain == 0.0) {
                lines.fill(0.0, base, base + 18)
                continue
            }
            for (k in 0 until 18) {
                val row = k * 36
                var sum = 0.0
                for (n in 0 until 18) sum += kernel[row + n] * previous[base + n] + kernel[row + 18 + n] * current[base + n]
                lines[base + k] = sum * gain
            }
        }
        for (band in 1 until 32) {
            val lower = band * 18 - 1
            val upper = band * 18
            for (i in 0 until 8) {
                val bu = lines[lower - i]
                val bd = lines[upper + i]
                lines[lower - i] = bu * CS[i] + bd * CA[i]
                lines[upper + i] = bd * CS[i] - bu * CA[i]
            }
        }
    }

    private fun shortTransform(previous: DoubleArray, current: DoubleArray, lines: DoubleArray) {
        for (band in 0 until 32) {
            val gain = gains[band]
            val base = band * 18
            previous.copyInto(span, 0, base, base + 18)
            current.copyInto(span, 18, base, base + 18)
            for (w in 0 until 3) {
                for (k in 0 until 6) {
                    var sum = 0.0
                    if (gain != 0.0) {
                        val row = k * 12
                        val start = 6 + 6 * w
                        for (n in 0 until 12) sum += SHORT_KERNEL[row + n] * span[start + n]
                    }
                    windows[w * 192 + band * 6 + k] = sum * gain
                }
            }
        }
        var p = 0
        for (b in 0 until shortEdges.size - 1) {
            for (w in 0 until 3) {
                for (f in shortEdges[b] until shortEdges[b + 1]) lines[p++] = windows[w * 192 + f]
            }
        }
    }
}
