package com.vasmarfas.card.tools.device

import com.vasmarfas.card.tools.developer.Sha256
import kotlin.time.TimeSource

inline fun timeSeconds(block: () -> Unit): Double {
    val mark = TimeSource.Monotonic.markNow()
    block()
    return mark.elapsedNow().inWholeMicroseconds / 1_000_000.0
}

class BenchmarkScore(val name: String, val value: Double, val unit: BenchmarkUnit)

enum class BenchmarkUnit { MWIPS, MFLOPS, MB_PER_SECOND, GB_PER_SECOND, MPIXELS_PER_SECOND }

class Benchmark(
    val id: String,
    val run: (minSeconds: Double) -> List<BenchmarkScore>,
)

val benchmarks: List<Benchmark> = listOf(
    Benchmark("whetstone") { minSeconds ->
        val loop = 1000
        var majorLoops = 0
        val whetstone = Whetstone()
        var sink = 0.0
        val seconds = timeSeconds {
            val mark = TimeSource.Monotonic.markNow()
            do {
                sink += whetstone.majorLoop(loop)
                majorLoops++
            } while (mark.elapsedNow().inWholeMilliseconds < minSeconds * 1000)
        }
        keep(sink)
        listOf(BenchmarkScore("MWIPS", Whetstone.mwips(loop, majorLoops, seconds), BenchmarkUnit.MWIPS))
    },
    Benchmark("linpack") { _ ->
        val result = Linpack.run(LinpackSize) { block -> timeSeconds(block) }
        listOf(BenchmarkScore("MFLOPS", result.mflops, BenchmarkUnit.MFLOPS))
    },
    Benchmark("scimark") { minSeconds ->
        val perKernel = minSeconds / 5.0
        val fft = SciMark.fft(perKernel)
        val sor = SciMark.sor(perKernel)
        val monteCarlo = SciMark.monteCarlo(perKernel)
        val sparse = SciMark.sparseMatMult(perKernel)
        val lu = SciMark.lu(perKernel)
        listOf(
            BenchmarkScore("composite", (fft + sor + monteCarlo + sparse + lu) / 5.0, BenchmarkUnit.MFLOPS),
            BenchmarkScore("FFT", fft, BenchmarkUnit.MFLOPS),
            BenchmarkScore("SOR", sor, BenchmarkUnit.MFLOPS),
            BenchmarkScore("Monte Carlo", monteCarlo, BenchmarkUnit.MFLOPS),
            BenchmarkScore("sparse matmult", sparse, BenchmarkUnit.MFLOPS),
            BenchmarkScore("LU", lu, BenchmarkUnit.MFLOPS),
        )
    },
    Benchmark("sha256") { minSeconds ->
        val block = ByteArray(ShaBlockBytes) { (it and 0xFF).toByte() }
        var rounds = 0
        var sink = 0
        val seconds = timeSeconds {
            val mark = TimeSource.Monotonic.markNow()
            do {
                sink += Sha256.digest(block)[0].toInt()
                rounds++
            } while (mark.elapsedNow().inWholeMilliseconds < minSeconds * 1000)
        }
        keep(sink.toDouble())
        val megabytes = rounds.toDouble() * ShaBlockBytes / (1024 * 1024)
        listOf(BenchmarkScore("SHA-256", if (seconds <= 0.0) 0.0 else megabytes / seconds, BenchmarkUnit.MB_PER_SECOND))
    },
    Benchmark("memory") { minSeconds ->
        val source = DoubleArray(MemoryWords) { it.toDouble() }
        val target = DoubleArray(MemoryWords)
        var passes = 0
        var sink = 0.0
        val seconds = timeSeconds {
            val mark = TimeSource.Monotonic.markNow()
            do {
                source.copyInto(target)
                sink += target[passes % MemoryWords]
                passes++
            } while (mark.elapsedNow().inWholeMilliseconds < minSeconds * 1000)
        }
        keep(sink)
        val gigabytes = passes.toDouble() * MemoryWords * 8 * 2 / (1024.0 * 1024 * 1024)
        listOf(BenchmarkScore("copy", if (seconds <= 0.0) 0.0 else gigabytes / seconds, BenchmarkUnit.GB_PER_SECOND))
    },
    Benchmark("mandelbrot") { minSeconds ->
        var frames = 0
        var sink = 0L
        val seconds = timeSeconds {
            val mark = TimeSource.Monotonic.markNow()
            do {
                sink += mandelbrot(MandelbrotSide, MandelbrotIterations)
                frames++
            } while (mark.elapsedNow().inWholeMilliseconds < minSeconds * 1000)
        }
        keep(sink.toDouble())
        val megapixels = frames.toDouble() * MandelbrotSide * MandelbrotSide / 1_000_000.0
        listOf(
            BenchmarkScore(
                "Mandelbrot",
                if (seconds <= 0.0) 0.0 else megapixels / seconds,
                BenchmarkUnit.MPIXELS_PER_SECOND,
            ),
        )
    },
)

private const val LinpackSize = 500
private const val ShaBlockBytes = 1 shl 20
private const val MemoryWords = 1 shl 21
private const val MandelbrotSide = 256
private const val MandelbrotIterations = 500

fun mandelbrot(side: Int, maxIterations: Int): Long {
    var total = 0L
    for (py in 0 until side) {
        val y0 = py * 2.0 / side - 1.0
        for (px in 0 until side) {
            val x0 = px * 3.0 / side - 2.0
            var x = 0.0
            var y = 0.0
            var i = 0
            while (i < maxIterations && x * x + y * y <= 4.0) {
                val xt = x * x - y * y + x0
                y = 2.0 * x * y + y0
                x = xt
                i++
            }
            total += i
        }
    }
    return total
}

// no blackhole intrinsic on Wasm or Native, so results go somewhere the optimiser cannot discard
private var sinkHolder = 0.0

private fun keep(value: Double) {
    sinkHolder += value
    if (sinkHolder == Double.MAX_VALUE) sinkHolder = 0.0
}
