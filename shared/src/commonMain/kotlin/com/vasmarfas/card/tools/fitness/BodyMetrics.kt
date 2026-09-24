package com.vasmarfas.card.tools.fitness

import com.vasmarfas.card.resources.*
import kotlin.math.pow
import kotlin.math.sqrt
import org.jetbrains.compose.resources.StringResource

class NamedValue(val name: String, val value: Double)

object BodyMetrics {
    fun waistToHeight(waistCm: Double, heightCm: Double): Double = waistCm / heightCm

    fun waistToHeightNote(ratio: Double): StringResource = when {
        ratio < 0.4 -> Res.string.body_below_the_healthy_band
        ratio < 0.5 -> Res.string.healthy
        ratio < 0.6 -> Res.string.increased_central_fat
        else -> Res.string.high_central_fat
    }

    fun waistToHip(waistCm: Double, hipCm: Double): Double = waistCm / hipCm

    fun waistToHipNote(sex: Sex, ratio: Double): StringResource = if (sex == Sex.MALE) {
        when {
            ratio < 0.90 -> Res.string.low_risk
            ratio < 1.00 -> Res.string.moderate_risk
            else -> Res.string.high_risk
        }
    } else {
        when {
            ratio < 0.80 -> Res.string.low_risk
            ratio < 0.85 -> Res.string.moderate_risk
            else -> Res.string.high_risk
        }
    }

    fun bsaDuBois(kg: Double, cm: Double): Double = 0.007184 * cm.pow(0.725) * kg.pow(0.425)

    fun bsaMosteller(kg: Double, cm: Double): Double = sqrt(cm * kg / 3600.0)

    fun leanMasses(sex: Sex, kg: Double, cm: Double): List<NamedValue> = if (sex == Sex.MALE) {
        listOf(
            NamedValue("Boer", 0.407 * kg + 0.267 * cm - 19.2),
            NamedValue("James", 1.1 * kg - 128 * (kg / cm).pow(2)),
            NamedValue("Hume", 0.32810 * kg + 0.33929 * cm - 29.5336),
        )
    } else {
        listOf(
            NamedValue("Boer", 0.252 * kg + 0.473 * cm - 48.3),
            NamedValue("James", 1.07 * kg - 148 * (kg / cm).pow(2)),
            NamedValue("Hume", 0.29569 * kg + 0.41813 * cm - 43.2933),
        )
    }

    fun leanFromBodyFat(kg: Double, bodyFatPercent: Double): Double = kg * (1.0 - bodyFatPercent / 100.0)

    fun ffmi(leanKg: Double, cm: Double): Double = leanKg / (cm / 100.0).pow(2)

    fun ffmiNormalized(leanKg: Double, cm: Double): Double = ffmi(leanKg, cm) + 6.1 * (1.8 - cm / 100.0)

    fun ffmiNote(sex: Sex, value: Double): StringResource {
        val shift = if (sex == Sex.MALE) 0.0 else -3.0
        return when {
            value < 18 + shift -> Res.string.below_average_muscle_mass
            value < 20 + shift -> Res.string.body_average
            value < 22 + shift -> Res.string.above_average_visibly_trained
            value < 25 + shift -> Res.string.advanced_years_of_training
            else -> Res.string.body_above_the_natural_ceiling
        }
    }
}
