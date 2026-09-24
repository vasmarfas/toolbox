package com.vasmarfas.card.tools.media

import java.awt.image.BufferedImage
import java.util.zip.Inflater
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PngTest {
    private class Header(val depth: Int, val colorType: Int)

    private fun header(png: ByteArray): Header {
        val chunks = pngChunks(png)
        assertEquals("IHDR", chunks.first().type)
        assertEquals("IEND", chunks.last().type)
        assertTrue(chunks.all { it.crcValid }, "every chunk CRC must match")
        val ihdr = chunks.first().data
        return Header(ihdr[8].toInt(), ihdr[9].toInt())
    }

    private fun roundTrip(src: IntArray, w: Int, h: Int, colorType: Int) {
        val png = Png.encode(src, w, h)
        assertEquals(colorType, header(png).colorType)
        assertContentEquals(src, pixels(decode(png)))
    }

    @Test
    fun opaqueColourIsRgb() {
        roundTrip(photo(257, 131), 257, 131, 2)
    }

    @Test
    fun alphaIsRgba() {
        val src = photo(120, 77).mapIndexed { i, p -> (p and 0xFFFFFF) or ((i * 7 % 256) shl 24) }.toIntArray()
        roundTrip(src, 120, 77, 6)
    }

    // ImageIO's getRGB gamma-converts grey images, so samples are read straight from the raster
    @Test
    fun greyIsGrey() {
        val w = 90
        val h = 61
        val grey = photo(w, h).map { p ->
            val g = (((p shr 16) and 0xFF) + ((p shr 8) and 0xFF) + (p and 0xFF)) / 3
            argb(255, g, g, g)
        }.toIntArray()
        val withAlpha = grey.mapIndexed { i, p -> (p and 0xFFFFFF) or ((255 - i % 200) shl 24) }.toIntArray()
        for ((src, colorType) in listOf(grey to 0, withAlpha to 4)) {
            val png = Png.encode(src, w, h)
            assertEquals(colorType, header(png).colorType)
            val raster = decode(png).raster
            for (i in src.indices) {
                assertEquals(src[i] and 0xFF, raster.getSample(i % w, i / w, 0), "grey at $i")
                if (colorType == 4) assertEquals(src[i] ushr 24, raster.getSample(i % w, i / w, 1), "alpha at $i")
            }
        }
    }

    @Test
    fun onePixel() {
        roundTrip(intArrayOf(argb(255, 1, 2, 3)), 1, 1, 2)
    }

    @Test
    fun paletteModeOnPhoto() {
        val w = 640
        val h = 480
        val src = photo(w, h, seed = 21)
        val truecolor = Png.encode(src, w, h)
        val imageIo = encodeWith(image(src, w, h, BufferedImage.TYPE_INT_RGB), "png")
        for (dither in listOf(false, true)) {
            val indexed = Png.encode(src, w, h, paletteColors = 256, dither = dither)
            val head = header(indexed)
            assertEquals(3, head.colorType)
            assertEquals(8, head.depth)
            val out = pixels(decode(indexed))
            val err = channelError(src, out)
            val block = blockError(src, out, w, h)
            assertTrue(indexed.size * 2 < truecolor.size, "palette PNG should be under half the truecolor size")
            assertTrue(if (dither) block < 1.0 else err.mean < 2.5)
        }
        assertTrue(truecolor.size <= imageIo.size * 1.05, "truecolor ${truecolor.size} vs ImageIO ${imageIo.size}")
    }

    @Test
    fun bitDepthFollowsPaletteSize() {
        for ((colors, depth) in listOf(2 to 1, 4 to 2, 9 to 4, 16 to 4, 17 to 8)) {
            val w = 37
            val h = 11
            val src = IntArray(w * h) { argb(255, (it % colors) * 13, 255 - (it % colors) * 7, 50) }
            val png = Png.encode(src, w, h, paletteColors = 256)
            assertEquals(depth, header(png).depth, "$colors colours")
            assertContentEquals(src, pixels(decode(png)), "$colors colours")
        }
    }

    @Test
    fun transparencyGoesToTrns() {
        val w = 64
        val h = 48
        val src = IntArray(w * h) {
            val x = it % w
            when {
                x < 8 -> 0
                x < 16 -> argb(128, 255, 0, 0)
                x < 24 -> argb(40, 0, 255, 0)
                else -> argb(255, x / 8 * 40, it / w / 8 * 40, 60)
            }
        }
        val png = Png.encode(src, w, h, paletteColors = 256)
        val chunks = pngChunks(png)
        val plte = chunks.first { it.type == "PLTE" }.data
        val trns = chunks.first { it.type == "tRNS" }.data
        assertEquals(3, trns.size, "only the non-opaque entries at the front need tRNS")
        assertTrue(plte.size / 3 <= 256)
        val out = pixels(decode(png))
        for (i in src.indices) {
            if (src[i] ushr 24 == 0) assertEquals(0, out[i] ushr 24) else assertEquals(src[i], out[i], "pixel $i")
        }
    }

    @Test
    fun softAlphaSurvivesQuantization() {
        val w = 200
        val h = 200
        val src = IntArray(w * h) {
            val dx = it % w - 100.0
            val dy = it / w - 100.0
            val d = hypot(dx, dy)
            argb(((80 - d) * 255 / 16).toInt().coerceIn(0, 255), 30 + (it % w), 220 - (it / w) / 2, 140)
        }
        val out = pixels(decode(Png.encode(src, w, h, paletteColors = 128)))
        var worst = 0
        for (i in src.indices) worst = maxOf(worst, abs((src[i] ushr 24) - (out[i] ushr 24)))
        assertTrue(worst <= 24, "alpha error $worst")
    }

    @Test
    fun filtersAreAdaptive() {
        val w = 300
        val h = 200
        val png = Png.encode(photo(w, h), w, h)
        val idat = pngChunks(png).filter { it.type == "IDAT" }.fold(ByteArray(0)) { a, c -> a + c.data }
        val raw = ByteArray(h * (w * 3 + 1))
        Inflater().apply { setInput(idat) }.inflate(raw)
        val types = (0 until h).map { raw[it * (w * 3 + 1)].toInt() }.toSet()
        assertTrue(types.size > 1, "rows should not all use one filter: $types")
        assertTrue(types.all { it in 0..4 })
    }

    @Test
    fun twelveMegapixelPalettePng() {
        val w = 4000
        val h = 3000
        val src = photo(w, h, seed = 8)
        val png = Png.encode(src, w, h, paletteColors = 256)
        assertEquals(3, header(png).colorType)
    }
}
