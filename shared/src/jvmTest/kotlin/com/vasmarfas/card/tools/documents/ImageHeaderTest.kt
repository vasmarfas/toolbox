package com.vasmarfas.card.tools.documents

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ImageHeaderTest {
    @Test
    fun sizes() {
        assertEquals(40 to 30, ImageHeader.size(TestImages.png(40, 30)))
        assertEquals(640 to 480, ImageHeader.size(TestImages.jpeg(640, 480)))
        assertEquals(12 to 34, ImageHeader.size(TestImages.gif(12, 34)))
        assertEquals(100 to 50, ImageHeader.size(TestImages.bmp(100, 50)))
        assertEquals(300 to 200, ImageHeader.size(TestImages.webpLossy(300, 200)))
        assertEquals(1024 to 16384, ImageHeader.size(TestImages.webpLossless(1024, 16384)))
        assertEquals(5000 to 70000, ImageHeader.size(TestImages.webpExtended(5000, 70000)))
        assertEquals(1234 to 70000, ImageHeader.size(TestImages.tiff(1234, 70000)))
    }

    @Test
    fun exifOrientationSwapsSides() {
        assertEquals(640 to 480, ImageHeader.size(TestImages.jpeg(640, 480, orientation = 1)))
        assertEquals(640 to 480, ImageHeader.size(TestImages.jpeg(640, 480, orientation = 3)))
        for (o in 5..8) assertEquals(480 to 640, ImageHeader.size(TestImages.jpeg(640, 480, orientation = o)), "orientation $o")
    }

    @Test
    fun mimeTypes() {
        assertEquals("image/png", ImageHeader.mimeType(TestImages.png(1, 1)))
        assertEquals("image/jpeg", ImageHeader.mimeType(TestImages.jpeg(1, 1)))
        assertEquals("image/gif", ImageHeader.mimeType(TestImages.gif(1, 1)))
        assertEquals("image/bmp", ImageHeader.mimeType(TestImages.bmp(1, 1)))
        assertEquals("image/webp", ImageHeader.mimeType(TestImages.webpLossless(1, 1)))
        assertEquals("image/tiff", ImageHeader.mimeType(TestImages.tiff(1, 1)))
        assertEquals("image/svg+xml", ImageHeader.mimeType("<?xml version=\"1.0\"?>\n<!-- c --><!DOCTYPE svg><svg xmlns=\"http://www.w3.org/2000/svg\"/>".encodeToByteArray()))
        val emf = ByteArray(44).also {
            it[0] = 1
            " EMF".encodeToByteArray().copyInto(it, 40)
        }
        assertEquals("image/emf", ImageHeader.mimeType(emf))
        assertEquals("image/wmf", ImageHeader.mimeType(byteArrayOf(0xD7.toByte(), 0xCD.toByte(), 0xC6.toByte(), 0x9A.toByte(), 0, 0)))
        assertNull(ImageHeader.mimeType("<html></html>".encodeToByteArray()))
        assertNull(ImageHeader.mimeType(ByteArray(0)))
    }

    @Test
    fun damagedHeaders() {
        val png = TestImages.png(40, 30)
        for (n in 0 until 30) ImageHeader.size(png.copyOf(n))
        val jpeg = TestImages.jpeg(640, 480, orientation = 6)
        for (n in 0 until jpeg.size) ImageHeader.size(jpeg.copyOf(n))
        assertNull(ImageHeader.size(png.copyOf(20)))
        assertNull(ImageHeader.size(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xC0.toByte(), 0, 1)))
        assertNull(ImageHeader.size("GIF89a".encodeToByteArray()))
        assertNull(ImageHeader.size("<svg/>".encodeToByteArray()))
    }
}
