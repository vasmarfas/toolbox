package com.vasmarfas.card.tools.money

import com.vasmarfas.card.resources.*
import org.jetbrains.compose.resources.StringResource

enum class Compounding(val perYear: Int, val title: StringResource) {
    YEARLY(1, Res.string.yearly),
    QUARTERLY(4, Res.string.quarterly),
    MONTHLY(12, Res.string.monthly),
    DAILY(365, Res.string.daily),
}

class YearRow(val year: Int, val contributed: Double, val interest: Double, val balance: Double)

object CompoundInterest {
    fun grow(principal: Double, annualRate: Double, years: Int, compounding: Compounding, monthlyContribution: Double): List<YearRow> {
        val periods = compounding.perYear
        val rate = annualRate / 100 / periods
        val contributionPerPeriod = monthlyContribution * 12 / periods
        var balance = principal
        var contributed = principal
        var interest = 0.0
        val rows = ArrayList<YearRow>(years)
        for (year in 1..years) {
            repeat(periods) {
                balance += contributionPerPeriod
                contributed += contributionPerPeriod
                val gained = balance * rate
                balance += gained
                interest += gained
            }
            rows.add(YearRow(year, contributed, interest, balance))
        }
        return rows
    }
}
