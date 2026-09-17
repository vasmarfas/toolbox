package com.vasmarfas.card.tools.converters

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RomanTest {
    @Test
    fun bothDirections() {
        assertEquals("MCMXCIV", Roman.toRoman(1994))
        assertEquals("MMXXVI", Roman.toRoman(2026))
        assertEquals(2026, Roman.toArabic("mmxxvi"))
        assertEquals(1994, Roman.toArabic("MCMXCIV"))
        assertEquals(3999, Roman.toArabic("MMMCMXCIX"))
    }

    @Test
    fun invalid() {
        assertNull(Roman.toRoman(0))
        assertNull(Roman.toRoman(4000))
        assertNull(Roman.toArabic("IIII"))
        assertNull(Roman.toArabic(""))
        assertNull(Roman.toArabic("ABC"))
    }
}
