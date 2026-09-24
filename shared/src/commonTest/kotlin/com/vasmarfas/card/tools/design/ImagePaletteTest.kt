package com.vasmarfas.card.tools.design

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImagePaletteTest {
    @Test
    fun mainColoursComeFirstAndTransparencyIsIgnored() {
        val red = 0xFFD32F2F.toInt()
        val blue = 0xFF1976D2.toInt()
        val pixels = IntArray(100) {
            when {
                it < 60 -> red
                it < 90 -> blue
                else -> 0
            }
        }
        val entries = ImagePalette.extract(pixels, 10, 10, 2)
        assertEquals(listOf("#D32F2F", "#1976D2"), entries.map { it.color.hex() })
        assertTrue(abs(entries[0].share - 60.0 / 90) < 1e-9)
        assertTrue(abs(entries.sumOf { it.share } - 1.0) < 1e-9)
    }

    @Test
    fun exportFormats() {
        val entries = listOf(PaletteEntry(Rgba(50, 103, 115), 0.7), PaletteEntry(Rgba(240, 234, 225), 0.3))
        assertEquals("#326773\n#F0EAE1", ImagePalette.export(entries, PaletteFormat.HEX))
        assertEquals(":root {\n  --color-1: #326773;\n  --color-2: #F0EAE1;\n}", ImagePalette.export(entries, PaletteFormat.CSS))
        assertEquals("[\"#326773\", \"#F0EAE1\"]", ImagePalette.export(entries, PaletteFormat.JSON))
    }
}
