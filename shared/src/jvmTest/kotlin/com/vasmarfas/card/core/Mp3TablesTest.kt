package com.vasmarfas.card.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Mp3TablesTest {
    @Test
    fun huffmanTablesArePrefixFreeAndComplete() {
        for (table in 1 until 32) {
            val size = Mp3Tables.tableSize[table]
            if (size == 0) continue
            val codes = Mp3Tables.codes[table]
            val lengths = Mp3Tables.lengths[table]
            assertEquals(size * size, codes.size)
            checkPrefixCode(codes, lengths, "table $table")
        }
        checkPrefixCode(Mp3Tables.count1Codes, Mp3Tables.count1Lengths, "count1 A")
        assertEquals(1, Mp3Tables.lengths[1][0])
        assertEquals(0b000, Mp3Tables.codes[1][3])
        assertEquals(0b01, Mp3Tables.codes[1][2])
    }

    private fun checkPrefixCode(codes: IntArray, lengths: IntArray, name: String) {
        var kraft = 0.0
        for (i in codes.indices) {
            assertTrue(lengths[i] in 1..19 && codes[i] < (1 shl lengths[i]), "$name entry $i")
            kraft += 1.0 / (1L shl lengths[i])
            for (j in codes.indices) {
                if (i == j || lengths[j] < lengths[i]) continue
                assertTrue(codes[j] ushr (lengths[j] - lengths[i]) != codes[i], "$name: code $i is a prefix of code $j")
            }
        }
        assertEquals(1.0, kraft, 1e-12, name)
    }

    @Test
    fun analysisWindowIsTheSymmetricIsoWindow() {
        val c = Mp3Tables.analysisWindow
        assertEquals(512, c.size)
        assertEquals(0, c[0])
        assertEquals(-1, c[1])
        assertEquals(75038, c[256])
        for (n in 1 until 512) assertEquals(if (n % 64 == 0) c[n] else -c[n], c[512 - n], "C[$n]")
    }

    @Test
    fun scalefactorBandsCoverTheGranule() {
        for (edges in Mp3Tables.sfbLongMpeg1 + Mp3Tables.sfbLongMpeg2) {
            assertEquals(23, edges.size)
            assertEquals(0, edges.first())
            assertEquals(576, edges.last())
            assertTrue((1 until edges.size).all { edges[it] > edges[it - 1] && (edges[it] - edges[it - 1]) % 2 == 0 })
        }
    }

    private fun analyze(x: DoubleArray, types: IntArray, shortEdges: IntArray): List<DoubleArray> {
        val bank = Mp3Filterbank(DoubleArray(32) { 1.0 }, shortEdges)
        val previous = DoubleArray(576)
        val current = DoubleArray(576)
        return types.indices.map { g ->
            bank.polyphase(x, g * 576, current)
            val lines = DoubleArray(576)
            bank.transform(previous, current, types[g], lines)
            current.copyInto(previous)
            lines
        }
    }

    @Test
    fun filterbankReconstructsItsInput() {
        val shortEdges = Mp3Tables.sfbShortMpeg1[0]
        val types = intArrayOf(0, 0, 0, 1, 2, 3, 0, 1, 2, 2, 2, 3, 1, 3, 0, 0, 1, 2, 3, 0, 1, 2, 2, 3, 0, 0, 0, 0, 0, 0)
        val n = 576 * types.size
        val random = Random(7)
        val x = DoubleArray(n) { if (it < n - 3000) random.nextDouble(-0.5, 0.5) else 0.0 }
        val lines = analyze(x, types, shortEdges)
        val synthesis = ReferenceSynthesis(shortEdges)
        val y = DoubleArray(n)
        for (g in types.indices) synthesis.granule(lines[g], types[g], y, g * 576)
        assertEquals(Mp3Encoder.DELAY, Signals.delay(x, y))
        var signal = 0.0
        var noise = 0.0
        for (i in 2000 until n - 3000 - Mp3Encoder.DELAY) {
            signal += x[i] * x[i]
            noise += (x[i] - y[i + Mp3Encoder.DELAY]) * (x[i] - y[i + Mp3Encoder.DELAY])
        }
        val snr = 10 * log10(signal / noise)
        assertTrue(snr > 80, "reconstruction at $snr dB")
    }

    @Test
    fun fullScaleSineCarriesUnitEnergy() {
        val x = DoubleArray(576 * 40) { sin(2 * PI * 1000 * it / 44100) }
        for (type in listOf(NORMAL_BLOCK, SHORT_BLOCK)) {
            val lines = analyze(x, IntArray(40) { type }, Mp3Tables.sfbShortMpeg1[0])
            val mean = lines.drop(4).sumOf { granule -> granule.sumOf { it * it } } / 36 / if (type == SHORT_BLOCK) 3 else 1
            assertTrue(abs(mean - 1.0) < 0.02, "block type $type: energy $mean per ${if (type == SHORT_BLOCK) "window" else "granule"}")
        }
    }
}
