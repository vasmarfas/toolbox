package com.vasmarfas.card.tools.device

import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

// Port of netlib.org/benchmark/whetstone.c; weights and score formula are the reference ones.
class Whetstone {
    private val t = 0.499975
    private val t1 = 0.50025
    private val t2 = 2.0

    private val e1 = DoubleArray(5)
    private var j = 0
    private var k = 0
    private var l = 0

    fun majorLoop(loop: Int): Double {
        val n1 = 0 // zero in the reference; kept so module numbering matches the published table
        val n2 = 12 * loop
        val n3 = 14 * loop
        val n4 = 345 * loop
        val n6 = 210 * loop
        val n7 = 32 * loop
        val n8 = 899 * loop
        val n9 = 616 * loop
        val n11 = 93 * loop

        var x1 = 1.0
        var x2 = -1.0
        var x3 = -1.0
        var x4 = -1.0
        repeat(n1) {
            x1 = (x1 + x2 + x3 - x4) * t
            x2 = (x1 + x2 - x3 + x4) * t
            x3 = (x1 - x2 + x3 + x4) * t
            x4 = (-x1 + x2 + x3 + x4) * t
        }

        e1[1] = 1.0
        e1[2] = -1.0
        e1[3] = -1.0
        e1[4] = -1.0
        repeat(n2) {
            e1[1] = (e1[1] + e1[2] + e1[3] - e1[4]) * t
            e1[2] = (e1[1] + e1[2] - e1[3] + e1[4]) * t
            e1[3] = (e1[1] - e1[2] + e1[3] + e1[4]) * t
            e1[4] = (-e1[1] + e1[2] + e1[3] + e1[4]) * t
        }

        repeat(n3) { pa() }

        j = 1
        repeat(n4) {
            j = if (j == 1) 2 else 3
            j = if (j > 2) 0 else 1
            j = if (j < 1) 1 else 0
        }

        j = 1
        k = 2
        l = 3
        repeat(n6) {
            j *= (k - j) * (l - k)
            k = l * k - (l - j) * k
            l = (l - k) * (k + j)
            e1[l - 1] = (j + k + l).toDouble()
            e1[k - 1] = (j * k * l).toDouble()
        }

        var x = 0.5
        var y = 0.5
        repeat(n7) {
            x = t * atan(t2 * sin(x) * cos(x) / (cos(x + y) + cos(x - y) - 1.0))
            y = t * atan(t2 * sin(y) * cos(y) / (cos(x + y) + cos(x - y) - 1.0))
        }

        var z = 1.0
        x = 1.0
        y = 1.0
        repeat(n8) { z = p3(x, y) }

        j = 1
        k = 2
        l = 3
        e1[1] = 1.0
        e1[2] = 2.0
        e1[3] = 3.0
        repeat(n9) { p0() }

        x = 0.75
        repeat(n11) { x = sqrt(exp(ln(x) / t1)) }

        return x1 + x2 + x3 + x4 + x + y + z + e1[1] + e1[2] + e1[3] + e1[4]
    }

    private fun pa() {
        var n = 0
        do {
            e1[1] = (e1[1] + e1[2] + e1[3] - e1[4]) * t
            e1[2] = (e1[1] + e1[2] - e1[3] + e1[4]) * t
            e1[3] = (e1[1] - e1[2] + e1[3] + e1[4]) * t
            e1[4] = (-e1[1] + e1[2] + e1[3] + e1[4]) / t2
            n += 1
        } while (n < 6)
    }

    private fun p0() {
        e1[j] = e1[k]
        e1[k] = e1[l]
        e1[l] = e1[j]
    }

    private fun p3(x: Double, y: Double): Double {
        var x1 = x
        var y1 = y
        x1 = t * (x1 + y1)
        y1 = t * (x1 + y1)
        return (x1 + y1) / t2
    }

    companion object {
        // the reference driver prints KIPS / 1000, so the divisor is a thousand, not a million
        fun mwips(loop: Int, majorLoops: Int, seconds: Double): Double =
            if (seconds <= 0.0) 0.0 else (100.0 * loop * majorLoops) / seconds / 1000.0
    }
}
