package com.vasmarfas.card.tools.calculators

import kotlin.math.PI

class TireSize(val widthMm: Double, val profilePercent: Double, val rimInches: Double) {
    val sidewallMm: Double get() = widthMm * profilePercent / 100
    val diameterMm: Double get() = rimInches * Tires.MM_PER_INCH + 2 * sidewallMm
    val circumferenceMm: Double get() = diameterMm * PI
    val revolutionsPerKm: Double get() = 1_000_000 / circumferenceMm

    override fun toString(): String = "${widthMm.toInt()}/${profilePercent.toInt()} R${rimInches.toInt()}"
}

object Tires {
    const val MM_PER_INCH = 25.4

    const val USUAL_TOLERANCE_PERCENT = 3.0

    fun diameterChangePercent(current: TireSize, other: TireSize): Double = (other.diameterMm / current.diameterMm - 1) * 100

    fun actualSpeed(current: TireSize, other: TireSize, shownKmh: Double): Double = shownKmh * other.diameterMm / current.diameterMm

    fun clearanceChangeMm(current: TireSize, other: TireSize): Double = (other.diameterMm - current.diameterMm) / 2
}
