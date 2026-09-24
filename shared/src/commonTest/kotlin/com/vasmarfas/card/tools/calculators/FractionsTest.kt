package com.vasmarfas.card.tools.calculators

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class FractionsTest {
    private fun f(n: Long, d: Long) = Fraction(n, d)

    @Test
    fun arithmetic() {
        assertEquals(f(5, 6), Fractions.solve(f(1, 2), FractionOp.ADD, f(1, 3)).value)
        assertEquals(f(1, 6), Fractions.solve(f(1, 2), FractionOp.SUBTRACT, f(1, 3)).value)
        assertEquals(f(1, 2), Fractions.solve(f(2, 3), FractionOp.MULTIPLY, f(3, 4)).value)
        assertEquals(f(2, 3), Fractions.solve(f(1, 2), FractionOp.DIVIDE, f(3, 4)).value)
        assertEquals(f(-2, 3), Fractions.solve(f(1, 2), FractionOp.DIVIDE, f(-3, 4)).value)
        assertEquals(f(1, 1), Fractions.solve(f(1, 2), FractionOp.ADD, f(1, 2)).value)
    }

    @Test
    fun stepsFollowTheSchoolOrder() {
        val sum = Fractions.solve(f(3, 4), FractionOp.ADD, f(5, 6))
        val common = assertIs<FractionStep.CommonDenominator>(sum.steps[0])
        assertEquals(12, common.lcm)
        assertEquals(f(9, 12), common.leftScaled)
        assertEquals(f(10, 12), common.rightScaled)
        assertEquals(f(19, 12), assertIs<FractionStep.Combine>(sum.steps[1]).result)
        assertIs<FractionStep.WholePart>(sum.steps[2])
        assertEquals("1 7/12", sum.value?.mixed())

        val product = Fractions.solve(f(2, 3), FractionOp.MULTIPLY, f(3, 4))
        assertEquals(f(6, 12), assertIs<FractionStep.Multiply>(product.steps[0]).result)
        assertEquals(6, assertIs<FractionStep.Reduce>(product.steps[1]).gcd)

        val mixed = Fractions.solve(f(7, 3), FractionOp.ADD, f(1, 3), leftMixed = "2 1/3")
        assertIs<FractionStep.ToImproper>(mixed.steps[0])
        assertEquals(f(8, 3), mixed.value)
    }

    @Test
    fun comparison() {
        val result = Fractions.solve(f(3, 4), FractionOp.COMPARE, f(2, 3))
        assertNull(result.value)
        assertEquals(1, result.comparison)
        assertEquals(0, Fractions.solve(f(2, 4), FractionOp.COMPARE, f(1, 2)).comparison)
        assertEquals(-1, Fractions.solve(f(-1, 2), FractionOp.COMPARE, f(1, 3)).comparison)
    }

    @Test
    fun mixedNumbers() {
        assertEquals(f(7, 3), Fractions.mixed(2, 1, 3))
        assertEquals(f(-7, 3), Fractions.mixed(-2, 1, 3))
        assertEquals(f(-1, 3), Fractions.mixed(0, 1, -3))
        assertNull(Fractions.mixed(1, 1, 0))
        assertEquals("-2 1/3", f(-7, 3).mixed())
        assertEquals("-1/3", f(-1, 3).mixed())
        assertEquals("4", f(8, 2).mixed())
    }

    @Test
    fun decimals() {
        assertEquals("0.375", Fractions.expand(f(3, 8)).toString())
        assertEquals("0.(3)", Fractions.expand(f(1, 3)).toString())
        assertEquals("0.1(6)", Fractions.expand(f(1, 6)).toString())
        assertEquals("0.(142857)", Fractions.expand(f(1, 7)).toString())
        assertEquals("-2.5", Fractions.expand(f(-5, 2)).toString())
        assertEquals("3", Fractions.expand(f(3, 1)).toString())

        assertEquals(f(3, 8), Fractions.parseDecimal("0,375")?.reduced())
        assertEquals(f(1, 3), Fractions.parseDecimal("0.(3)")?.reduced())
        assertEquals(f(611, 495), Fractions.parseDecimal("1.2(34)")?.reduced())
        assertEquals(f(-5, 4), Fractions.parseDecimal("-1.25")?.reduced())
        assertEquals(f(1, 2), Fractions.parseDecimal(".5")?.reduced())
        assertNull(Fractions.parseDecimal("abc"))
        assertNull(Fractions.parseDecimal("-"))
        assertFailsWith<ArithmeticException> { Fractions.parseDecimal("0.123456789012345678") }

        val solution = Fractions.fromDecimal("0.375")
        assertEquals(f(3, 8), solution?.value)
        assertEquals(f(375, 1000), assertIs<FractionStep.FromDecimal>(solution?.steps?.first()).fraction)
    }

    @Test
    fun approximation() {
        assertEquals(f(22, 7), Fractions.approximate(f(314159, 100000), 10))
        assertEquals(f(311, 99), Fractions.approximate(f(314159, 100000), 100))
        assertEquals(f(1, 3), Fractions.approximate(f(333, 1000), 100))
        assertEquals(f(-1, 3), Fractions.approximate(f(-333, 1000), 10))
        assertEquals(f(3, 8), Fractions.approximate(f(3, 8), 100))
    }

    @Test
    fun overflowIsReported() {
        assertFailsWith<ArithmeticException> {
            Fractions.solve(f(Long.MAX_VALUE / 2, 3), FractionOp.MULTIPLY, f(Long.MAX_VALUE / 2, 5))
        }
    }
}
