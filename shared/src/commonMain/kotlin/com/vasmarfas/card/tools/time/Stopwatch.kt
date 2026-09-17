package com.vasmarfas.card.tools.time

data class Lap(val index: Int, val lap: Long, val total: Long)

fun lapSplits(totals: List<Long>): List<Lap> =
    totals.mapIndexed { i, total -> Lap(i + 1, total - (if (i == 0) 0L else totals[i - 1]), total) }
