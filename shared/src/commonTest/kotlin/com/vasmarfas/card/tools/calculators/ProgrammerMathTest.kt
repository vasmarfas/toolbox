package com.vasmarfas.card.tools.calculators

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProgrammerMathTest {
    @Test
    fun parseAndFormat() {
        assertEquals(255L, ProgrammerMath.parse("0xFF", NumBase.HEX, 8))
        assertEquals(-1L, ProgrammerMath.signed(255L, 8))
        assertEquals("11111111", ProgrammerMath.format(-1L, NumBase.BIN, 8))
        assertEquals("FFFFFFFF", ProgrammerMath.format(-1L, NumBase.HEX, 32))
        assertEquals("-128", ProgrammerMath.format(128L, NumBase.DEC, 8))
        assertEquals(128L, ProgrammerMath.parse("-128", NumBase.DEC, 8))
        assertNull(ProgrammerMath.parse("256", NumBase.DEC, 8))
        assertNull(ProgrammerMath.parse("12", NumBase.BIN, 8))
        assertEquals("1111 1111", ProgrammerMath.grouped("11111111", 4))
    }

    @Test
    fun operations() {
        assertEquals(0b1000L, ProgrammerMath.apply(BitOp.AND, 0b1100L, 0b1010L, 8))
        assertEquals(0b0110L, ProgrammerMath.apply(BitOp.XOR, 0b1100L, 0b1010L, 8))
        assertEquals(0xF0L, ProgrammerMath.apply(BitOp.NOT, 0x0FL, 0L, 8))
        assertEquals(0x80L, ProgrammerMath.apply(BitOp.SHL, 1L, 7L, 8))
        assertEquals(0x7FL, ProgrammerMath.apply(BitOp.SHR, 0xFFL, 1L, 8))
        assertEquals(0xFFL, ProgrammerMath.apply(BitOp.SAR, 0xFFL, 1L, 8))
        assertEquals(0x03L, ProgrammerMath.apply(BitOp.ROL, 0x81L, 1L, 8))
        assertEquals(0L, ProgrammerMath.apply(BitOp.ADD, 0xFFL, 1L, 8))
        assertEquals(0xFFL, ProgrammerMath.apply(BitOp.SUB, 0L, 1L, 8))
        assertNull(ProgrammerMath.apply(BitOp.DIV, 5L, 0L, 8))
    }
}
