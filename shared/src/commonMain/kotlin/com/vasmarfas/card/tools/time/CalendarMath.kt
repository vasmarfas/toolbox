package com.vasmarfas.card.tools.time

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

object CalendarMath {
    fun monthGrid(year: Int, month: Int): List<List<LocalDate>> {
        val first = LocalDate(year, month, 1)
        val start = first.minus(first.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
        val last = LocalDate(year, month, DateMath.daysInMonth(year, month))
        val weeks = start.daysUntil(last) / 7 + 1
        return (0 until weeks).map { w -> (0 until 7).map { d -> start.plus(w * 7 + d, DateTimeUnit.DAY) } }
    }

    fun shiftMonth(year: Int, month: Int, delta: Int): Pair<Int, Int> {
        val index = year * 12 + (month - 1) + delta
        return index.floorDiv(12) to index.mod(12) + 1
    }
}
