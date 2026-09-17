package com.vasmarfas.card.tools.calculators

import com.vasmarfas.card.core.fmt
import kotlin.math.floor

object Battery {
    fun runtimeHours(capacityMah: Double, loadMa: Double, efficiencyPercent: Double): Double =
        capacityMah * efficiencyPercent / 100 / loadMa

    fun runtimeHoursByPower(capacityMah: Double, voltage: Double, loadW: Double, efficiencyPercent: Double): Double =
        capacityMah / 1000 * voltage * efficiencyPercent / 100 / loadW

    fun chargeHours(capacityMah: Double, chargerMa: Double, efficiencyPercent: Double): Double =
        capacityMah / (chargerMa * efficiencyPercent / 100)

    fun mahToWh(mah: Double, voltage: Double): Double = mah * voltage / 1000

    fun whToMah(wh: Double, voltage: Double): Double = wh / voltage * 1000

    fun formatHours(hours: Double): String {
        if (hours.isNaN() || hours.isInfinite()) return "∞"
        val totalMinutes = floor(hours * 60 + 0.5)
        val days = floor(totalMinutes / 1440)
        val h = floor((totalMinutes - days * 1440) / 60)
        val m = totalMinutes - days * 1440 - h * 60
        return buildString {
            if (days > 0) append(days.fmt(0)).append(" d ")
            if (days > 0 || h > 0) append(h.fmt(0)).append(" h ")
            append(m.fmt(0)).append(" min")
        }
    }
}
