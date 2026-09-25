package com.vasmarfas.card.tools.electronics

import kotlin.math.abs
import kotlin.math.pow

class DividerPair(val r1: Double, val r2: Double, val vout: Double)

object VoltageDivider {
    fun vout(vin: Double, r1: Double, r2: Double): Double = vin * r2 / (r1 + r2)

    fun r1(vin: Double, vout: Double, r2: Double): Double = r2 * (vin - vout) / vout

    fun r2(vin: Double, vout: Double, r1: Double): Double = r1 * vout / (vin - vout)

    fun current(vin: Double, r1: Double, r2: Double): Double = vin / (r1 + r2)

    fun pairs(vin: Double, vout: Double, series: List<Double>, count: Int = 5): List<DividerPair> =
        (2..5).flatMap { exp -> series.map { it * 10.0.pow(exp) } }
            .map { r2 -> LedResistor.nearestInSeries(r1(vin, vout, r2), series) to r2 }
            .filter { (r1, r2) -> r1 + r2 in 10e3..100e3 }
            .distinct()
            .map { (r1, r2) -> DividerPair(r1, r2, vout(vin, r1, r2)) }
            .sortedBy { abs(it.vout - vout) }
            .take(count)
}
