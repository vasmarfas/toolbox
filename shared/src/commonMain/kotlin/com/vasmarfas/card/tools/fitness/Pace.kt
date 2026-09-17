package com.vasmarfas.card.tools.fitness

import com.vasmarfas.card.resources.*
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.StringResource

const val KM_PER_MILE = 1.609344

class RaceDistance(val title: StringResource, val km: Double)

val raceDistances = listOf(
    RaceDistance(Res.string.s_1_km, 1.0),
    RaceDistance(Res.string.s_1_mile, KM_PER_MILE),
    RaceDistance(Res.string.s_5k, 5.0),
    RaceDistance(Res.string.s_10k, 10.0),
    RaceDistance(Res.string.half_marathon, 21.0975),
    RaceDistance(Res.string.marathon, 42.195),
)

object Pace {
    fun parseTime(text: String): Double? {
        val parts = text.trim().split(':', '.', ',', ' ').filter { it.isNotEmpty() }
        if (parts.isEmpty() || parts.size > 3) return null
        val numbers = parts.map { it.toIntOrNull() ?: return null }
        if (numbers.any { it < 0 }) return null
        return when (numbers.size) {
            1 -> numbers[0].toDouble()
            2 -> numbers[0] * 60.0 + numbers[1]
            else -> numbers[0] * 3600.0 + numbers[1] * 60.0 + numbers[2]
        }
    }

    fun formatTime(seconds: Double): String {
        val total = seconds.roundToInt().coerceAtLeast(0)
        val h = total / 3600
        val m = total % 3600 / 60
        val s = total % 60
        val mm = m.toString().padStart(2, '0')
        val ss = s.toString().padStart(2, '0')
        return if (h > 0) "$h:$mm:$ss" else "$mm:$ss"
    }

    fun formatPace(secondsPerUnit: Double): String {
        val total = secondsPerUnit.roundToInt().coerceAtLeast(0)
        return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
    }

    fun paceSeconds(totalSeconds: Double, distance: Double): Double = totalSeconds / distance

    fun speed(distance: Double, totalSeconds: Double): Double = distance / (totalSeconds / 3600.0)

    fun riegel(knownSeconds: Double, knownDistance: Double, targetDistance: Double): Double =
        knownSeconds * (targetDistance / knownDistance).pow(1.06)

    fun splits(distanceKm: Double, paceSecondsPerKm: Double, unitKm: Double, limit: Int = 60): List<Triple<Int, Double, Double>> {
        val unitsTotal = distanceKm / unitKm
        val whole = floor(unitsTotal).toInt().coerceAtMost(limit)
        val pacePerUnit = paceSecondsPerKm * unitKm
        val rows = (1..whole).map { Triple(it, pacePerUnit, it * pacePerUnit) }
        val tail = unitsTotal - floor(unitsTotal)
        return if (tail > 0.005 && whole < limit) {
            rows + Triple(whole + 1, pacePerUnit * tail, whole * pacePerUnit + pacePerUnit * tail)
        } else {
            rows
        }
    }
}
