package com.vasmarfas.card.tools.time

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

data class SolarTimes(
    val solarNoon: Double,
    val sunrise: Double?,
    val sunset: Double?,
    val civilDawn: Double?,
    val civilDusk: Double?,
    val nauticalDawn: Double?,
    val nauticalDusk: Double?,
    val astronomicalDawn: Double?,
    val astronomicalDusk: Double?,
    val polarDay: Boolean,
    val polarNight: Boolean,
) {
    val dayLengthMinutes: Double? get() = if (sunrise != null && sunset != null) sunset - sunrise else null
}

object SolarMath {
    private const val OFFICIAL = 90.833
    private const val CIVIL = 96.0
    private const val NAUTICAL = 102.0
    private const val ASTRONOMICAL = 108.0

    private class Pass(val noon: Double, val cosHa: Double, val rise: Double?, val set: Double?)

    private fun rad(deg: Double) = deg * PI / 180

    private fun deg(rad: Double) = rad * 180 / PI

    private fun pass(days: Int, minutesUtc: Double, lat: Double, lon: Double, zenith: Double): Pass {
        val jd = 2440587.5 + days + minutesUtc / 1440.0
        val jc = (jd - 2451545.0) / 36525.0
        val l0 = (280.46646 + jc * (36000.76983 + jc * 0.0003032)).mod(360.0)
        val m = 357.52911 + jc * (35999.05029 - 0.0001537 * jc)
        val e = 0.016708634 - jc * (0.000042037 + 0.0000001267 * jc)
        val c = sin(rad(m)) * (1.914602 - jc * (0.004817 + 0.000014 * jc)) +
            sin(rad(2 * m)) * (0.019993 - 0.000101 * jc) +
            sin(rad(3 * m)) * 0.000289
        val omega = 125.04 - 1934.136 * jc
        val appLong = l0 + c - 0.00569 - 0.00478 * sin(rad(omega))
        val obliq0 = 23 + (26 + (21.448 - jc * (46.815 + jc * (0.00059 - jc * 0.001813))) / 60) / 60
        val obliq = obliq0 + 0.00256 * cos(rad(omega))
        val decl = deg(asin(sin(rad(obliq)) * sin(rad(appLong))))
        val y = tan(rad(obliq / 2)) * tan(rad(obliq / 2))
        val eot = 4 * deg(
            y * sin(2 * rad(l0)) - 2 * e * sin(rad(m)) + 4 * e * y * sin(rad(m)) * cos(2 * rad(l0)) -
                0.5 * y * y * sin(4 * rad(l0)) - 1.25 * e * e * sin(2 * rad(m)),
        )
        val noon = 720 - 4 * lon - eot
        val cosHa = cos(rad(zenith)) / (cos(rad(lat)) * cos(rad(decl))) - tan(rad(lat)) * tan(rad(decl))
        if (cosHa < -1 || cosHa > 1) return Pass(noon, cosHa, null, null)
        val ha = deg(acos(cosHa))
        return Pass(noon, cosHa, noon - 4 * ha, noon + 4 * ha)
    }

    private fun events(days: Int, lat: Double, lon: Double, zenith: Double): Pair<Double?, Double?> {
        val first = pass(days, 720.0, lat, lon, zenith)
        if (first.rise == null || first.set == null) return null to null
        return pass(days, first.rise, lat, lon, zenith).rise to pass(days, first.set, lat, lon, zenith).set
    }

    fun compute(date: LocalDate, lat: Double, lon: Double): SolarTimes {
        val days = LocalDate(1970, 1, 1).daysUntil(date)
        val noonPass = pass(days, 720.0, lat, lon, OFFICIAL)
        val official = events(days, lat, lon, OFFICIAL)
        val civil = events(days, lat, lon, CIVIL)
        val nautical = events(days, lat, lon, NAUTICAL)
        val astronomical = events(days, lat, lon, ASTRONOMICAL)
        return SolarTimes(
            solarNoon = noonPass.noon,
            sunrise = official.first,
            sunset = official.second,
            civilDawn = civil.first,
            civilDusk = civil.second,
            nauticalDawn = nautical.first,
            nauticalDusk = nautical.second,
            astronomicalDawn = astronomical.first,
            astronomicalDusk = astronomical.second,
            polarDay = noonPass.cosHa < -1,
            polarNight = noonPass.cosHa > 1,
        )
    }

    fun formatMinutes(minutesUtc: Double, offsetSeconds: Int): String {
        val total = floor((minutesUtc + offsetSeconds / 60.0).mod(1440.0)).toInt()
        return "${(total / 60).pad2()}:${(total % 60).pad2()}"
    }

    fun durationParts(minutes: Double): Pair<Int, Int> {
        val total = floor(minutes + 0.5).toInt()
        return total / 60 to total % 60
    }
}
