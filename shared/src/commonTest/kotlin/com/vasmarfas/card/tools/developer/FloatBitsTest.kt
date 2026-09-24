package com.vasmarfas.card.tools.developer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FloatBitsTest {
    @Test
    fun singlePrecisionLayoutOfOneTenth() {
        val layout = FloatBits.of(0.1, FloatFormat.SINGLE)
        assertEquals("3DCCCCCD", layout.hex)
        assertEquals(0, layout.sign)
        assertEquals(123, layout.exponentField)
        assertEquals(-4, layout.exponent)
        assertEquals("00111101110011001100110011001101", layout.binary)
        assertEquals(FloatClass.NORMAL, layout.kind)
    }

    @Test
    fun doublePrecisionLayout() {
        val layout = FloatBits.of(-2.0, FloatFormat.DOUBLE)
        assertEquals("C000000000000000", layout.hex)
        assertEquals(1, layout.sign)
        assertEquals(1, layout.exponent)
    }

    @Test
    fun hexInputRoundTrips() {
        val layout = assertNotNull(FloatBits.fromHex("0x3F800000", FloatFormat.SINGLE))
        assertEquals(1.0, layout.value)
        assertEquals(FloatClass.INFINITE, FloatBits.fromHex("7F800000", FloatFormat.SINGLE)?.kind)
        assertEquals(FloatClass.NAN, FloatBits.fromHex("7FC00000", FloatFormat.SINGLE)?.kind)
        assertEquals(FloatClass.SUBNORMAL, FloatBits.fromHex("00000001", FloatFormat.SINGLE)?.kind)
        assertNull(FloatBits.fromHex("0x123456789", FloatFormat.SINGLE))
        assertNull(FloatBits.fromHex("xyz", FloatFormat.DOUBLE))
    }

    @Test
    fun exactDecimalShowsEveryDigit() {
        assertEquals("0.1000000000000000055511151231257827021181583404541015625", FloatBits.exactDecimal(0.1))
        assertEquals("0.100000001490116119384765625", FloatBits.exactDecimal(0.1f.toDouble()))
        assertEquals("1024", FloatBits.exactDecimal(1024.0))
        assertEquals("-2.5", FloatBits.exactDecimal(-2.5))
        assertEquals("0", FloatBits.exactDecimal(0.0))
        assertEquals(1076, FloatBits.exactDecimal(Double.MIN_VALUE).length)
    }

    @Test
    fun neighboursAreOneStepAway() {
        val one = FloatBits.of(1.0, FloatFormat.SINGLE)
        assertEquals(1.0 + 1.1920928955078125e-7, FloatBits.nextUp(one))
        assertEquals(1.0 - 5.9604644775390625e-8, FloatBits.nextDown(one))
    }
}
