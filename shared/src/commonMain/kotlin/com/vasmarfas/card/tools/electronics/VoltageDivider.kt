package com.vasmarfas.card.tools.electronics

object VoltageDivider {
    fun vout(vin: Double, r1: Double, r2: Double): Double = vin * r2 / (r1 + r2)

    fun r1(vin: Double, vout: Double, r2: Double): Double = r2 * (vin - vout) / vout

    fun r2(vin: Double, vout: Double, r1: Double): Double = r1 * vout / (vin - vout)

    fun current(vin: Double, r1: Double, r2: Double): Double = vin / (r1 + r2)
}
