package com.vasmarfas.card.tools.money

import kotlin.math.pow

enum class LoanType { ANNUITY, DIFFERENTIATED }
class LoanPayment(val month: Int, val payment: Double, val principal: Double, val interest: Double, val balance: Double)
class LoanResult(val schedule: List<LoanPayment>) {
    val total: Double = schedule.sumOf { it.payment }
    val interest: Double = schedule.sumOf { it.interest }
}
object Loan {
    fun annuityPayment(principal: Double, annualRate: Double, months: Int): Double {
        val r = annualRate / 100 / 12
        if (r == 0.0) return principal / months
        return principal * r / (1 - (1 + r).pow(-months))
    }
    fun schedule(principal: Double, annualRate: Double, months: Int, type: LoanType): LoanResult {
        val r = annualRate / 100 / 12
        val annuity = annuityPayment(principal, annualRate, months)
        val principalPart = principal / months
        var balance = principal
        val payments = ArrayList<LoanPayment>(months)
        for (month in 1..months) {
            val interest = balance * r
            val principalPaid = when {
                month == months -> balance
                type == LoanType.ANNUITY -> annuity - interest
                else -> principalPart
            }
            balance -= principalPaid
            payments.add(LoanPayment(month, principalPaid + interest, principalPaid, interest, balance.coerceAtLeast(0.0)))
        }
        return LoanResult(payments)
    }
}