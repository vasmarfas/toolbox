package com.vasmarfas.card.tools.device

import kotlin.math.abs
import kotlin.math.sin

// Port of SciMark 2.0 (NIST, math.nist.gov/scimark) with the reference "small" problem sizes.
object SciMark {
    const val FFT_SIZE = 1024
    const val SOR_SIZE = 100
    const val SPARSE_SIZE_M = 1000
    const val SPARSE_SIZE_NZ = 5000
    const val LU_SIZE = 100

    fun fft(minSeconds: Double, seed: Int = 101): Double {
        val twoN = 2 * FFT_SIZE
        val x = randomVector(twoN, SciMarkRandom(seed))
        val flops = fftNumFlops(FFT_SIZE)
        return tune(minSeconds, flops) { cycles ->
            repeat(cycles) {
                fftTransform(twoN, x)
                fftInverse(twoN, x)
            }
        }
    }

    fun sor(minSeconds: Double, seed: Int = 101): Double {
        val n = SOR_SIZE
        val g = randomMatrix(n, n, SciMarkRandom(seed))
        return tuneVariable(minSeconds, { cycles -> (n - 1.0) * (n - 1.0) * cycles * 6.0 }) { cycles ->
            sorExecute(n, n, 1.25, g, cycles)
        }
    }

    fun monteCarlo(minSeconds: Double): Double =
        tuneVariable(minSeconds, { cycles -> cycles * 4.0 }) { cycles -> monteCarloIntegrate(cycles) }

    fun sparseMatMult(minSeconds: Double, seed: Int = 101): Double {
        val n = SPARSE_SIZE_M
        val nz = SPARSE_SIZE_NZ
        val random = SciMarkRandom(seed)
        val x = randomVector(n, random)
        val y = DoubleArray(n)
        val nr = nz / n
        val anz = nr * n
        val value = randomVector(anz, random)
        val col = IntArray(nz)
        val row = IntArray(n + 1)
        row[0] = 0
        for (r in 0 until n) {
            val rowR = row[r]
            row[r + 1] = rowR + nr
            val step = maxOf(r / nr, 1)
            for (i in 0 until nr) col[rowR + i] = i * step
        }
        val actualNz = (nz / n) * n
        return tuneVariable(minSeconds, { cycles -> actualNz * 2.0 * cycles }) { cycles ->
            sparseMatMult(n, y, value, row, col, x, cycles)
        }
    }

    fun lu(minSeconds: Double, seed: Int = 101): Double {
        val n = LU_SIZE
        val a = randomMatrix(n, n, SciMarkRandom(seed))
        val lu = Array(n) { DoubleArray(n) }
        val pivot = IntArray(n)
        val flops = 2.0 * n * n * n / 3.0
        return tune(minSeconds, flops) { cycles ->
            repeat(cycles) {
                for (i in 0 until n) a[i].copyInto(lu[i])
                luFactor(n, n, lu, pivot)
            }
        }
    }

    private inline fun tune(minSeconds: Double, flopsPerCycle: Double, body: (Int) -> Unit): Double =
        tuneVariable(minSeconds, { cycles -> flopsPerCycle * cycles }, body)

    private inline fun tuneVariable(
        minSeconds: Double,
        flops: (Int) -> Double,
        body: (Int) -> Unit,
    ): Double {
        var cycles = 1
        while (true) {
            val seconds = timeSeconds { body(cycles) }
            if (seconds >= minSeconds || cycles >= MaxCycles) {
                return if (seconds <= 0.0) 0.0 else flops(cycles) / seconds * 1.0e-6
            }
            cycles *= 2
        }
    }

    private const val MaxCycles = 1 shl 26

    private fun fftNumFlops(n: Int): Double {
        val logN = intLog2(n).toDouble()
        return (5.0 * n - 2) * logN + 2 * (n + 1)
    }

    private fun intLog2(n: Int): Int {
        var k = 1
        var log = 0
        while (k < n) {
            k *= 2
            log++
        }
        return log
    }

    internal fun fftTransform(n: Int, data: DoubleArray) = fftTransformInternal(n, data, -1)

    internal fun fftInverse(n: Int, data: DoubleArray) {
        fftTransformInternal(n, data, 1)
        val norm = 1.0 / (n / 2)
        for (i in 0 until n) data[i] *= norm
    }

    private fun fftTransformInternal(n: Int, data: DoubleArray, direction: Int) {
        val half = n / 2
        if (half == 1 || n == 0) return
        val logn = intLog2(half)
        fftBitReverse(n, data)
        var dual = 1
        for (bit in 0 until logn) {
            var wReal = 1.0
            var wImag = 0.0
            val theta = 2.0 * direction * PI / (2.0 * dual)
            val s = sin(theta)
            val t = sin(theta / 2.0)
            val s2 = 2.0 * t * t
            var b = 0
            while (b < half) {
                val i = 2 * b
                val j = 2 * (b + dual)
                val wdReal = data[j]
                val wdImag = data[j + 1]
                data[j] = data[i] - wdReal
                data[j + 1] = data[i + 1] - wdImag
                data[i] += wdReal
                data[i + 1] += wdImag
                b += 2 * dual
            }
            for (a in 1 until dual) {
                val tmpReal = wReal - s * wImag - s2 * wReal
                val tmpImag = wImag + s * wReal - s2 * wImag
                wReal = tmpReal
                wImag = tmpImag
                var c = 0
                while (c < half) {
                    val i = 2 * (c + a)
                    val j = 2 * (c + a + dual)
                    val z1Real = data[j]
                    val z1Imag = data[j + 1]
                    val wdReal = wReal * z1Real - wImag * z1Imag
                    val wdImag = wReal * z1Imag + wImag * z1Real
                    data[j] = data[i] - wdReal
                    data[j + 1] = data[i + 1] - wdImag
                    data[i] += wdReal
                    data[i + 1] += wdImag
                    c += 2 * dual
                }
            }
            dual *= 2
        }
    }

    private fun fftBitReverse(n: Int, data: DoubleArray) {
        val half = n / 2
        var j = 0
        for (i in 0 until half - 1) {
            val ii = i shl 1
            val jj = j shl 1
            var k = half shr 1
            if (i < j) {
                val tmpReal = data[ii]
                val tmpImag = data[ii + 1]
                data[ii] = data[jj]
                data[ii + 1] = data[jj + 1]
                data[jj] = tmpReal
                data[jj + 1] = tmpImag
            }
            while (k <= j) {
                j -= k
                k = k shr 1
            }
            j += k
        }
    }

    private fun sorExecute(m: Int, n: Int, omega: Double, g: Array<DoubleArray>, iterations: Int) {
        val omegaOverFour = omega * 0.25
        val oneMinusOmega = 1.0 - omega
        repeat(iterations) {
            for (i in 1 until m - 1) {
                val gi = g[i]
                val gim1 = g[i - 1]
                val gip1 = g[i + 1]
                for (j in 1 until n - 1) {
                    gi[j] = omegaOverFour * (gim1[j] + gip1[j] + gi[j - 1] + gi[j + 1]) +
                        oneMinusOmega * gi[j]
                }
            }
        }
    }

    internal fun monteCarloIntegrate(samples: Int): Double {
        val random = SciMarkRandom(MonteCarloSeed)
        var underCurve = 0
        repeat(samples) {
            val x = random.nextDouble()
            val y = random.nextDouble()
            if (x * x + y * y <= 1.0) underCurve++
        }
        return underCurve.toDouble() / samples * 4.0
    }

    private const val MonteCarloSeed = 113

    private fun sparseMatMult(
        m: Int,
        y: DoubleArray,
        value: DoubleArray,
        row: IntArray,
        col: IntArray,
        x: DoubleArray,
        iterations: Int,
    ) {
        repeat(iterations) {
            for (r in 0 until m) {
                var sum = 0.0
                for (i in row[r] until row[r + 1]) sum += x[col[i]] * value[i]
                y[r] = sum
            }
        }
    }

    internal fun luFactor(m: Int, n: Int, a: Array<DoubleArray>, pivot: IntArray) {
        val minMN = minOf(m, n)
        for (j in 0 until minMN) {
            var jp = j
            var t = abs(a[j][j])
            for (i in j + 1 until m) {
                val ab = abs(a[i][j])
                if (ab > t) {
                    jp = i
                    t = ab
                }
            }
            pivot[j] = jp
            if (a[jp][j] == 0.0) return
            if (jp != j) {
                val tmp = a[j]
                a[j] = a[jp]
                a[jp] = tmp
            }
            if (j < m - 1) {
                val recp = 1.0 / a[j][j]
                for (k in j + 1 until m) a[k][j] *= recp
            }
            if (j < minMN - 1) {
                for (ii in j + 1 until m) {
                    val aii = a[ii]
                    val aj = a[j]
                    val aiiJ = aii[j]
                    for (jj in j + 1 until n) aii[jj] -= aiiJ * aj[jj]
                }
            }
        }
    }

    private fun randomVector(n: Int, random: SciMarkRandom) = DoubleArray(n) { random.nextDouble() }

    private fun randomMatrix(m: Int, n: Int, random: SciMarkRandom) =
        Array(m) { DoubleArray(n) { random.nextDouble() } }

    private const val PI = 3.1415926535897932
}

// SciMark's own lagged-Fibonacci generator; Int arithmetic wraps the way C's does
internal class SciMarkRandom(seed: Int) {
    private val m = IntArray(17)
    private var i = 4
    private var j = 16

    init {
        var s = if (seed < 0) -seed else seed
        var jseed = if (s < M1) s else M1
        if (jseed % 2 == 0) jseed--
        val k0 = 9069 % M2
        val k1 = 9069 / M2
        var j0 = jseed % M2
        var j1 = jseed / M2
        for (index in 0 until 17) {
            s = j0 * k0
            j1 = (s / M2 + j0 * k1 + j1 * k0) % (M2 / 2)
            j0 = s % M2
            m[index] = j0 + M2 * j1
        }
    }

    fun nextDouble(): Double {
        var k = m[i] - m[j]
        if (k < 0) k += M1
        m[j] = k
        i = if (i == 0) 16 else i - 1
        j = if (j == 0) 16 else j - 1
        return DM1 * k
    }

    private companion object {
        const val MDIG = 32
        const val M1 = (1 shl (MDIG - 2)) + ((1 shl (MDIG - 2)) - 1)
        const val M2 = 1 shl (MDIG / 2)
        const val DM1 = 1.0 / M1
    }
}
