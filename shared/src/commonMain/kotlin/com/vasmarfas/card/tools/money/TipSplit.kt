package com.vasmarfas.card.tools.money

import kotlin.math.ceil

class TipResult(val tip: Double, val total: Double, val perPerson: Double, val tipPerPerson: Double)

object TipSplit {
    fun compute(bill: Double, tipPercent: Double, people: Int, roundUp: Boolean): TipResult {
        val n = people.coerceAtLeast(1)
        var total = bill * (1 + tipPercent / 100)
        var perPerson = total / n
        if (roundUp) {
            perPerson = ceil(perPerson)
            total = perPerson * n
        }
        val tip = total - bill
        return TipResult(tip, total, perPerson, tip / n)
    }
}
