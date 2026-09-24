package com.vasmarfas.card.tools.media

import org.apache.commons.imaging.bytesource.ByteSource
import org.apache.commons.imaging.formats.ico.IcoImageParser
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BmpIcoTest {
    private fun le(bytes: ByteArray) = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

    @Test
    fun opaqueBmpIs24Bit() {
        for (w in listOf(1, 2, 3, 37, 64)) {
            val h = 23
            val src = photo(w, h)
            val bmp = Bmp.encode(src, w, h)
            val header = le(bmp)
            assertEquals(bmp.size, header.getInt(2))
            assertEquals(54, header.getInt(10))
            assertEquals(24, header.getShort(28).toInt())
            assertEquals(0, header.getInt(30))
            assertEquals(54 + ((w * 3 + 3) / 4 * 4) * h, bmp.size)
            assertContentEquals(src, pixels(decode(bmp)), "width $w")
        }
    }

    @Test
    fun alphaBmpIs32BitWithBitfields() {
        val w = 45
        val h = 17
        val src = photo(w, h).mapIndexed { i, p -> (p and 0xFFFFFF) or ((i * 11 % 256) shl 24) }.toIntArray()
        val bmp = Bmp.encode(src, w, h)
        val header = le(bmp)
        assertEquals(108, header.getInt(14))
        assertEquals(32, header.getShort(28).toInt())
        assertEquals(3, header.getInt(30))
        assertEquals(0xFF000000.toInt(), header.getInt(66))
        assertEquals(14 + 108 + w * h * 4, bmp.size)
        assertContentEquals(src, pixels(decode(bmp)))
    }

    @Test
    fun icoEntriesPointAtTheirPngs() {
        val sizes = listOf(16, 48, 256)
        val images = sizes.map { s -> IcoImage(s, s, Png.encode(photo(s, s, seed = s), s, s)) }
        val ico = Ico.encode(images)
        val buf = le(ico)
        assertEquals(0, buf.getShort(0).toInt())
        assertEquals(1, buf.getShort(2).toInt())
        assertEquals(sizes.size, buf.getShort(4).toInt())
        for ((i, s) in sizes.withIndex()) {
            val entry = 6 + i * 16
            assertEquals(s % 256, ico[entry].toInt() and 0xFF)
            assertEquals(s % 256, ico[entry + 1].toInt() and 0xFF)
            assertEquals(1, buf.getShort(entry + 4).toInt())
            assertEquals(32, buf.getShort(entry + 6).toInt())
            val size = buf.getInt(entry + 8)
            val offset = buf.getInt(entry + 12)
            assertContentEquals(images[i].png, ico.copyOfRange(offset, offset + size))
            val decoded = decode(ico.copyOfRange(offset, offset + size))
            assertEquals(s, decoded.width)
        }
        val read = IcoImageParser().getAllBufferedImages(ByteSource.array(ico))
        assertEquals(sizes, read.map { it.width })
        assertContentEquals(photo(48, 48, seed = 48), pixels(read[1]))
    }

    @Test
    fun icoRejectsOversizedImages() {
        assertFailsWith<IllegalArgumentException> { Ico.encode(listOf(IcoImage(257, 16, ByteArray(0)))) }
        assertFailsWith<IllegalArgumentException> { Ico.encode(emptyList()) }
    }
}
