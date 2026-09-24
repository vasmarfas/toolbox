package com.vasmarfas.card.tools.fitness

import com.vasmarfas.card.resources.*
import org.jetbrains.compose.resources.StringResource

enum class MaxHrFormula(val title: StringResource) {
    CLASSIC(Res.string.s_220_age),
    TANAKA(Res.string.tanaka_208_0_7_age),
    GULATI(Res.string.gulati_206_0_88_age),
}

class HrZone(
    val index: Int,
    val lowPercent: Int,
    val highPercent: Int,
    val title: StringResource,
    val trains: StringResource,
)

val hrZones = listOf(
    HrZone(
        1, 50, 60,
        Res.string.recovery,
        Res.string.hr_warm_up_cool_down,
    ),
    HrZone(
        2, 60, 70,
        Res.string.aerobic_base,
        Res.string.hr_fat_oxidation_and_capillary,
    ),
    HrZone(
        3, 70, 80,
        Res.string.tempo,
        Res.string.aerobic_power_steady_long_efforts,
    ),
    HrZone(
        4, 80, 90,
        Res.string.threshold,
        Res.string.hr_lactate_threshold_race_pace,
    ),
    HrZone(
        5, 90, 100,
        Res.string.maximal,
        Res.string.hr_vo2max_intervals,
    ),
)

object HeartRate {
    fun maxHr(formula: MaxHrFormula, age: Int): Double = when (formula) {
        MaxHrFormula.CLASSIC -> 220.0 - age
        MaxHrFormula.TANAKA -> 208.0 - 0.7 * age
        MaxHrFormula.GULATI -> 206.0 - 0.88 * age
    }

    fun byMaxPercent(maxHr: Double, percent: Int): Double = maxHr * percent / 100.0

    fun karvonen(maxHr: Double, restingHr: Double, percent: Int): Double =
        restingHr + (maxHr - restingHr) * percent / 100.0

    fun reserve(maxHr: Double, restingHr: Double): Double = maxHr - restingHr
}
