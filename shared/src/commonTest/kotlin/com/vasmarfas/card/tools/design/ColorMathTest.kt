package com.vasmarfas.card.tools.design

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ColorMathTest {
    private val orange = Rgba(255, 136, 0)

    @Test
    fun parsesAllForms() {
        assertEquals(orange, ColorMath.parse("#FF8800"))
        assertEquals(orange, ColorMath.parse("ff8800"))
        assertEquals(Rgba(255, 136, 0, 128), ColorMath.parse("#FF880080"))
        assertEquals(Rgba(17, 34, 51), ColorMath.parse("#123"))
        assertEquals(Rgba(17, 34, 51, 0x80), ColorMath.parse("0x80112233"))
        assertEquals(orange, ColorMath.parse("rgb(255, 136, 0)"))
        assertEquals(Rgba(255, 136, 0, 128), ColorMath.parse("rgba(255 136 0 / 50%)"))
        assertEquals(orange, ColorMath.parse("hsl(32, 100%, 50%)"))
        assertEquals(orange, ColorMath.parse("hsv(32, 100%, 100%)"))
        assertEquals(orange, ColorMath.parse("cmyk(0%, 46.7%, 100%, 0%)"))
        assertEquals(Rgba(102, 51, 153), ColorMath.parse("RebeccaPurple"))
        assertNull(ColorMath.parse("#GG0000"))
        assertNull(ColorMath.parse("rgb(300, 0, 0)"))
        assertEquals(148, CssColors.entries.size)
    }

    @Test
    fun roundTrips() {
        val samples = listOf(Rgba(12, 200, 99), Rgba(0, 0, 0), Rgba(255, 255, 255), Rgba(128, 128, 128), orange, Rgba(7, 8, 250))
        samples.forEach { c ->
            assertEquals(c, ColorMath.fromHsl(ColorMath.toHsl(c)))
            assertEquals(c, ColorMath.fromHsv(ColorMath.toHsv(c)))
            assertEquals(c, ColorMath.fromCmyk(ColorMath.toCmyk(c)))
            assertEquals(c, ColorMath.parse(c.hex()))
        }
        val hsl = ColorMath.toHsl(orange)
        assertTrue(abs(hsl.h - 32) < 0.5)
        assertEquals(1.0, hsl.s)
        assertEquals(0.5, hsl.l)
    }

    @Test
    fun hsvSpansTheWheel() {
        (0..330 step 30).forEach { angle ->
            val hsv = ColorMath.toHsv(ColorMath.fromHsv(Hsv(angle.toDouble(), 1.0, 1.0)))
            assertTrue(abs(hsv.h - angle) < 0.5, "hue $angle")
            assertEquals(1.0, hsv.s)
            assertEquals(1.0, hsv.v)
        }
        assertEquals(ColorMath.fromHsv(Hsv(330.0, 1.0, 1.0)), ColorMath.fromHsv(Hsv(-30.0, 1.0, 1.0)))
        assertEquals(ColorMath.fromHsv(Hsv(20.0, 0.6, 0.8)), ColorMath.fromHsv(Hsv(380.0, 0.6, 0.8)))
        assertEquals(Rgba(0, 0, 0, 128), ColorMath.fromHsv(Hsv(210.0, 0.8, 0.0), 128))
    }

    @Test
    fun formatsStrings() {
        assertEquals("#FF8800", orange.hex())
        assertEquals("#80FF8800", Rgba(255, 136, 0, 128).androidHex)
        assertEquals("Color(0xFFFF8800)", orange.composeLiteral)
        assertEquals("rgba(255, 136, 0, 0.5)", ColorMath.rgbString(Rgba(255, 136, 0, 128)))
        assertEquals("hsl(32, 100%, 50%)", ColorMath.hslString(orange))
        assertEquals("cmyk(0%, 47%, 100%, 0%)", ColorMath.cmykString(orange))
        assertEquals("rebeccapurple" to true, ColorMath.nearestCssName(Rgba(102, 51, 153)))
        assertEquals("darkorange" to false, ColorMath.nearestCssName(orange))
    }
}
