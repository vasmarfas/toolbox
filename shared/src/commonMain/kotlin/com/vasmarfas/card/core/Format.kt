package com.vasmarfas.card.core

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

fun Double.fmt(maxFraction: Int = 2, minFraction: Int = 0, grouping: Boolean = false): String {
    if (isNaN()) return "NaN"
    if (isInfinite()) return if (this > 0) "∞" else "-∞"
    val factor = 10.0.pow(maxFraction)
    val rounded = (abs(this) * factor).roundToLong()
    var intPart = (rounded / factor.toLong()).toString()
    var frac = (rounded % factor.toLong()).toString().padStart(maxFraction, '0')
    frac = frac.trimEnd('0')
    if (frac.length < minFraction) frac = frac.padEnd(minFraction, '0')
    if (grouping) intPart = groupThousands(intPart)
    val sign = if (this < 0 && rounded != 0L) "-" else ""
    return if (frac.isEmpty()) "$sign$intPart" else "$sign$intPart.$frac"
}

fun Long.fmtGrouped(): String {
    val s = abs(this).toString()
    return (if (this < 0) "-" else "") + groupThousands(s)
}

fun Int.fmtGrouped(): String = toLong().fmtGrouped()

private fun groupThousands(digits: String): String {
    val sb = StringBuilder()
    digits.forEachIndexed { i, c ->
        if (i > 0 && (digits.length - i) % 3 == 0) sb.append(' ')
        sb.append(c)
    }
    return sb.toString()
}

fun formatBytes(bytes: Long, binary: Boolean = true): String {
    val unit = if (binary) 1024.0 else 1000.0
    if (bytes < unit) return "$bytes B"
    val units = if (binary) listOf("KiB", "MiB", "GiB", "TiB", "PiB") else listOf("kB", "MB", "GB", "TB", "PB")
    var value = bytes.toDouble()
    var idx = -1
    while (value >= unit && idx < units.lastIndex) {
        value /= unit
        idx++
    }
    return "${value.fmt(2)} ${units[idx]}"
}

fun formatDurationMs(ms: Long): String {
    if (ms < 1000) return "$ms ms"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return buildString {
        if (h > 0) append(h).append("h ")
        if (h > 0 || m > 0) append(m).append("m ")
        append(s).append("s")
    }
}

fun String.toDoubleLenient(): Double? = trim().replace(',', '.').replace(" ", "").toDoubleOrNull()
