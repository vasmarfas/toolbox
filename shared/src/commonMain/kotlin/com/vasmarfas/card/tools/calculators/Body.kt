package com.vasmarfas.card.tools.calculators

import com.vasmarfas.card.resources.*
import kotlin.math.log10
import kotlin.math.pow
import org.jetbrains.compose.resources.StringResource

enum class Sex { MALE, FEMALE }

enum class ActivityLevel(val factor: Double, val title: StringResource) {
    SEDENTARY(1.2, Res.string.sedentary),
    LIGHT(1.375, Res.string.light_1_3_workouts_a_week),
    MODERATE(1.55, Res.string.moderate_3_5_workouts_a_week),
    ACTIVE(1.725, Res.string.active_6_7_workouts_a_week),
    VERY_ACTIVE(1.9, Res.string.very_active_hard_daily_training),
}

class IdealWeight(val formula: String, val kg: Double)

object Body {
    fun bmi(kg: Double, cm: Double): Double = kg / (cm / 100).pow(2)

    fun bmiCategory(bmi: Double): StringResource = when {
        bmi < 16 -> Res.string.severe_thinness
        bmi < 17 -> Res.string.moderate_thinness
        bmi < 18.5 -> Res.string.mild_thinness
        bmi < 25 -> Res.string.normal
        bmi < 30 -> Res.string.overweight
        bmi < 35 -> Res.string.obesity_class_i
        bmi < 40 -> Res.string.obesity_class_ii
        else -> Res.string.obesity_class_iii
    }

    fun healthyWeightRange(cm: Double): Pair<Double, Double> {
        val m2 = (cm / 100).pow(2)
        return 18.5 * m2 to 24.9 * m2
    }

    fun bmr(sex: Sex, kg: Double, cm: Double, age: Int): Double =
        10 * kg + 6.25 * cm - 5 * age + (if (sex == Sex.MALE) 5 else -161)

    fun idealWeights(sex: Sex, cm: Double): List<IdealWeight> {
        val inchesOver5ft = cm / 2.54 - 60
        return if (sex == Sex.MALE) {
            listOf(
                IdealWeight("Devine", 50.0 + 2.3 * inchesOver5ft),
                IdealWeight("Robinson", 52.0 + 1.9 * inchesOver5ft),
                IdealWeight("Miller", 56.2 + 1.41 * inchesOver5ft),
                IdealWeight("Hamwi", 48.0 + 2.7 * inchesOver5ft),
            )
        } else {
            listOf(
                IdealWeight("Devine", 45.5 + 2.3 * inchesOver5ft),
                IdealWeight("Robinson", 49.0 + 1.7 * inchesOver5ft),
                IdealWeight("Miller", 53.1 + 1.36 * inchesOver5ft),
                IdealWeight("Hamwi", 45.5 + 2.2 * inchesOver5ft),
            )
        }
    }

    fun navyBodyFat(sex: Sex, cm: Double, waistCm: Double, neckCm: Double, hipCm: Double): Double? {
        if (sex == Sex.MALE) {
            if (waistCm <= neckCm) return null
            return 495 / (1.0324 - 0.19077 * log10(waistCm - neckCm) + 0.15456 * log10(cm)) - 450
        }
        if (waistCm + hipCm <= neckCm) return null
        return 495 / (1.29579 - 0.35004 * log10(waistCm + hipCm - neckCm) + 0.22100 * log10(cm)) - 450
    }

    fun bodyFatCategory(sex: Sex, percent: Double): StringResource {
        val (essential, athletes, fitness, average) = if (sex == Sex.MALE) listOf(6.0, 14.0, 18.0, 25.0) else listOf(14.0, 21.0, 25.0, 32.0)
        return when {
            percent < essential -> Res.string.essential_fat
            percent < athletes -> Res.string.athletes
            percent < fitness -> Res.string.fitness
            percent < average -> Res.string.average
            else -> Res.string.above_average
        }
    }
}
