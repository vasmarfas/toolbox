package com.vasmarfas.card.tools.media

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

class QuantizerTest {
    private fun apply(palette: Palette, indices: ByteArray) = IntArray(indices.size) { palette.colors[indices[it].toInt() and 0xFF] }

    @Test
    fun fewColorsGiveAnExactPalette() {
        val w = 50
        val h = 40
        val src = poster(w, h, 37).also { it[5] = 0x80FF8040.toInt() }
        val palette = Quantizer.palette(src, 256)
        assertEquals(src.toSet(), palette.colors.toSet())
        assertEquals(-1, palette.transparentIndex)
        for (dither in listOf(false, true)) assertContentEquals(src, apply(palette, Quantizer.remap(src, w, h, palette, dither)))
    }

    @Test
    fun transparentPixelsGetTheirOwnEntry() {
        val src = IntArray(64) { if (it % 3 == 0) 0x00FFFFFF else argb(255, it * 4, 0, 0) }
        val palette = Quantizer.palette(src, 8)
        assertEquals(8, palette.size)
        assertEquals(0, palette.transparentIndex)
        assertEquals(0, palette.colors[0])
        val indices = Quantizer.remap(src, 8, 8, palette, dither = true)
        for (i in src.indices) assertEquals(src[i] ushr 24 == 0, indices[i].toInt() == 0, "pixel $i")
    }

    @Test
    fun translucentEntriesComeFirst() {
        val src = IntArray(300) { argb(if (it < 100) 255 else it % 200, it, 255 - it % 256, 30) }
        val palette = Quantizer.palette(src, 64)
        val alphas = palette.colors.map { it ushr 24 }
        assertEquals(alphas.sorted(), alphas)
    }

    @Test
    fun photoQualityByPaletteSize() {
        val w = 320
        val h = 240
        val src = photo(w, h, seed = 11)
        val limits = mapOf(256 to 2.5, 64 to 5.0, 16 to 11.0)
        for ((colors, limit) in limits) {
            val palette = Quantizer.palette(src, colors)
            assertTrue(palette.size <= colors)
            val err = channelError(src, apply(palette, Quantizer.remap(src, w, h, palette, dither = false)))
            val dithered = apply(palette, Quantizer.remap(src, w, h, palette, dither = true))
            val block = blockError(src, dithered, w, h)
            assertTrue(err.mean < limit, "mean error ${err.mean} for $colors colours")
            assertTrue(block < err.mean, "dithering should lower the block error for $colors colours")
        }
    }

    @Test
    fun ditheredOutliersStayBounded() {
        val w = 320
        val h = 240
        val src = photo(w, h)
        val palette = Quantizer.palette(src, 256)
        val err = channelError(src, apply(palette, Quantizer.remap(src, w, h, palette, dither = true)))
        assertTrue(err.max <= 40, "max error ${err.max}")
    }

    @Test
    fun twoColourDitherKeepsTheGreyLevels() {
        val w = 256
        val h = 64
        val ramp = IntArray(w * h) { argb(255, it % w, it % w, it % w) }
        val palette = Palette(intArrayOf(argb(255, 0, 0, 0), argb(255, 255, 255, 255)), -1)
        val out = apply(palette, Quantizer.remap(ramp, w, h, palette, dither = true))
        for (bx in 0 until w / 16) {
            var sum = 0
            for (y in 16 until 48) for (x in bx * 16 until bx * 16 + 16) sum += out[y * w + x] and 0xFF
            val mean = sum / (16.0 * 32)
            val expected = bx * 16 + 7.5
            assertTrue(abs(mean - expected) < 10, "block $bx: $mean instead of $expected")
        }
    }

    @Test
    fun nearlyInvisibleColoursDoNotTakeSlots() {
        val rnd = Random(4)
        val opaque = IntArray(200) { argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)) }
        val src = IntArray(40_000) { if (it % 2 == 0) opaque[it / 2 % 200] else argb(2, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)) }
        val palette = Quantizer.palette(src, 256)
        val out = apply(palette, Quantizer.remap(src, 200, 200, palette, dither = false))
        var worst = 0
        for (i in src.indices step 2) worst = maxOf(worst, channelError(intArrayOf(src[i]), intArrayOf(out[i])).max)
        assertTrue(worst <= 8, "opaque colours drift by $worst")
        for (i in 1 until src.size step 2) assertTrue(out[i] ushr 24 < 16, "faint pixel $i became ${(out[i] ushr 24)}")
    }

    @Test
    fun softEdgesKeepTheirAlpha() {
        val w = 128
        val h = 128
        val src = IntArray(w * h) {
            val dx = it % w - 64.0
            val dy = it / w - 64.0
            val d = sqrt(dx * dx + dy * dy)
            val a = ((50 - d) * 255 / 10).toInt().coerceIn(0, 255)
            argb(a, 200 + (it % w) / 3, 40 + (it / w), 90)
        }
        val palette = Quantizer.palette(src, 64)
        val out = apply(palette, Quantizer.remap(src, w, h, palette, dither = false))
        var worstAlpha = 0
        for (i in src.indices) worstAlpha = maxOf(worstAlpha, abs((src[i] ushr 24) - (out[i] ushr 24)))
        assertTrue(worstAlpha <= 24, "alpha error $worstAlpha")
        for (i in src.indices) if (src[i] ushr 24 == 0) assertEquals(0, out[i] ushr 24)
    }

    @Test
    fun twelveMegapixels() {
        val w = 4000
        val h = 3000
        val src = photo(w, h, seed = 2)
        lateinit var palette: Palette
        val paletteTime = measureTime { palette = Quantizer.palette(src, 256) }
        val remapTime = measureTime { Quantizer.remap(src, w, h, palette, dither = true) }
        assertTrue(paletteTime + remapTime < 30.seconds)
    }
}
