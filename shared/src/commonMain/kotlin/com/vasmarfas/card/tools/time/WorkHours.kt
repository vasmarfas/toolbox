package com.vasmarfas.card.tools.time

data class Shift(val start: String, val end: String, val breakMinutes: String) {
    val minutes: Int?
        get() {
            val s = parseHhMm(start) ?: return null
            val e = parseHhMm(end) ?: return null
            val b = breakMinutes.trim().ifEmpty { "0" }.toIntOrNull()?.takeIf { it >= 0 } ?: return null
            return WorkHours.shiftMinutes(s, e, b)
        }
}

object WorkHours {
    fun shiftMinutes(start: Int, end: Int, breakMinutes: Int): Int {
        val span = if (end >= start) end - start else end + 1440 - start
        return maxOf(0, span - breakMinutes)
    }

    fun decimalHours(minutes: Int): Double = minutes / 60.0
}
