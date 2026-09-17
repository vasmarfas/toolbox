package com.vasmarfas.card.tools.calculators

import kotlin.math.abs
import kotlin.math.sqrt

class DensityPreset(val name: String, val density: Double)

object Ratios {
    val common = listOf(
        "1:1" to 1.0,
        "5:4" to 1.25,
        "4:3" to 4.0 / 3,
        "3:2" to 1.5,
        "16:10" to 1.6,
        "16:9" to 16.0 / 9,
        "1.85:1" to 1.85,
        "2:1" to 2.0,
        "21:9" to 21.0 / 9,
        "2.35:1" to 2.35,
        "2.39:1" to 2.39,
        "4:5" to 0.8,
        "3:4" to 0.75,
        "2:3" to 2.0 / 3,
        "10:16" to 0.625,
        "9:16" to 9.0 / 16,
        "9:19.5" to 9.0 / 19.5,
        "9:20" to 0.45,
        "9:21" to 9.0 / 21,
    )

    val densities = listOf(
        DensityPreset("ldpi", 0.75),
        DensityPreset("mdpi", 1.0),
        DensityPreset("hdpi", 1.5),
        DensityPreset("xhdpi", 2.0),
        DensityPreset("xxhdpi", 3.0),
        DensityPreset("xxxhdpi", 4.0),
    )

    fun gcd(a: Int, b: Int): Int {
        var x = abs(a)
        var y = abs(b)
        while (y != 0) {
            val t = x % y
            x = y
            y = t
        }
        return x
    }

    fun simplify(width: Int, height: Int): Pair<Int, Int> {
        val d = gcd(width, height)
        return if (d == 0) width to height else width / d to height / d
    }

    fun closest(ratio: Double): String? {
        if (ratio <= 0) return null
        val best = common.minBy { abs(it.second - ratio) / ratio }
        return best.first.takeIf { abs(best.second - ratio) / ratio < 0.015 }
    }

    fun pxToDp(px: Double, density: Double): Double = px / density

    fun dpToPx(dp: Double, density: Double): Double = dp * density

    fun spToPx(sp: Double, density: Double, fontScale: Double): Double = sp * density * fontScale

    fun pxToSp(px: Double, density: Double, fontScale: Double): Double = px / density / fontScale

    fun ppi(width: Int, height: Int, diagonalInches: Double): Double =
        sqrt(width.toDouble() * width + height.toDouble() * height) / diagonalInches

    fun bucket(density: Double): String = densities.minBy { abs(it.density - density) }.name
}
