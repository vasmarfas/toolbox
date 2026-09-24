package com.vasmarfas.card.tools.media

import java.awt.Transparency
import java.awt.color.ColorSpace
import java.awt.image.BufferedImage
import java.awt.image.ComponentColorModel
import java.awt.image.DataBuffer
import java.awt.image.IndexColorModel
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier
import javax.imageio.ImageWriteParam
import javax.imageio.plugins.tiff.BaselineTIFFTagSet
import javax.imageio.plugins.tiff.TIFFDirectory
import javax.imageio.plugins.tiff.TIFFField
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.fail

class TiffTest {
    private class Sample(val name: String, val image: BufferedImage, val expected: IntArray)

    private fun grey(v: Int) = argb(255, v, v, v)

    private fun samples(): List<Sample> {
        val w = 61
        val h = 47
        val rnd = Random(12)
        val list = ArrayList<Sample>()
        val rgb = photo(w, h)
        list += Sample("rgb", image(rgb, w, h, BufferedImage.TYPE_INT_RGB), rgb)
        val argbPixels = photo(w, h, seed = 3).mapIndexed { i, p -> (p and 0xFFFFFF) or ((i * 5 % 256) shl 24) }.toIntArray()
        list += Sample("argb", image(argbPixels, w, h, BufferedImage.TYPE_INT_ARGB), argbPixels)
        val greyValues = IntArray(w * h) { rnd.nextInt(256) }
        val greyImage = BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY).apply { raster.setPixels(0, 0, w, h, greyValues) }
        list += Sample("grey", greyImage, IntArray(w * h) { grey(greyValues[it]) })
        val bits = IntArray(w * h) { if ((it % w + it / w) % 3 == 0) 1 else 0 }
        val bilevel = BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY).apply { raster.setPixels(0, 0, w, h, bits) }
        list += Sample("1-bit", bilevel, IntArray(w * h) { grey(bits[it] * 255) })
        val colors = IntArray(200) { argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)) }
        val model = IndexColorModel(8, colors.size, colors, 0, false, -1, DataBuffer.TYPE_BYTE)
        val indices = IntArray(w * h) { rnd.nextInt(colors.size) }
        val indexed = BufferedImage(w, h, BufferedImage.TYPE_BYTE_INDEXED, model).apply { raster.setPixels(0, 0, w, h, indices) }
        list += Sample("palette", indexed, IntArray(w * h) { colors[indices[it]] })
        val colors16 = IntArray(16) { argb(255, it * 16, 255 - it * 9, 100) }
        val model16 = IndexColorModel(4, 16, colors16, 0, false, -1, DataBuffer.TYPE_BYTE)
        val indices16 = IntArray(w * h) { rnd.nextInt(16) }
        val indexed16 = BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY, model16).apply { raster.setPixels(0, 0, w, h, indices16) }
        list += Sample("4-bit palette", indexed16, IntArray(w * h) { colors16[indices16[it]] })
        val deep = IntArray(w * h) { rnd.nextInt(65536) }
        val deepGrey = BufferedImage(w, h, BufferedImage.TYPE_USHORT_GRAY).apply { raster.setPixels(0, 0, w, h, deep) }
        list += Sample("16-bit grey", deepGrey, IntArray(w * h) { grey(to8(deep[it])) })
        val deepRgb = IntArray(w * h * 3) { rnd.nextInt(65536) }
        val rgbModel = ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false, Transparency.OPAQUE, DataBuffer.TYPE_USHORT)
        val rgbRaster = rgbModel.createCompatibleWritableRaster(w, h).apply { setPixels(0, 0, w, h, deepRgb) }
        val deepRgbImage = BufferedImage(rgbModel, rgbRaster, false, null)
        list += Sample("16-bit rgb", deepRgbImage, IntArray(w * h) { argb(255, to8(deepRgb[it * 3]), to8(deepRgb[it * 3 + 1]), to8(deepRgb[it * 3 + 2])) })
        return list
    }

    private fun to8(v: Int) = (v * 255 + 32767) / 65535

    private fun imageIoTiff(images: List<BufferedImage>, compression: String?, predictor: Boolean = false): ByteArray {
        val writer = ImageIO.getImageWritersByFormatName("tif").next()
        val out = ByteArrayOutputStream()
        ImageIO.createImageOutputStream(out).use { stream ->
            writer.output = stream
            val param = writer.defaultWriteParam
            if (compression == null) {
                param.compressionMode = ImageWriteParam.MODE_DISABLED
            } else {
                param.compressionMode = ImageWriteParam.MODE_EXPLICIT
                param.compressionType = compression
            }
            fun metadata(img: BufferedImage) = if (!predictor) {
                null
            } else {
                val dir = TIFFDirectory.createFromMetadata(writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(img), param))
                dir.addTIFFField(TIFFField(BaselineTIFFTagSet.getInstance().getTag(BaselineTIFFTagSet.TAG_PREDICTOR), 2))
                dir.asMetadata
            }
            if (images.size == 1) {
                writer.write(null, IIOImage(images[0], null, metadata(images[0])), param)
            } else {
                writer.prepareWriteSequence(null)
                for (img in images) writer.writeToSequence(IIOImage(img, null, metadata(img)), param)
                writer.endWriteSequence()
            }
        }
        writer.dispose()
        return out.toByteArray()
    }

    private fun tag(tiff: ByteArray, tag: Int): Long? {
        val reader = TiffReader.of(tiff, 0, tiff.size)!!
        val entry = reader.ifd(reader.firstIfd)!!.entries.firstOrNull { it.tag == tag } ?: return null
        return reader.int(entry)
    }

    private fun assertDecodes(expected: IntArray, width: Int, height: Int, tiff: ByteArray, page: Int = 0, message: String = "") {
        val decoded = Tiff.decode(tiff, page)
        assertEquals(width, decoded.width, message)
        assertEquals(height, decoded.height, message)
        assertContentEquals(expected, decoded.pixels, message)
    }

    @Test
    fun imageIoFilesInEveryCompression() {
        val compressions = listOf(null to 1L, "PackBits" to 32773L, "LZW" to 5L, "ZLib" to 8L, "Deflate" to 32946L)
        for (sample in samples()) {
            for ((compression, code) in compressions) {
                val tiff = imageIoTiff(listOf(sample.image), compression)
                val label = "${sample.name}, ${compression ?: "uncompressed"}"
                assertEquals(code, tag(tiff, 259) ?: 1L, label)
                assertDecodes(sample.expected, sample.image.width, sample.image.height, tiff, message = label)
            }
        }
    }

    // the JDK writer applies the predictor to 8-bit samples only, 16-bit is covered by the hand-built tiles
    @Test
    fun horizontalPredictor() {
        for (sample in samples().filter { it.name in setOf("rgb", "argb", "grey") }) {
            for (compression in listOf("LZW", "ZLib")) {
                val tiff = imageIoTiff(listOf(sample.image), compression, predictor = true)
                val label = "${sample.name}, $compression"
                assertEquals(2L, tag(tiff, 317), label)
                assertDecodes(sample.expected, sample.image.width, sample.image.height, tiff, message = label)
            }
        }
    }

    @Test
    fun multiPageFromImageIo() {
        val all = samples()
        val pages = listOf(all[0], all[2], all[4])
        val tiff = imageIoTiff(pages.map { it.image }, "LZW")
        assertEquals(3, Tiff.pageCount(tiff))
        pages.forEachIndexed { i, p -> assertDecodes(p.expected, p.image.width, p.image.height, tiff, i, p.name) }
        assertFailsWith<IllegalArgumentException> { Tiff.decode(tiff, 3) }
    }

    @Test
    fun jpegAndFaxAreReported() {
        val rgb = samples()[0].image
        val jpeg = assertFailsWith<ImageFormatException> { Tiff.decode(imageIoTiff(listOf(rgb), "JPEG")) }
        assertTrue("JPEG" in jpeg.message!!)
        val bilevel = samples().first { it.name == "1-bit" }.image
        val fax = assertFailsWith<ImageFormatException> { Tiff.decode(imageIoTiff(listOf(bilevel), "CCITT T.6")) }
        assertTrue("CCITT" in fax.message!!)
    }

    private val rgbFields = listOf(
        TField(262, SHORT, 2),
        TField(277, SHORT, 3),
        TField(258, SHORT, 8, 8, 8),
    )

    private fun interleave(pixels: IntArray, alpha: Boolean = false): ByteArray {
        val n = if (alpha) 4 else 3
        return ByteArray(pixels.size * n) {
            val p = pixels[it / n]
            when (it % n) {
                0 -> (p shr 16).toByte()
                1 -> (p shr 8).toByte()
                2 -> p.toByte()
                else -> (p ushr 24).toByte()
            }
        }
    }

    @Test
    fun tiledBothByteOrders() {
        val w = 50
        val h = 37
        val src = photo(w, h, seed = 4)
        for (bigEndian in listOf(false, true)) {
            val tiles = ArrayList<ByteArray>()
            for (ty in 0 until 3) {
                for (tx in 0 until 4) {
                    tiles += ByteArray(16 * 16 * 3) {
                        val x = tx * 16 + it / 3 % 16
                        val y = ty * 16 + it / 48
                        if (x < w && y < h) interleave(intArrayOf(src[y * w + x]))[it % 3] else 0x55
                    }
                }
            }
            val fields = rgbFields + listOf(TField(256, LONG, w.toLong()), TField(257, SHORT, h.toLong()), TField(322, SHORT, 16), TField(323, SHORT, 16))
            val tiff = buildTiff(bigEndian, listOf(TPage(fields, tiles, tiled = true)))
            assertDecodes(src, w, h, tiff, message = if (bigEndian) "MM" else "II")
        }
    }

    @Test
    fun tiledDeflateWithSixteenBitPredictor() {
        val w = 40
        val h = 24
        val rnd = Random(9)
        val values = IntArray(w * h) { (it % w) * 1500 + rnd.nextInt(300) }
        val tiles = ArrayList<ByteArray>()
        for (ty in 0 until 2) {
            for (tx in 0 until 3) {
                val raw = ByteArray(16 * 16 * 2)
                for (y in 0 until 16) {
                    var prev = 0
                    for (x in 0 until 16) {
                        val gx = tx * 16 + x
                        val gy = ty * 16 + y
                        val v = if (gx < w && gy < h) values[gy * w + gx] else 0
                        val d = if (x == 0) v else (v - prev) and 0xFFFF
                        prev = v
                        raw[(y * 16 + x) * 2] = (d shr 8).toByte()
                        raw[(y * 16 + x) * 2 + 1] = d.toByte()
                    }
                }
                tiles += zlib(raw)
            }
        }
        val fields = listOf(
            TField(256, SHORT, w.toLong()),
            TField(257, SHORT, h.toLong()),
            TField(258, SHORT, 16),
            TField(259, SHORT, 8),
            TField(262, SHORT, 1),
            TField(317, SHORT, 2),
            TField(322, SHORT, 16),
            TField(323, SHORT, 16),
        )
        val tiff = buildTiff(true, listOf(TPage(fields, tiles, tiled = true)))
        assertDecodes(IntArray(w * h) { grey(to8(values[it])) }, w, h, tiff)
    }

    @Test
    fun planarStrips() {
        val w = 33
        val h = 20
        val src = photo(w, h, seed = 6).mapIndexed { i, p -> (p and 0xFFFFFF) or ((i * 3 % 256) shl 24) }.toIntArray()
        val rowsPerStrip = 7
        val strips = ArrayList<ByteArray>()
        for (plane in 0 until 4) {
            for (s in 0 until (h + rowsPerStrip - 1) / rowsPerStrip) {
                val rows = minOf(rowsPerStrip, h - s * rowsPerStrip)
                val raw = ByteArray(rows * w) {
                    val p = src[(s * rowsPerStrip + it / w) * w + it % w]
                    (if (plane == 3) p ushr 24 else p shr (16 - plane * 8)).toByte()
                }
                strips += packBits(raw)
            }
        }
        val fields = listOf(
            TField(256, SHORT, w.toLong()),
            TField(257, SHORT, h.toLong()),
            TField(258, SHORT, 8, 8, 8, 8),
            TField(259, SHORT, 32773),
            TField(262, SHORT, 2),
            TField(277, SHORT, 4),
            TField(278, SHORT, rowsPerStrip.toLong()),
            TField(284, SHORT, 2),
            TField(338, SHORT, 2),
        )
        assertDecodes(src, w, h, buildTiff(false, listOf(TPage(fields, strips))))
    }

    @Test
    fun lzwOldAndNewBitOrder() {
        val w = 300
        val h = 90
        val src = photo(w, h, seed = 7)
        val raw = interleave(src)
        for (oldStyle in listOf(false, true)) {
            val data = tiffLzw(raw, oldStyle)
            assertEquals(if (oldStyle) 0 else 0x80, data[0].toInt() and 0xFF)
            val fields = rgbFields + listOf(TField(256, SHORT, w.toLong()), TField(257, SHORT, h.toLong()), TField(259, SHORT, 5))
            assertDecodes(src, w, h, buildTiff(false, listOf(TPage(fields, listOf(data)))), message = if (oldStyle) "old-style" else "new-style")
        }
    }

    @Test
    fun lzwWithPredictorInPlanarStrips() {
        val w = 64
        val h = 30
        val src = photo(w, h, seed = 10)
        val strips = (0 until 3).map { plane ->
            val raw = ByteArray(w * h)
            for (y in 0 until h) {
                var prev = 0
                for (x in 0 until w) {
                    val v = (src[y * w + x] shr (16 - plane * 8)) and 0xFF
                    raw[y * w + x] = (v - prev).toByte()
                    prev = v
                }
            }
            tiffLzw(raw, oldStyle = false)
        }
        val fields = rgbFields + listOf(
            TField(256, SHORT, w.toLong()),
            TField(257, SHORT, h.toLong()),
            TField(259, SHORT, 5),
            TField(284, SHORT, 2),
            TField(317, SHORT, 2),
        )
        assertDecodes(src, w, h, buildTiff(true, listOf(TPage(fields, strips))))
    }

    @Test
    fun orientationIsApplied() {
        val w = 5
        val h = 3
        val src = IntArray(w * h) { argb(255, it * 10, 0, 0) }
        val expected = mapOf(
            1 to (w to intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14)),
            2 to (w to intArrayOf(4, 3, 2, 1, 0, 9, 8, 7, 6, 5, 14, 13, 12, 11, 10)),
            3 to (w to intArrayOf(14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0)),
            4 to (w to intArrayOf(10, 11, 12, 13, 14, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4)),
            5 to (h to intArrayOf(0, 5, 10, 1, 6, 11, 2, 7, 12, 3, 8, 13, 4, 9, 14)),
            6 to (h to intArrayOf(10, 5, 0, 11, 6, 1, 12, 7, 2, 13, 8, 3, 14, 9, 4)),
            7 to (h to intArrayOf(14, 9, 4, 13, 8, 3, 12, 7, 2, 11, 6, 1, 10, 5, 0)),
            8 to (h to intArrayOf(4, 9, 14, 3, 8, 13, 2, 7, 12, 1, 6, 11, 0, 5, 10)),
        )
        for ((orientation, result) in expected) {
            val fields = rgbFields + listOf(TField(256, SHORT, w.toLong()), TField(257, SHORT, h.toLong()), TField(274, SHORT, orientation.toLong()))
            val decoded = Tiff.decode(buildTiff(false, listOf(TPage(fields, listOf(interleave(src))))))
            assertEquals(result.first, decoded.width, "orientation $orientation")
            assertContentEquals(result.second.map { src[it] }.toIntArray(), decoded.pixels, "orientation $orientation")
        }
    }

    @Test
    fun cmykWhiteIsZeroAndAssociatedAlpha() {
        val cmyk = byteArrayOf(0, 0, 0, 0, -1, 0, 0, 0, 0, -1, 0, 0, 0, 0, -1, 0, 0, 0, 0, -1, 64, 32, 16, 8)
        val cmykFields = listOf(TField(256, SHORT, 6), TField(257, SHORT, 1), TField(258, SHORT, 8, 8, 8, 8), TField(262, SHORT, 5), TField(277, SHORT, 4))
        val expectedCmyk = intArrayOf(
            argb(255, 255, 255, 255),
            argb(255, 0, 255, 255),
            argb(255, 255, 0, 255),
            argb(255, 255, 255, 0),
            argb(255, 0, 0, 0),
        )
        val last = argb(255, (191 * 247 + 127) / 255, (223 * 247 + 127) / 255, (239 * 247 + 127) / 255)
        val cmykPixels = Tiff.decode(buildTiff(false, listOf(TPage(cmykFields, listOf(cmyk))))).pixels
        assertContentEquals(expectedCmyk, cmykPixels.copyOf(5))
        assertEquals(last, cmykPixels[5])

        val white = listOf(TField(256, SHORT, 4), TField(257, SHORT, 1), TField(258, SHORT, 8), TField(262, SHORT, 0))
        val whitePixels = Tiff.decode(buildTiff(true, listOf(TPage(white, listOf(byteArrayOf(0, 64, -128, -1)))))).pixels
        assertContentEquals(intArrayOf(grey(255), grey(191), grey(127), grey(0)), whitePixels)
        val whiteBits = listOf(TField(256, SHORT, 8), TField(257, SHORT, 1), TField(258, SHORT, 1), TField(262, SHORT, 0), TField(266, SHORT, 2))
        val bitPixels = Tiff.decode(buildTiff(false, listOf(TPage(whiteBits, listOf(byteArrayOf(0x03)))))).pixels
        assertContentEquals(intArrayOf(0, 0, 255, 255, 255, 255, 255, 255).map { grey(it) }.toIntArray(), bitPixels)

        val premultiplied = listOf(
            TField(256, SHORT, 2),
            TField(257, SHORT, 1),
            TField(258, SHORT, 8, 8, 8, 8),
            TField(262, SHORT, 2),
            TField(277, SHORT, 4),
            TField(338, SHORT, 1),
        )
        val associated = Tiff.decode(buildTiff(false, listOf(TPage(premultiplied, listOf(byteArrayOf(64, 32, 0, -128, 0, 0, 0, 0)))))).pixels
        assertContentEquals(intArrayOf(argb(128, 128, 64, 0), 0), associated)
    }

    @Test
    fun handBuiltMultiPage() {
        val pages = listOf(photo(10, 6, seed = 1), photo(7, 9, seed = 2), photo(3, 3, seed = 3))
        val sizes = listOf(10 to 6, 7 to 9, 3 to 3)
        val tiff = buildTiff(true, pages.mapIndexed { i, p ->
            val (w, h) = sizes[i]
            TPage(rgbFields + listOf(TField(256, SHORT, w.toLong()), TField(257, SHORT, h.toLong())), listOf(interleave(p)))
        })
        assertEquals(3, Tiff.pageCount(tiff))
        pages.forEachIndexed { i, p -> assertDecodes(p, sizes[i].first, sizes[i].second, tiff, i) }
    }

    @Test
    fun unsupportedVariantsSayWhy() {
        val base = listOf(TField(256, SHORT, 2), TField(257, SHORT, 1), TField(262, SHORT, 1))
        val cases = listOf(
            listOf(TField(258, SHORT, 32), TField(339, SHORT, 3)) to "Floating-point",
            listOf(TField(258, SHORT, 8), TField(317, SHORT, 3)) to "predictor",
            listOf(TField(258, SHORT, 12)) to "12 bits",
            listOf(TField(258, SHORT, 8), TField(259, SHORT, 34712)) to "compression 34712",
        )
        for ((extra, words) in cases) {
            val tiff = buildTiff(false, listOf(TPage(base + extra, listOf(ByteArray(16)))))
            val e = assertFailsWith<ImageFormatException> { Tiff.decode(tiff) }
            assertTrue(words in e.message!!, "${e.message} should mention $words")
        }
        val big = byteArrayOf(0x49, 0x49, 0x2B, 0, 8, 0, 0, 0, 16, 0, 0, 0, 0, 0, 0, 0)
        assertTrue("BigTIFF" in assertFailsWith<ImageFormatException> { Tiff.decode(big) }.message!!)
        assertFailsWith<ImageFormatException> { Tiff.decode(ascii("not a tiff at all")) }
    }

    @Test
    fun corruptFilesOnlyThrowFormatErrors() {
        val sources = listOf(
            imageIoTiff(listOf(samples()[0].image), "LZW"),
            imageIoTiff(listOf(samples()[1].image), "ZLib", predictor = true),
            imageIoTiff(listOf(samples()[4].image), "PackBits"),
        )
        val rnd = Random(77)
        repeat(3000) { round ->
            val src = sources[round % sources.size]
            val bytes = if (round % 5 == 0) {
                src.copyOf(rnd.nextInt(src.size))
            } else {
                src.copyOf().also { b -> repeat(1 + rnd.nextInt(4)) { b[rnd.nextInt(b.size)] = rnd.nextInt(256).toByte() } }
            }
            try {
                Tiff.decode(bytes)
                Tiff.pageCount(bytes)
            } catch (e: ImageFormatException) {
                // expected for broken structure
            } catch (e: IllegalArgumentException) {
                // page list came out empty or shorter
            } catch (e: Throwable) {
                fail("round $round: ${e::class.simpleName}: ${e.message}")
            }
        }
    }

    private fun zlib(data: ByteArray): ByteArray {
        val d = Deflater(9).apply {
            setInput(data)
            finish()
        }
        val out = ByteArrayOutputStream()
        val buf = ByteArray(4096)
        while (!d.finished()) out.write(buf, 0, d.deflate(buf))
        return out.toByteArray()
    }

    private fun packBits(data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        var i = 0
        while (i < data.size) {
            var run = 1
            while (i + run < data.size && run < 128 && data[i + run] == data[i]) run++
            if (run > 1) {
                out.write(1 - run)
                out.write(data[i].toInt())
                i += run
            } else {
                var n = 1
                while (i + n < data.size && n < 128 && (i + n + 1 >= data.size || data[i + n] != data[i + n + 1])) n++
                out.write(n - 1)
                out.write(data, i, n)
                i += n
            }
        }
        return out.toByteArray()
    }
}

const val SHORT = 3
const val LONG = 4

class TField(val tag: Int, val type: Int, vararg val values: Long)

class TPage(val fields: List<TField>, val chunks: List<ByteArray>, val tiled: Boolean = false)

fun buildTiff(bigEndian: Boolean, pages: List<TPage>): ByteArray {
    val out = TiffSink(bigEndian)
    out.bytes(ascii(if (bigEndian) "MM\u0000*" else "II*\u0000"))
    out.u32(0)
    var nextAt = 4
    for (page in pages) {
        val offsets = page.chunks.map { chunk ->
            out.align()
            val at = out.size
            out.bytes(chunk)
            at.toLong()
        }
        out.align()
        val fields = (
            page.fields +
                TField(if (page.tiled) 324 else 273, LONG, *offsets.toLongArray()) +
                TField(if (page.tiled) 325 else 279, LONG, *page.chunks.map { it.size.toLong() }.toLongArray())
            ).sortedBy { it.tag }
        out.patch32(nextAt, out.size.toLong())
        out.u16(fields.size)
        var valueAt = out.size + fields.size * 12 + 4
        val deferred = ArrayList<TField>()
        for (f in fields) {
            val size = (if (f.type == SHORT) 2 else 4) * f.values.size
            out.u16(f.tag)
            out.u16(f.type)
            out.u32(f.values.size.toLong())
            if (size <= 4) {
                f.values.forEach { if (f.type == SHORT) out.u16(it.toInt()) else out.u32(it) }
                repeat(4 - size) { out.u8(0) }
            } else {
                out.u32(valueAt.toLong())
                valueAt += size
                deferred += f
            }
        }
        nextAt = out.size
        out.u32(0)
        for (f in deferred) f.values.forEach { if (f.type == SHORT) out.u16(it.toInt()) else out.u32(it) }
    }
    return out.toByteArray()
}

class TiffSink(private val bigEndian: Boolean) {
    private val out = ByteArrayOutputStream()
    private var patches = ArrayList<Pair<Int, Long>>()
    val size get() = out.size()

    fun u8(v: Int) = out.write(v)

    fun u16(v: Int) {
        if (bigEndian) {
            u8(v shr 8)
            u8(v and 0xFF)
        } else {
            u8(v and 0xFF)
            u8(v shr 8)
        }
    }

    fun u32(v: Long) {
        if (bigEndian) {
            u16((v shr 16).toInt() and 0xFFFF)
            u16(v.toInt() and 0xFFFF)
        } else {
            u16(v.toInt() and 0xFFFF)
            u16((v shr 16).toInt() and 0xFFFF)
        }
    }

    fun bytes(b: ByteArray) = out.write(b)

    fun align() {
        if (size % 2 == 1) u8(0)
    }

    fun patch32(at: Int, v: Long) {
        patches += at to v
    }

    fun toByteArray(): ByteArray {
        val bytes = out.toByteArray()
        for ((at, v) in patches) {
            for (i in 0 until 4) {
                val shift = if (bigEndian) 24 - i * 8 else i * 8
                bytes[at + i] = (v shr shift).toByte()
            }
        }
        return bytes
    }
}

fun tiffLzw(data: ByteArray, oldStyle: Boolean): ByteArray {
    val out = ByteArrayOutputStream()
    var acc = 0L
    var bits = 0
    fun put(code: Int, width: Int) {
        if (oldStyle) {
            acc = acc or (code.toLong() shl bits)
            bits += width
            while (bits >= 8) {
                out.write((acc and 0xFF).toInt())
                acc = acc ushr 8
                bits -= 8
            }
        } else {
            acc = (acc shl width) or code.toLong()
            bits += width
            while (bits >= 8) {
                out.write(((acc shr (bits - 8)) and 0xFF).toInt())
                bits -= 8
            }
        }
    }
    val limit = if (oldStyle) 0 else 1
    val table = HashMap<Long, Int>()
    var width = 9
    var next = 258
    put(256, width)
    var prefix = data[0].toInt() and 0xFF
    for (i in 1 until data.size) {
        val k = data[i].toInt() and 0xFF
        val key = (prefix.toLong() shl 8) or k.toLong()
        val code = table[key]
        if (code != null) {
            prefix = code
            continue
        }
        put(prefix, width)
        table[key] = next++
        if (next == 4094) {
            put(256, width)
            table.clear()
            next = 258
            width = 9
        } else if (next > (1 shl width) - limit) {
            width++
        }
        prefix = k
    }
    put(prefix, width)
    next++
    if (next > (1 shl width) - limit && width < 12) width++
    put(257, width)
    if (bits > 0) {
        if (oldStyle) out.write((acc and 0xFF).toInt()) else out.write(((acc shl (8 - bits)) and 0xFF).toInt())
    }
    return out.toByteArray()
}
