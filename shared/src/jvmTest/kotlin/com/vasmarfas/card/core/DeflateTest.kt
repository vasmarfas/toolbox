package com.vasmarfas.card.core

import java.io.ByteArrayOutputStream
import java.util.zip.Adler32
import java.util.zip.Deflater
import kotlin.math.pow
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DeflateTest {
    private val sizes = intArrayOf(0, 1, 2, 258, 32 * 1024, 65535, 65536, 65537, 1 shl 20)
    private val strategies = intArrayOf(Deflater.DEFAULT_STRATEGY, Deflater.FILTERED, Deflater.HUFFMAN_ONLY)
    private val large = 2 shl 20

    private inline fun <T> timed(block: () -> T): Pair<T, Double> {
        val start = System.nanoTime()
        val result = block()
        return result to (System.nanoTime() - start) / 1e6
    }

    @Test
    fun inflatesJdkOutputForEveryLevelStrategyAndSize() {
        for (kind in SampleKind.entries) {
            for (size in sizes) {
                val data = Samples.get(kind, size)
                for (level in 0..9) {
                    for (strategy in strategies) {
                        val compressed = jdkDeflate(data, level, strategy)
                        assertContentEquals(data, Inflate.inflate(compressed), "$kind size=$size level=$level strategy=$strategy")
                    }
                }
            }
        }
    }

    @Test
    fun inflatesLargeJdkOutput() {
        for (kind in SampleKind.entries) {
            val data = Samples.get(kind, large)
            for (level in intArrayOf(0, 1, 6, 9)) {
                for (strategy in if (level == 6) strategies else intArrayOf(Deflater.DEFAULT_STRATEGY)) {
                    val compressed = jdkDeflate(data, level, strategy)
                    assertContentEquals(data, Inflate.inflate(compressed), "$kind level=$level strategy=$strategy")
                    if (level == 6) assertContentEquals(data, Inflate.inflate(compressed, sizeHint = data.size), "$kind strategy=$strategy, exact hint")
                }
            }
        }
    }

    @Test
    fun deflateOutputIsReadByJdkAndOwnInflater() {
        for (kind in SampleKind.entries) {
            for (size in sizes) {
                val data = Samples.get(kind, size)
                for (level in 0..9) {
                    val compressed = Deflate.deflate(data, level)
                    assertContentEquals(data, jdkInflate(compressed), "JDK, $kind size=$size level=$level")
                    assertContentEquals(data, Inflate.inflate(compressed), "own, $kind size=$size level=$level")
                }
            }
        }
    }

    @Test
    fun deflatesLargeInputAtEveryLevel() {
        for (kind in SampleKind.entries) {
            val data = Samples.get(kind, large)
            for (level in 0..9) {
                val compressed = Deflate.deflate(data, level)
                assertContentEquals(data, jdkInflate(compressed), "JDK, $kind level=$level")
                assertContentEquals(data, Inflate.inflate(compressed), "own, $kind level=$level")
            }
        }
    }

    @Test
    fun compressedSizeStaysCloseToJdk() {
        val report = StringBuilder("\nsize ratio own/JDK at the same level (1 MB samples)\n")
        for (kind in listOf(SampleKind.ENGLISH, SampleKind.RUSSIAN, SampleKind.SOURCE, SampleKind.RUNS)) {
            val data = Samples.get(kind, 1 shl 20)
            val jdk6 = jdkDeflate(data, 6).size
            report.append(kind.name.padEnd(8))
            for (level in 1..9) {
                val own = Deflate.deflate(data, level).size
                val jdk = jdkDeflate(data, level).size
                report.append("  L$level ").append("%.3f".format(own.toDouble() / jdk))
                assertTrue(own <= jdk * 1.12, "$kind level $level: $own bytes vs JDK $jdk")
                if (level >= 6) assertTrue(own <= jdk6 * 1.12, "$kind level $level: $own bytes vs JDK level 6 $jdk6")
            }
            report.append("  (JDK L6 = ").append("%.2f".format(data.size.toDouble() / jdk6)).append(":1)\n")
        }
        println(report)
    }

    @Test
    fun incompressibleDataFallsBackToStoredBlocks() {
        for (size in intArrayOf(1000, 65535, 65536, 1 shl 20)) {
            val data = Samples.get(SampleKind.RANDOM, size)
            for (level in 1..9) {
                val compressed = Deflate.deflate(data, level)
                assertTrue(compressed.size <= size + size / 1000 + 16, "size $size level $level: ${compressed.size} bytes")
                assertEquals(0, (compressed[0].toInt() shr 1) and 3, "first block should be stored")
            }
        }
    }

    @Test
    fun choosesBlockTypeBySize() {
        assertEquals(1, (Deflate.deflate("abcabcabc".encodeToByteArray())[0].toInt() shr 1) and 3)
        assertEquals(2, (Deflate.deflate(Samples.get(SampleKind.ENGLISH, 100_000))[0].toInt() shr 1) and 3)
        assertContentEquals(byteArrayOf(0x03, 0x00), Deflate.deflate(ByteArray(0)))
        assertContentEquals(byteArrayOf(0x01, 0x00, 0x00, 0xFF.toByte(), 0xFF.toByte()), Deflate.deflate(ByteArray(0), 0))
    }

    @Test
    fun stayFastEnoughForPhones() {
        val data = Samples.get(SampleKind.ENGLISH, 4 shl 20)
        Inflate.inflate(Deflate.deflate(data.copyOf(1 shl 20)))
        val (compressed, deflateMs) = timed { Deflate.deflate(data, 6) }
        val (restored, inflateMs) = timed { Inflate.inflate(compressed) }
        assertContentEquals(data, restored)
        assertTrue(deflateMs < 3000, "deflate took $deflateMs ms")
        assertTrue(inflateMs < 800, "inflate took $inflateMs ms")
    }

    @Test
    fun inflateHonoursRangeAndIgnoresTrailingBytes() {
        val data = Samples.get(SampleKind.ENGLISH, 50_000)
        val stream = jdkDeflate(data, 6)
        val padded = ByteArray(17) { it.toByte() } + stream + Random(5).nextBytes(300)
        assertContentEquals(data, Inflate.inflate(padded, 17, stream.size))
        assertContentEquals(data, Inflate.inflate(padded, 17))
        assertContentEquals(data, Inflate.inflate(stream, sizeHint = 10))
        assertContentEquals(data, Inflate.inflate(stream, sizeHint = 1 shl 24))
        assertFailsWith<IllegalArgumentException> { Inflate.inflate(stream, 10, stream.size) }
        assertFailsWith<IllegalArgumentException> { Deflate.deflate(data, 10) }
    }

    @Test
    fun overlappingBackReferences() {
        val bits = Bits().bits(1, 1).bits(1, 2).fixed('a'.code).fixed(258).code(0, 5).fixed('b'.code).fixed(265).bits(1, 1).code(1, 5).fixed(256)
        assertEquals("aaaaab" + "ab".repeat(6), Inflate.inflate(bits.bytes()).decodeToString())
        for (period in 1..300) {
            val data = ByteArray(100_000) { (it % period).toByte() }
            assertContentEquals(data, Inflate.inflate(Deflate.deflate(data)), "period $period")
            assertContentEquals(data, Inflate.inflate(jdkDeflate(data, 9)), "JDK, period $period")
        }
    }

    @Test
    fun handlesLengthLimitedHuffmanCodes() {
        val fibonacci = IntArray(26).also {
            it[0] = 1
            it[1] = 1
            for (i in 2 until it.size) it[i] = it[i - 1] + it[i - 2]
        }
        val data = ByteArray(fibonacci.sum())
        var p = 0
        for (symbol in fibonacci.indices) repeat(fibonacci[symbol]) { data[p++] = (symbol * 7).toByte() }
        data.shuffle(Random(12))
        for (level in intArrayOf(1, 4, 6, 9)) {
            val own = Deflate.deflate(data, level)
            assertContentEquals(data, jdkInflate(own), "level $level")
            assertContentEquals(data, Inflate.inflate(own), "level $level")
        }
        for (strategy in strategies) assertContentEquals(data, Inflate.inflate(jdkDeflate(data, 9, strategy)), "strategy $strategy")
    }

    @Test
    fun buildsCompleteLengthLimitedCodes() {
        fun check(freq: IntArray, maxBits: Int) {
            val lengths = IntArray(freq.size)
            buildLengths(freq, freq.size, maxBits, lengths)
            var kraft = 0L
            for (s in freq.indices) {
                if (freq[s] > 0) assertTrue(lengths[s] in 1..maxBits, "symbol $s of ${freq.toList()}")
                if (lengths[s] > 0) kraft += 1L shl (maxBits - lengths[s])
            }
            assertEquals(1L shl maxBits, kraft, "code must be complete for ${freq.toList()}")
            for (a in freq.indices) {
                for (b in freq.indices) {
                    if (freq[a] > freq[b] && freq[b] > 0) assertTrue(lengths[a] <= lengths[b], "more frequent symbol got a longer code")
                }
            }
        }
        val fibonacci = IntArray(30).also {
            it[0] = 1
            it[1] = 1
            for (i in 2 until it.size) it[i] = it[i - 1] + it[i - 2]
        }
        check(fibonacci, 15)
        check(fibonacci.copyOf(19), 7)
        check(IntArray(19) { 1 shl it.coerceAtMost(20) }, 7)
        check(IntArray(286), 15)
        check(IntArray(286).also { it[256] = 1 }, 15)
        check(IntArray(30).also { it[0] = 5 }, 15)
        val random = Random(14)
        repeat(3000) {
            val maxBits = if (random.nextBoolean()) 15 else 7
            val n = if (maxBits == 7) 19 else random.nextInt(2, 287)
            val freq = IntArray(n) {
                when (random.nextInt(4)) {
                    0 -> 0
                    1 -> random.nextInt(1, 4)
                    2 -> 1 shl random.nextInt(0, 17)
                    else -> random.nextInt(1, 20_000)
                }
            }
            check(freq, maxBits)
        }
        val lengths = IntArray(30)
        buildLengths(fibonacci, 30, 15, lengths)
        assertEquals(15, lengths.max())
    }

    @Test
    fun roundTripsRandomSymbolDistributions() {
        val random = Random(13)
        repeat(400) { iteration ->
            val alphabet = random.nextInt(1, 257)
            val skew = random.nextDouble(0.3, 8.0)
            val data = ByteArray(random.nextInt(0, 60_000)) { minOf(alphabet - 1, (alphabet * random.nextDouble().pow(skew)).toInt()).toByte() }
            val level = random.nextInt(0, 10)
            val own = Deflate.deflate(data, level)
            assertContentEquals(data, jdkInflate(own), "iteration $iteration")
            assertContentEquals(data, Inflate.inflate(own), "iteration $iteration")
            assertContentEquals(data, Inflate.inflate(jdkDeflate(data, level, strategies[iteration % 3])), "iteration $iteration")
        }
    }

    @Test
    fun rejectsMalformedStreams() {
        val fixedHeader = { Bits().bits(1, 1).bits(1, 2) }
        val cases = mapOf(
            "empty input" to ByteArray(0),
            "reserved block type" to byteArrayOf(0x07),
            "stored length complement" to byteArrayOf(0x01, 0x05, 0x00, 0x00, 0x00),
            "stored block past the end" to byteArrayOf(0x01, 0x05, 0x00, 0xFA.toByte(), 0xFF.toByte(), 0x61),
            "distance before output start" to fixedHeader().fixed(257).code(0, 5).fixed(256).bytes(),
            "distance too far back" to fixedHeader().fixed('a'.code).fixed(257).code(1, 5).fixed(256).bytes(),
            "length symbol 286" to fixedHeader().fixed('a'.code).fixed(286).code(0, 5).fixed(256).bytes(),
            "distance symbol 30" to fixedHeader().fixed('a'.code).fixed(257).code(30, 5).fixed(256).bytes(),
            "no end of block" to fixedHeader().fixed('a'.code).fixed('b'.code).bytes(),
            "over-subscribed code lengths" to dynamicHeader(19).apply { repeat(19) { bits(1, 3) } }.bytes(),
            "incomplete code length code" to dynamicHeader(4).bits(1, 3).bits(0, 3).bits(0, 3).bits(0, 3).bytes(),
            "repeat without previous length" to dynamicHeader(4).bits(1, 3).bits(0, 3).bits(0, 3).bits(1, 3).code(1, 1).bits(0, 2).bytes(),
            "missing end-of-block code" to missingEndOfBlock(),
            "incomplete literal code" to incompleteLiteralCode(),
            "too many literal codes" to Bits().bits(1, 1).bits(2, 2).bits(30, 5).bits(0, 5).bits(0, 4).bytes(),
        )
        for ((name, stream) in cases) {
            assertFailsWith<DeflateException>(name) { Inflate.inflate(stream) }
        }
    }

    @Test
    fun truncatedStreamsFailCleanly() {
        val data = Samples.get(SampleKind.ENGLISH, 20_000)
        for (level in intArrayOf(0, 1, 6, 9)) {
            val stream = Zlib.compress(data, level)
            for (cut in 0 until stream.size - 4) {
                assertFailsWith<DeflateException>("level $level cut $cut") { Zlib.decompress(stream.copyOf(cut)) }
            }
        }
        val jdk = jdkDeflate(Samples.get(SampleKind.SOURCE, 20_000), 6, Deflater.HUFFMAN_ONLY)
        for (cut in jdk.indices) assertFailsWith<DeflateException>("cut $cut") { Inflate.inflate(jdk, 0, cut) }
    }

    @Test
    fun corruptStreamsOnlyThrowDeflateException() {
        val random = Random(42)
        val data = Samples.get(SampleKind.ENGLISH, 5000)
        val streams = listOf(
            Deflate.deflate(data, 0),
            Deflate.deflate(data, 1),
            Deflate.deflate(data, 6),
            jdkDeflate(data, 9),
            jdkDeflate(data, 6, Deflater.HUFFMAN_ONLY),
            jdkDeflate(Samples.get(SampleKind.RUNS, 5000), 6),
        )
        var failures = 0
        repeat(30_000) {
            val corrupt = streams[random.nextInt(streams.size)].copyOf()
            repeat(1 + random.nextInt(4)) {
                val i = random.nextInt(corrupt.size)
                corrupt[i] = (corrupt[i].toInt() xor (1 shl random.nextInt(8))).toByte()
            }
            try {
                Inflate.inflate(corrupt)
            } catch (e: DeflateException) {
                failures++
            }
        }
        repeat(5000) {
            val garbage = random.nextBytes(random.nextInt(1, 3000))
            try {
                Inflate.inflate(garbage)
            } catch (e: DeflateException) {
                failures++
            }
            try {
                Zlib.decompress(byteArrayOf(0x78, 0x9C.toByte()) + garbage)
            } catch (e: DeflateException) {
                failures++
            }
        }
        assertTrue(failures > 20_000, "corruption should mostly be detected, detected $failures")
    }

    @Test
    fun zlibRoundTripsWithJdk() {
        for (kind in SampleKind.entries) {
            for (size in intArrayOf(0, 1, 1000, 200_000)) {
                val data = Samples.get(kind, size)
                val adler = Adler32().apply { update(data) }.value.toInt()
                for (level in 0..9) {
                    val own = Zlib.compress(data, level)
                    assertEquals(0x78, own[0].toInt() and 0xFF)
                    assertEquals(0, ((own[0].toInt() and 0xFF) * 256 + (own[1].toInt() and 0xFF)) % 31)
                    assertEquals(adler, readIntBigEndian(own, own.size - 4), "$kind size=$size level=$level")
                    assertContentEquals(data, jdkInflate(own, nowrap = false), "JDK, $kind size=$size level=$level")
                    assertContentEquals(data, Zlib.decompress(own), "own, $kind size=$size level=$level")
                    assertContentEquals(data, Zlib.decompress(jdkDeflate(data, level, nowrap = false)), "from JDK, $kind size=$size level=$level")
                }
            }
        }
        assertEquals(0x9C, Zlib.compress(ByteArray(10))[1].toInt() and 0xFF)
        assertEquals(0x01, Zlib.compress(ByteArray(10), 1)[1].toInt() and 0xFF)
        assertEquals(0xDA, Zlib.compress(ByteArray(10), 9)[1].toInt() and 0xFF)
    }

    @Test
    fun zlibToleratesDamagedTrailerAndGarbage() {
        val data = Samples.get(SampleKind.RUSSIAN, 50_000)
        val stream = jdkDeflate(data, 6, nowrap = false)
        val wrongAdler = stream.copyOf().also { it[it.size - 1] = (it[it.size - 1] + 1).toByte() }
        assertContentEquals(data, Zlib.decompress(wrongAdler))
        assertContentEquals(data, Zlib.decompress(stream.copyOf(stream.size - 4)))
        assertContentEquals(data, Zlib.decompress(stream.copyOf(stream.size - 1)))
        assertContentEquals(data, Zlib.decompress(stream + "\r\nendstream\r\nendobj\r\n".encodeToByteArray()))
        assertContentEquals(data, Zlib.decompress(stream + Random(9).nextBytes(4096)))
        assertFailsWith<DeflateException> { Zlib.decompress(byteArrayOf(0x78)) }
        assertFailsWith<DeflateException> { Zlib.decompress(byteArrayOf(0x78, 0x9D.toByte(), 0x03, 0x00)) }
        assertFailsWith<DeflateException> { Zlib.decompress(byteArrayOf(0x79, 0x9C.toByte(), 0x03, 0x00)) }
        assertFailsWith<DeflateException> { Zlib.decompress(byteArrayOf(0x78, 0xBB.toByte(), 0, 0, 0, 1, 0x03, 0x00)) }
    }

    private fun readIntBigEndian(b: ByteArray, at: Int): Int =
        ((b[at].toInt() and 0xFF) shl 24) or ((b[at + 1].toInt() and 0xFF) shl 16) or ((b[at + 2].toInt() and 0xFF) shl 8) or (b[at + 3].toInt() and 0xFF)

    private fun dynamicHeader(codeLengthCount: Int): Bits = Bits().bits(1, 1).bits(2, 2).bits(0, 5).bits(0, 5).bits(codeLengthCount - 4, 4)

    private fun missingEndOfBlock(): ByteArray {
        val bits = dynamicHeader(18)
        val order = intArrayOf(16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1)
        for (symbol in order) bits.bits(if (symbol == 0 || symbol == 1) 1 else 0, 3)
        bits.code(1, 1).code(1, 1)
        repeat(256) { bits.code(0, 1) }
        return bits.bytes()
    }

    private fun incompleteLiteralCode(): ByteArray {
        val bits = dynamicHeader(16)
        val order = intArrayOf(16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2)
        for (symbol in order) bits.bits(if (symbol == 0 || symbol == 2) 1 else 0, 3)
        bits.code(1, 1).code(1, 1)
        repeat(254) { bits.code(0, 1) }
        bits.code(1, 1)
        bits.code(1, 1)
        return bits.bytes()
    }

    private class Bits {
        private val out = ByteArrayOutputStream()
        private var acc = 0
        private var count = 0

        fun bits(value: Int, n: Int): Bits {
            for (i in 0 until n) bit((value ushr i) and 1)
            return this
        }

        fun code(value: Int, n: Int): Bits {
            for (i in n - 1 downTo 0) bit((value ushr i) and 1)
            return this
        }

        fun fixed(symbol: Int): Bits = when {
            symbol < 144 -> code(0x30 + symbol, 8)
            symbol < 256 -> code(0x190 + symbol - 144, 9)
            symbol < 280 -> code(symbol - 256, 7)
            else -> code(0xC0 + symbol - 280, 8)
        }

        fun bytes(): ByteArray {
            if (count > 0) out.write(acc)
            acc = 0
            count = 0
            return out.toByteArray()
        }

        private fun bit(b: Int) {
            acc = acc or (b shl count)
            if (++count == 8) {
                out.write(acc)
                acc = 0
                count = 0
            }
        }
    }
}
