package com.vasmarfas.card.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// decoder side of the Layer III hybrid filterbank straight from the formulas of ISO/IEC 11172-3 2.4.3.4
class ReferenceSynthesis(private val shortEdges: IntArray) {
    private val overlap = DoubleArray(576)
    private val v = DoubleArray(1024)
    private val xr = DoubleArray(576)

    fun granule(lines: DoubleArray, blockType: Int, out: DoubleArray, offset: Int) {
        if (blockType == SHORT_BLOCK) {
            var p = 0
            for (b in 0 until shortEdges.size - 1) {
                for (w in 0 until 3) {
                    for (f in shortEdges[b] until shortEdges[b + 1]) xr[w * 192 + f] = lines[p++]
                }
            }
        } else {
            lines.copyInto(xr)
            for (sb in 1 until 32) {
                for (i in 0 until 8) {
                    val bu = xr[sb * 18 - 1 - i]
                    val bd = xr[sb * 18 + i]
                    xr[sb * 18 - 1 - i] = bu * CS[i] - bd * CA[i]
                    xr[sb * 18 + i] = bd * CS[i] + bu * CA[i]
                }
            }
        }
        val subband = Array(18) { DoubleArray(32) }
        for (sb in 0 until 32) {
            val z = DoubleArray(36)
            if (blockType == SHORT_BLOCK) {
                for (w in 0 until 3) {
                    for (i in 0 until 12) {
                        var sum = 0.0
                        for (k in 0 until 6) sum += xr[w * 192 + sb * 6 + k] * cos(PI / 24 * (2 * i + 7) * (2 * k + 1))
                        z[6 + 6 * w + i] += sum * sin(PI / 12 * (i + 0.5))
                    }
                }
            } else {
                for (i in 0 until 36) {
                    var sum = 0.0
                    for (k in 0 until 18) sum += xr[sb * 18 + k] * cos(PI / 72 * (2 * i + 19) * (2 * k + 1))
                    z[i] = sum * window(blockType, i)
                }
            }
            for (t in 0 until 18) {
                var s = z[t] + overlap[sb * 18 + t]
                overlap[sb * 18 + t] = z[t + 18]
                if (sb % 2 == 1 && t % 2 == 1) s = -s
                subband[t][sb] = s
            }
        }
        for (t in 0 until 18) synthesize(subband[t], out, offset + 32 * t)
    }

    private fun window(type: Int, i: Int): Double = when {
        type == START_BLOCK && i >= 30 || type == STOP_BLOCK && i < 6 -> 0.0
        type == START_BLOCK && i >= 24 -> sin(PI / 12 * (i - 18 + 0.5))
        type == START_BLOCK && i >= 18 || type == STOP_BLOCK && i in 12 until 18 -> 1.0
        type == STOP_BLOCK && i < 12 -> sin(PI / 12 * (i - 6 + 0.5))
        else -> sin(PI / 36 * (i + 0.5))
    }

    private fun synthesize(s: DoubleArray, out: DoubleArray, offset: Int) {
        for (i in 1023 downTo 64) v[i] = v[i - 64]
        for (i in 0 until 64) {
            var sum = 0.0
            for (k in 0 until 32) sum += cos((16 + i) * (2 * k + 1) * PI / 64) * s[k]
            v[i] = sum
        }
        for (j in 0 until 32) {
            var sum = 0.0
            for (i in 0 until 16) {
                val u = if (i % 2 == 0) v[64 * i + j] else v[64 * (i - 1) + 96 + j]
                sum += u * Mp3Tables.analysisWindow[j + 32 * i] * 32.0 / 2097152.0
            }
            out[offset + j] = sum
        }
    }

    private companion object {
        val C = doubleArrayOf(-0.6, -0.535, -0.33, -0.185, -0.095, -0.041, -0.0142, -0.0037)
        val CS = DoubleArray(8) { 1 / sqrt(1 + C[it] * C[it]) }
        val CA = DoubleArray(8) { C[it] / sqrt(1 + C[it] * C[it]) }
    }
}
