package com.vasmarfas.card.tools.text

import kotlin.test.Test
import kotlin.test.assertEquals

class KeyboardLayoutTest {
    @Test
    fun convertsBothWays() {
        assertEquals("привет", KeyboardLayout.convert("ghbdtn", LayoutDirection.EN_TO_RU))
        assertEquals("Привет, мир!", KeyboardLayout.convert("Ghbdtn? vbh!", LayoutDirection.EN_TO_RU))
        assertEquals("hello", KeyboardLayout.convert("руддщ", LayoutDirection.RU_TO_EN))
        assertEquals("git commit -m", KeyboardLayout.convert("пше сщььше -ь", LayoutDirection.RU_TO_EN))
    }

    @Test
    fun mapsPunctuationAndTheDigitRow() {
        assertEquals("хъжэ", KeyboardLayout.convert("[];'", LayoutDirection.EN_TO_RU))
        assertEquals("бю.", KeyboardLayout.convert(",./", LayoutDirection.EN_TO_RU))
        assertEquals("ёЁ", KeyboardLayout.convert("`~", LayoutDirection.EN_TO_RU))
        assertEquals("\"№;:?/", KeyboardLayout.convert("@#\$^&|", LayoutDirection.EN_TO_RU))
        assertEquals("ХЪЖЭБЮ,", KeyboardLayout.convert("{}:\"<>?", LayoutDirection.EN_TO_RU))
        assertEquals("[];'", KeyboardLayout.convert("хъжэ", LayoutDirection.RU_TO_EN))
    }

    @Test
    fun detectsDirectionByScript() {
        assertEquals(LayoutDirection.EN_TO_RU, KeyboardLayout.resolve("ghbdtn", LayoutDirection.AUTO))
        assertEquals(LayoutDirection.RU_TO_EN, KeyboardLayout.resolve("руддщ", LayoutDirection.AUTO))
        assertEquals(LayoutDirection.EN_TO_RU, KeyboardLayout.resolve("", LayoutDirection.AUTO))
        assertEquals(LayoutDirection.RU_TO_EN, KeyboardLayout.resolve("мир hi", LayoutDirection.AUTO))
        assertEquals("привет", KeyboardLayout.convert("ghbdtn", LayoutDirection.AUTO))
        assertEquals("hello", KeyboardLayout.convert("руддщ", LayoutDirection.AUTO))
        assertEquals(3, KeyboardLayout.cyrillicCount("мир hi"))
        assertEquals(2, KeyboardLayout.latinCount("мир hi"))
    }

    @Test
    fun roundTripIsLossless() {
        val latin = KeyboardLayout.enToRu.keys.joinToString("")
        assertEquals(latin, KeyboardLayout.convert(KeyboardLayout.convert(latin, LayoutDirection.EN_TO_RU), LayoutDirection.RU_TO_EN))
        val cyrillic = KeyboardLayout.ruToEn.keys.joinToString("")
        assertEquals(cyrillic, KeyboardLayout.convert(KeyboardLayout.convert(cyrillic, LayoutDirection.RU_TO_EN), LayoutDirection.EN_TO_RU))
        assertEquals(KeyboardLayout.enToRu.size, KeyboardLayout.ruToEn.size)
        assertEquals("1 2 3 —", KeyboardLayout.convert("1 2 3 —", LayoutDirection.EN_TO_RU))
    }
}
