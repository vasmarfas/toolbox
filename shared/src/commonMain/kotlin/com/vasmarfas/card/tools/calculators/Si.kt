package com.vasmarfas.card.tools.calculators

import com.vasmarfas.card.tools.converters.fmtSig
import kotlin.math.abs

object Si {
    private val prefixes = mapOf(
        'p' to 1e-12, 'n' to 1e-9, 'u' to 1e-6, 'µ' to 1e-6, 'μ' to 1e-6, 'm' to 1e-3,
        'k' to 1e3, 'K' to 1e3, 'M' to 1e6, 'G' to 1e9, 'T' to 1e12, 'R' to 1.0,
    )
    private val pattern = Regex("^([-+]?(?:\\d+\\.?\\d*|\\.\\d+))([pnuµμmkKMGTR])?(\\d*)(?:Ω|ohm|Ohm|OHM|V|v|A|a|W|w|F|H|Hz)?$")

    fun parse(text: String): Double? {
        val s = text.trim().replace(" ", "").replace(',', '.')
        if (s.isEmpty()) return null
        s.toDoubleOrNull()?.let { return it }
        val match = pattern.find(s) ?: return null
        val whole = match.groupValues[1]
        val prefix = match.groupValues[2]
        val tail = match.groupValues[3]
        val number = if (tail.isEmpty()) {
            whole
        } else {
            if (whole.contains('.')) return null
            "$whole.$tail"
        }
        val value = number.toDoubleOrNull() ?: return null
        val factor = if (prefix.isEmpty()) 1.0 else prefixes.getValue(prefix[0])
        return value * factor
    }

    fun format(value: Double, unit: String, significant: Int = 4): String {
        if (value.isNaN() || value.isInfinite()) return "${value.fmtSig()} $unit"
        val a = abs(value)
        if (a == 0.0) return "0 $unit"
        val (factor, prefix) = when {
            a >= 1e12 -> 1e12 to "T"
            a >= 1e9 -> 1e9 to "G"
            a >= 1e6 -> 1e6 to "M"
            a >= 1e3 -> 1e3 to "k"
            a >= 1 -> 1.0 to ""
            a >= 1e-3 -> 1e-3 to "m"
            a >= 1e-6 -> 1e-6 to "µ"
            a >= 1e-9 -> 1e-9 to "n"
            else -> 1e-12 to "p"
        }
        return "${(value / factor).fmtSig(significant)} $prefix$unit"
    }
}
