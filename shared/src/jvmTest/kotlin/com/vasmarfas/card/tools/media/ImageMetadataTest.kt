package com.vasmarfas.card.tools.media

import org.apache.commons.imaging.Imaging
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata
import org.apache.commons.imaging.formats.jpeg.xmp.JpegXmpRewriter
import java.awt.color.ColorSpace
import java.awt.color.ICC_Profile
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

class ImageMetadataTest {
    private val icc: ByteArray = ICC_Profile.getInstance(ColorSpace.CS_sRGB).data
    private val xmp = """<x:xmpmeta xmlns:x="adobe:ns:meta/"><rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">""" +
        """<rdf:Description xmlns:exif="http://ns.adobe.com/exif/1.0/" exif:GPSLatitude="55,45.348N"/></rdf:RDF></x:xmpmeta>"""

    private fun withXmp(jpeg: ByteArray): ByteArray = ByteArrayOutputStream().also { JpegXmpRewriter().updateXmpXml(jpeg, it, xmp) }.toByteArray()

    private fun iptc(): ByteArray {
        val record = byteArrayOf(0x1C, 0x02, 0x78, 0x00, 0x05) + ascii("Hello")
        return ascii("Photoshop 3.0\u0000") + ascii("8BIM") + byteArrayOf(0x04, 0x04, 0, 0) + be32(record.size) + record + byteArrayOf(0)
    }

    private fun fullyTagged(orientation: Int = 1): ByteArray {
        val jpeg = withXmp(exifJpeg(orientation = orientation))
        val extra = withSegments(
            jpeg,
            segment(0xE2, ascii("ICC_PROFILE\u0000") + byteArrayOf(1, 1) + icc),
            segment(0xE2, ascii("MPF\u0000") + ByteArray(40)),
            segment(0xED, iptc()),
            segment(0xEC, ascii("Ducky") + ByteArray(12)),
            segment(0xEE, ascii("Adobe") + byteArrayOf(0, 100, 0, 0, 0, 0, 1)),
            segment(0xFE, ascii("shot on a phone near home")),
        )
        return extra + exifJpeg(gps = GpsFix(1.0, 2.0, 3.0))
    }

    private fun markers(jpeg: ByteArray) = jpegSegments(jpeg).map { it.marker }

    @Test
    fun jpegLosesEverythingButPixelsAndProfile() {
        val original = fullyTagged()
        assertNotNull(Imaging.getXmpXml(original))
        assertNotNull((Imaging.getMetadata(original) as JpegImageMetadata).exif)
        assertNotNull((Imaging.getMetadata(original) as JpegImageMetadata).photoshop)

        val stripped = ImageMetadata.strip(original)
        val metadata = Imaging.getMetadata(stripped) as JpegImageMetadata?
        assertNull(metadata?.exif)
        assertNull(metadata?.photoshop)
        assertNull(Imaging.getXmpXml(stripped))
        assertNull(Exif.read(stripped))
        assertContentEquals(icc, Imaging.getIccProfileBytes(stripped))
        val kept = markers(stripped)
        assertTrue(0xEE in kept, "APP14 Adobe stays")
        assertTrue(0xE0 in kept, "JFIF stays")
        assertTrue(kept.none { it == 0xE1 || it == 0xED || it == 0xEC || it == 0xFE })
        assertEquals(1, kept.count { it == 0xE2 }, "only the ICC APP2 stays")
        assertContentEquals(scanData(original.copyOf(original.size - exifJpeg(gps = GpsFix(1.0, 2.0, 3.0)).size)), scanData(stripped))
        assertContentEquals(pixels(decode(original)), pixels(decode(stripped)))
        assertEquals(-1, stripped.indexOf(ascii("Exif")), "the trailing image after EOI carries EXIF of its own and must go")
        assertContentEquals(byteArrayOf(-1, 0xD9.toByte()), stripped.copyOfRange(stripped.size - 2, stripped.size))

        val noProfile = ImageMetadata.strip(original, keepColorProfile = false)
        assertNull(Imaging.getIccProfileBytes(noProfile))
        assertContentEquals(pixels(decode(original)), pixels(decode(noProfile)))
    }

    @Test
    fun jpegKeepsOnlyTheOrientation() {
        val original = fullyTagged(orientation = 6)
        val stripped = ImageMetadata.strip(original)
        val exif = assertNotNull((Imaging.getMetadata(stripped) as JpegImageMetadata).exif)
        assertEquals(1, exif.allFields.size)
        assertEquals(6, exif.allFields[0].intValue)
        assertNull(exif.gpsInfo)
        val ours = assertNotNull(Exif.read(stripped))
        assertEquals(6, ours.orientation)
        assertEquals(1, ours.tags.size)
        assertEquals(listOf(0xE0, 0xE1), markers(stripped).take(2))
        assertContentEquals(pixels(decode(original)), pixels(decode(stripped)))

        val flat = ImageMetadata.strip(original, keepOrientation = false)
        assertNull(Exif.read(flat))
        assertTrue(0xE1 !in markers(flat))
    }

    private fun zlib(data: ByteArray): ByteArray {
        val d = Deflater().apply {
            setInput(data)
            finish()
        }
        val out = ByteArrayOutputStream()
        val buf = ByteArray(4096)
        while (!d.finished()) out.write(buf, 0, d.deflate(buf))
        return out.toByteArray()
    }

    @Test
    fun pngTextExifAndTimeGo() {
        val w = 40
        val h = 30
        val src = photo(w, h)
        val chunks = pngChunks(Png.encode(src, w, h)).map { it.type to it.data }
        val tiff = exifBlock(exifJpeg(orientation = 8))
        val tagged = pngOf(
            chunks.take(1) + listOf(
                "iCCP" to ascii("sRGB\u0000\u0000") + zlib(icc),
                "tEXt" to ascii("Comment\u0000taken at home"),
                "zTXt" to ascii("Author\u0000\u0000") + zlib(ascii("someone")),
                "iTXt" to ascii("XML:com.adobe.xmp\u0000\u0000\u0000\u0000\u0000") + ascii(xmp),
                "eXIf" to tiff,
                "tIME" to byteArrayOf(0x07, 0xE8.toByte(), 5, 1, 12, 0, 0),
                "dSIG" to ByteArray(8),
            ) + chunks.drop(1),
        ) + ascii("trailing junk")
        assertNotNull(Imaging.getXmpXml(tagged))

        val stripped = ImageMetadata.strip(tagged)
        val result = pngChunks(stripped)
        assertEquals(listOf("IHDR", "iCCP", "eXIf", "IDAT", "IEND"), result.map { it.type })
        assertTrue(result.all { it.crcValid })
        assertNull(Imaging.getXmpXml(stripped))
        val exif = assertNotNull(Exif.read(stripped))
        assertEquals(8, exif.orientation)
        assertEquals(1, exif.tags.size)
        assertContentEquals(src, pixels(decode(stripped)))

        val bare = pngChunks(ImageMetadata.strip(tagged, keepOrientation = false, keepColorProfile = false))
        assertEquals(listOf("IHDR", "IDAT", "IEND"), bare.map { it.type })
    }

    private class RiffChunk(val fourcc: String, val data: ByteArray)

    private fun riffChunks(webp: ByteArray): List<RiffChunk> {
        assertEquals(webp.size - 8, le32At(webp, 4), "RIFF size")
        val list = ArrayList<RiffChunk>()
        var pos = 12
        while (pos < webp.size) {
            val size = le32At(webp, pos + 4)
            list += RiffChunk(String(webp, pos, 4, Charsets.ISO_8859_1), webp.copyOfRange(pos + 8, pos + 8 + size))
            pos += 8 + size + (size and 1)
        }
        assertEquals(webp.size, pos, "chunks must fill the RIFF body")
        return list
    }

    private fun le32At(b: ByteArray, p: Int) = (b[p].toInt() and 0xFF) or ((b[p + 1].toInt() and 0xFF) shl 8) or ((b[p + 2].toInt() and 0xFF) shl 16) or
        ((b[p + 3].toInt() and 0xFF) shl 24)

    @Test
    fun webpDropsExifAndXmpAndFixesFlags() {
        val tiff = exifBlock(exifJpeg(orientation = 3))
        val tagged = webp(vp8x(0x20 or 0x08 or 0x04), webpChunk("ICCP", icc), vp8lChunk, webpChunk("EXIF", tiff), webpChunk("XMP ", ascii(xmp)))
        assertNotNull(Imaging.getXmpXml(tagged))
        assertNotNull(Imaging.getMetadata(tagged))

        val stripped = ImageMetadata.strip(tagged)
        val chunks = riffChunks(stripped)
        assertEquals(listOf("VP8X", "ICCP", "VP8L", "EXIF"), chunks.map { it.fourcc })
        assertEquals(0x20 or 0x08, chunks[0].data[0].toInt())
        assertContentEquals(vp8lChunk.copyOfRange(8, 21), chunks[2].data)
        assertNull(Imaging.getXmpXml(stripped))
        assertEquals(1, Imaging.getImageInfo(stripped).width)
        val exif = assertNotNull(Exif.read(stripped))
        assertEquals(3, exif.orientation)
        assertEquals(1, exif.tags.size)

        val bare = ImageMetadata.strip(tagged, keepOrientation = false, keepColorProfile = false)
        val bareChunks = riffChunks(bare)
        assertEquals(listOf("VP8X", "VP8L"), bareChunks.map { it.fourcc })
        assertEquals(0, bareChunks[0].data[0].toInt())
        assertEquals(1, Imaging.getImageInfo(bare).height)
    }

    @Test
    fun gifKeepsLoopingAndDropsComments() {
        val w = 30
        val h = 20
        val frames = listOf(poster(w, h, 12), poster(w, h, 12, seed = 8))
        val gif = GifWriter(w, h, loopCount = 2).apply { frames.forEach { addFrame(it, 70) } }.finish()
        val insertAt = 13 + ascii("NETSCAPE2.0").size + 3 + 5
        val comment = byteArrayOf(0x21, 0xFE.toByte(), 9) + ascii("home trip") + byteArrayOf(0)
        val xmpBlock = byteArrayOf(0x21, 0xFF.toByte(), 11) + ascii("XMP DataXMP") + ascii(xmp) + byteArrayOf(1) + ByteArray(256) { (255 - it).toByte() } +
            byteArrayOf(0)
        val iccBlock = byteArrayOf(0x21, 0xFF.toByte(), 11) + ascii("ICCRGBG1012") + icc.toList().chunked(255).fold(ByteArray(0)) { a, c ->
            a + byteArrayOf(c.size.toByte()) + c.toByteArray()
        } + byteArrayOf(0)
        val tagged = gif.copyOf(insertAt) + comment + xmpBlock + iccBlock + gif.copyOfRange(insertAt, gif.size)
        assertNotNull(Imaging.getXmpXml(tagged))

        val stripped = ImageMetadata.strip(tagged)
        assertNull(Imaging.getXmpXml(stripped))
        assertEquals(-1, stripped.indexOf(ascii("home trip")))
        assertTrue(stripped.indexOf(ascii("ICCRGBG1012")) > 0)
        val decoded = GifTest.decodeGif(stripped, w, h)
        assertEquals(2, decoded.loopCount)
        frames.forEachIndexed { i, f -> assertContentEquals(f, decoded.composited[i]) }
        assertEquals(-1, ImageMetadata.strip(tagged, keepColorProfile = false).indexOf(ascii("ICCRGBG1012")))
        assertContentEquals(gif, ImageMetadata.strip(gif))
    }

    @Test
    fun otherContainersComeBackUnchanged() {
        val bmp = Bmp.encode(photo(5, 5), 5, 5)
        val tiff = encodeWith(image(photo(5, 5), 5, 5), "tif")
        val heif = heif(exifBlock(exifJpeg()), inIdat = false)
        assertEquals(ImageContainer.BMP, ImageMetadata.detect(bmp))
        assertEquals(ImageContainer.TIFF, ImageMetadata.detect(tiff))
        assertEquals(ImageContainer.HEIF, ImageMetadata.detect(heif))
        assertEquals(ImageContainer.UNKNOWN, ImageMetadata.detect(ascii("hello")))
        assertEquals(ImageContainer.JPEG, ImageMetadata.detect(baseJpeg))
        assertEquals(ImageContainer.PNG, ImageMetadata.detect(Png.encode(photo(2, 2), 2, 2)))
        assertEquals(ImageContainer.WEBP, ImageMetadata.detect(webp(vp8lChunk)))
        assertEquals(ImageContainer.GIF, ImageMetadata.detect(GifWriter(2, 2).apply { addFrame(IntArray(4), 10) }.finish()))
        for (file in listOf(bmp, tiff, heif)) assertSame(file, ImageMetadata.strip(file))
    }

    @Test
    fun brokenFilesFailCleanly() {
        val jpeg = fullyTagged(orientation = 5)
        assertFailsWith<ImageFormatException> { ImageMetadata.strip(jpeg.copyOf(300)) }
        val png = Png.encode(photo(20, 20), 20, 20)
        val gif = GifWriter(20, 20).apply { addFrame(photo(20, 20), 50) }.finish()
        val webpFile = webp(vp8x(0x08), vp8lChunk, webpChunk("EXIF", exifBlock(exifJpeg(orientation = 2))))
        val rnd = Random(3)
        val files = listOf(jpeg, png, gif, webpFile)
        var failures = 0
        repeat(2000) { round ->
            val src = files[round % files.size]
            val broken = if (round % 3 == 0) {
                src.copyOf(rnd.nextInt(8, src.size))
            } else {
                src.copyOf().also { b -> repeat(1 + rnd.nextInt(3)) { b[rnd.nextInt(b.size)] = rnd.nextInt(256).toByte() } }
            }
            try {
                ImageMetadata.strip(broken)
            } catch (e: ImageFormatException) {
                failures++
            } catch (e: Throwable) {
                fail("round $round: ${e::class.simpleName} ${e.message}")
            }
        }
        assertTrue(failures > 0)
        assertFalse(failures == 2000)
    }
}
