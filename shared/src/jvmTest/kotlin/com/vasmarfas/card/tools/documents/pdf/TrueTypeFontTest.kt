package com.vasmarfas.card.tools.documents.pdf

import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TrueTypeFontTest {
    private val sample = "The quick brown fox, 2026. Съешь же ещё этих мягких французских булок! «Ёж» — №5 ±1 €"
    private val fonts = File("src/commonMain/composeResources/files/fonts").listFiles { f -> f.extension == "ttf" }.orEmpty().sortedBy { it.name }

    private fun codePoints(text: String): List<Int> {
        val out = ArrayList<Int>()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c.isHighSurrogate() && i + 1 < text.length) {
                out += ((c.code - 0xD800) shl 10) + (text[i + 1].code - 0xDC00) + 0x10000
                i += 2
            } else {
                out += c.code
                i++
            }
        }
        return out
    }

    @Test
    fun bundledFontsCoverTheSampleWithAwtWidths() {
        assertEquals(10, fonts.size, "bundled fonts: $fonts")
        val context = FontRenderContext(null, false, true)
        fonts.forEach { file ->
            val font = TrueTypeFont(file.readBytes())
            assertTrue(font.postScriptName.startsWith("Toolbox"), "${file.name}: ${font.postScriptName}")
            val awt = Font.createFont(Font.TRUETYPE_FONT, file).deriveFont(font.unitsPerEm.toFloat())
            codePoints(sample).forEach { cp ->
                val glyph = font.glyphId(cp)
                assertTrue(glyph != 0, "${file.name} has no glyph for U+${cp.toString(16)}")
                val advance = awt.createGlyphVector(context, String(Character.toChars(cp))).getGlyphMetrics(0).advance
                assertTrue(abs(advance - font.advanceWidth(glyph)) <= 1f, "${file.name} U+${cp.toString(16)}: $advance vs ${font.advanceWidth(glyph)}")
            }
            assertTrue(font.ascent > 0 && font.descent < 0 && font.capHeight > 0, file.name)
        }
    }

    @Test
    fun subsetRendersTheSampleLikeTheFullFont() {
        fonts.forEach { file ->
            val bytes = file.readBytes()
            val font = TrueTypeFont(bytes)
            val glyphs = codePoints(sample).map(font::glyphId).toSet()
            val subset = font.subset(glyphs)
            assertTrue(subset.size < bytes.size / 4, "${file.name}: subset of ${subset.size} bytes")
            val full = render(Font.createFont(Font.TRUETYPE_FONT, ByteArrayInputStream(bytes)))
            val small = render(Font.createFont(Font.TRUETYPE_FONT, ByteArrayInputStream(subset)))
            val differing = (0 until full.width * full.height).count { full.getRGB(it % full.width, it / full.width) != small.getRGB(it % full.width, it / full.width) }
            assertEquals(0, differing, "${file.name}: pixels differ")
        }
    }

    @Test
    fun brokenFontsFailWithFontFormatException() {
        val bytes = fonts.first().readBytes()
        listOf(8, 12, 100, 1000, bytes.size / 2).forEach { size ->
            assertFailsWith<FontFormatException>("cut at $size") { TrueTypeFont(bytes.copyOf(size)).subset(setOf(1, 2, 3)) }
        }
        assertFailsWith<FontFormatException> { TrueTypeFont("OTTO".encodeToByteArray() + ByteArray(64)) }
    }

    private fun render(font: Font): BufferedImage {
        val image = BufferedImage(1400, 60, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.color = Color.WHITE
        g.fillRect(0, 0, image.width, image.height)
        g.color = Color.BLACK
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.font = font.deriveFont(18f)
        g.drawString(sample, 4, 40)
        g.dispose()
        return image
    }
}
