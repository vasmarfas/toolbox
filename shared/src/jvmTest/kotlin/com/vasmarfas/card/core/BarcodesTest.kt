package com.vasmarfas.card.core

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlin.test.Test
import kotlin.test.assertEquals

class BarcodesTest {
    private fun scan(matrix: BitMatrix): List<ScannedCode> {
        val pixels = IntArray(matrix.width * matrix.height) { if (matrix[it % matrix.width, it / matrix.width]) 0xFF000000.toInt() else -1 }
        return scanPixels(pixels, matrix.width, matrix.height)
    }

    @Test
    fun readsQrCodesWithTheirContent() {
        val text = "WIFI:T:WPA;S:Home;P:pa\\;ss;H:false;;"
        val codes = scan(MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, 300, 300))
        assertEquals(listOf(text), codes.map { it.text })
        assertEquals("QR", codes.single().format)
    }

    @Test
    fun readsProductBarcodes() {
        val codes = scan(MultiFormatWriter().encode("5901234123457", BarcodeFormat.EAN_13, 400, 120))
        assertEquals("5901234123457", codes.single().text)
        assertEquals("EAN-13", codes.single().format)
    }

    @Test
    fun anEmptyPictureHasNoCodes() {
        assertEquals(emptyList(), scanPixels(IntArray(200 * 200) { -1 }, 200, 200))
    }
}
