package com.vasmarfas.card.core

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.platform.registerSkikoComposeImplementation
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(InternalComposeUiApi::class)
class ImagesTest {
    init {
        registerSkikoComposeImplementation()
    }

    private fun fixture(name: String): ByteArray = assertNotNull(javaClass.getResourceAsStream("/$name")).readBytes()

    // stored 200x100 with a blue left half, Orientation = 6
    @Test
    fun skiaTurnsPhotosUprightByTheExifOrientation() {
        val raw = assertNotNull(runBlocking { decodeRawImage(fixture("exif-orientation-6.jpg")) })
        assertTrue(raw.oriented)
        assertEquals(100, raw.bitmap.width)
        assertEquals(200, raw.bitmap.height)
        val pixels = raw.bitmap.pixels()
        assertTrue((pixels[50 * 100 + 50] and 0xFF) > 200, "rotated clockwise, the blue half must end up on top")
        assertTrue(((pixels[150 * 100 + 50] shr 16) and 0xFF) > 200, "and the red half below it")
    }

    @Test
    fun exifOrientationSixTurnsClockwise() {
        val stored = imageBitmapOf(IntArray(4 * 2) { if (it % 4 < 2) 0xFF0000FF.toInt() else 0xFFFF0000.toInt() }, 4, 2)
        val upright = stored.oriented(6)
        assertEquals(2, upright.width)
        assertEquals(4, upright.height)
        assertEquals(0xFF0000FF.toInt(), upright.pixels()[0])
        assertEquals(0xFFFF0000.toInt(), upright.pixels()[7])
    }

    @Test
    fun transformsKeepPixelsWhereExpected() {
        val w = 3
        val h = 2
        val source = imageBitmapOf(IntArray(w * h) { 0xFF000000.toInt() or it }, w, h)
        assertEquals(listOf(2, 1, 0, 5, 4, 3), source.transformed(0, mirror = true).pixels().map { it and 0xFF })
        assertEquals(listOf(3, 0, 4, 1, 5, 2), source.transformed(1, mirror = false).pixels().map { it and 0xFF })
        assertEquals(listOf(5, 4, 3, 2, 1, 0), source.transformed(2, mirror = false).pixels().map { it and 0xFF })
        assertEquals(listOf(2, 5, 1, 4, 0, 3), source.transformed(3, mirror = false).pixels().map { it and 0xFF })
    }

    @Test
    fun encodedImagesDecodeBack() {
        val source = imageBitmapOf(IntArray(64 * 48) { if (it % 64 < 32) 0xFF2060A0.toInt() else 0x80FF0000.toInt() }, 64, 48)
        EncodedFormat.entries.forEach { format ->
            val decoded = assertNotNull(runBlocking { decodeRawImage(source.encode(format, 90)) }, format.name).bitmap
            assertEquals(64, decoded.width)
            assertEquals(48, decoded.height)
        }
        val png = assertNotNull(runBlocking { decodeRawImage(source.encode(EncodedFormat.PNG, 100)) }).bitmap.pixels()
        assertEquals(0xFF2060A0.toInt(), png[0])
        assertEquals(0x80, png[40] ushr 24)
    }
}
