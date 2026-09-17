package com.vasmarfas.card.tools.converters

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BaseConvertTest {
    @Test
    fun smallNumbers() {
        assertEquals("FF", BaseConvert.convert("255", 10, 16))
        assertEquals("11111111", BaseConvert.convert("ff", 16, 2))
        assertEquals("255", BaseConvert.convert("0xff", 16, 10))
        assertEquals("-10", BaseConvert.convert("-1010", 2, 10))
        assertEquals("0", BaseConvert.convert("000", 10, 2))
        assertEquals("1295", BaseConvert.convert("zz", 36, 10))
    }

    @Test
    fun arbitraryLength() {
        assertEquals("18EE90FF6C373E0EE4E3F0AD2", BaseConvert.convert("123456789012345678901234567890", 10, 16))
        assertEquals("123456789012345678901234567890", BaseConvert.convert("18EE90FF6C373E0EE4E3F0AD2", 16, 10))
    }

    @Test
    fun invalidDigits() {
        assertNull(BaseConvert.convert("12", 2, 10))
        assertNull(BaseConvert.convert("", 10, 2))
        assertNull(BaseConvert.convert("-", 10, 2))
    }
}
