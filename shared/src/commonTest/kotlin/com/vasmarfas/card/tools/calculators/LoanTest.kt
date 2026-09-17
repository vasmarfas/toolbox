package com.vasmarfas.card.tools.calculators

import kotlin.test.Test
import kotlin.test.assertEquals

class LoanTest {
    @Test
    fun annuity() {
        assertEquals(8884.88, Loan.annuityPayment(100000.0, 12.0, 12), 0.01)
        val result = Loan.schedule(100000.0, 12.0, 12, LoanType.ANNUITY)
        assertEquals(12, result.schedule.size)
        assertEquals(0.0, result.schedule.last().balance, 1e-6)
        assertEquals(100000.0, result.schedule.sumOf { it.principal }, 1e-6)
        assertEquals(6618.55, result.interest, 0.01)
    }

    @Test
    fun differentiated() {
        val result = Loan.schedule(120000.0, 12.0, 12, LoanType.DIFFERENTIATED)
        assertEquals(11200.0, result.schedule.first().payment, 1e-6)
        assertEquals(10100.0, result.schedule.last().payment, 1e-6)
        assertEquals(0.0, result.schedule.last().balance, 1e-6)
    }

    @Test
    fun zeroRate() {
        assertEquals(1000.0, Loan.annuityPayment(12000.0, 0.0, 12), 1e-9)
    }
}
