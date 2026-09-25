package com.vasmarfas.card.tools.fitness

import kotlin.math.ln
import kotlin.math.pow

enum class BpCategoryAha { NORMAL, ELEVATED, STAGE_1, STAGE_2, CRISIS }

enum class BpCategoryEsc { OPTIMAL, NORMAL, HIGH_NORMAL, GRADE_1, GRADE_2, GRADE_3, ISOLATED_SYSTOLIC }

object BloodPressure {
    fun aha(systolic: Int, diastolic: Int): BpCategoryAha = when {
        systolic > 180 || diastolic > 120 -> BpCategoryAha.CRISIS
        systolic >= 140 || diastolic >= 90 -> BpCategoryAha.STAGE_2
        systolic >= 130 || diastolic >= 80 -> BpCategoryAha.STAGE_1
        systolic >= 120 -> BpCategoryAha.ELEVATED
        else -> BpCategoryAha.NORMAL
    }

    fun esc(systolic: Int, diastolic: Int): BpCategoryEsc = when {
        systolic >= 140 && diastolic < 90 -> BpCategoryEsc.ISOLATED_SYSTOLIC
        systolic >= 180 || diastolic >= 110 -> BpCategoryEsc.GRADE_3
        systolic >= 160 || diastolic >= 100 -> BpCategoryEsc.GRADE_2
        systolic >= 140 || diastolic >= 90 -> BpCategoryEsc.GRADE_1
        systolic >= 130 || diastolic >= 85 -> BpCategoryEsc.HIGH_NORMAL
        systolic >= 120 || diastolic >= 80 -> BpCategoryEsc.NORMAL
        else -> BpCategoryEsc.OPTIMAL
    }

    fun pulsePressure(systolic: Int, diastolic: Int): Int = systolic - diastolic

    fun meanArterial(systolic: Int, diastolic: Int): Double =
        diastolic + (systolic - diastolic) / 3.0
}

object Glycemia {
    fun eagMgDl(a1cPercent: Double): Double = 28.7 * a1cPercent - 46.7

    fun eagMmolL(a1cPercent: Double): Double = 1.59 * a1cPercent - 2.59

    fun a1cFromEagMmolL(mmolPerL: Double): Double = (mmolPerL + 2.59) / 1.59

    fun ifccMmolMol(a1cPercent: Double): Double = (a1cPercent - 2.15) * 10.929

    fun dcctFromIfcc(mmolPerMol: Double): Double = mmolPerMol / 10.929 + 2.15

    // WHO and ADA thresholds: 0 below 5.7 %, 1 prediabetes up to 6.4 %, 2 diabetes from 6.5 %
    fun category(a1cPercent: Double): Int = when {
        a1cPercent < 5.7 -> 0
        a1cPercent < 6.5 -> 1
        else -> 2
    }
}

object Widmark {
    const val EthanolDensity = 0.789

    private const val RatioMale = 0.68
    private const val RatioFemale = 0.55

    const val EliminationLow = 0.10
    const val EliminationTypical = 0.15
    const val EliminationHigh = 0.20

    fun gramsOfEthanol(volumeMl: Double, abvPercent: Double): Double =
        volumeMl * abvPercent / 100.0 * EthanolDensity

    fun promille(
        grams: Double,
        bodyMassKg: Double,
        sex: Sex,
        hoursSince: Double,
        eliminationPerHour: Double = EliminationTypical,
    ): Double {
        val ratio = if (sex == Sex.MALE) RatioMale else RatioFemale
        val peak = grams / (ratio * bodyMassKg)
        return (peak - eliminationPerHour * hoursSince).coerceAtLeast(0.0)
    }

    fun hoursToClear(
        grams: Double,
        bodyMassKg: Double,
        sex: Sex,
        eliminationPerHour: Double = EliminationTypical,
    ): Double {
        val ratio = if (sex == Sex.MALE) RatioMale else RatioFemale
        return grams / (ratio * bodyMassKg) / eliminationPerHour
    }
}

object Caffeine {
    const val DefaultHalfLifeHours = 5.0

    fun remainingMg(doseMg: Double, hoursSince: Double, halfLifeHours: Double = DefaultHalfLifeHours): Double =
        doseMg * 0.5.pow(hoursSince / halfLifeHours)

    fun hoursUntil(doseMg: Double, targetMg: Double, halfLifeHours: Double = DefaultHalfLifeHours): Double? {
        if (targetMg <= 0.0 || doseMg <= targetMg) return null
        return halfLifeHours * ln(doseMg / targetMg) / ln(2.0)
    }
}

object Vo2Max {

    fun cooper(metres: Double): Double = (metres - 504.9) / 44.73

    fun rockport(
        massKg: Double,
        ageYears: Int,
        sex: Sex,
        walkMinutes: Double,
        heartRateBpm: Int,
    ): Double = 132.853 - 0.0769 * (massKg * 2.20462) - 0.3877 * ageYears +
        6.315 * (if (sex == Sex.MALE) 1 else 0) - 3.2649 * walkMinutes - 0.1565 * heartRateBpm

    fun heartRateRatio(maxBpm: Int, restingBpm: Int): Double = 15.3 * maxBpm / restingBpm

    fun mets(vo2Max: Double): Double = vo2Max / 3.5

    // Cooper Institute norms by decade of age from 20: where poor, fair, good, excellent and superior begin
    private val men = listOf(
        listOf(33.0, 36.5, 42.5, 46.5, 52.5),
        listOf(31.5, 35.5, 41.0, 45.0, 49.5),
        listOf(30.2, 33.6, 39.0, 43.8, 48.1),
        listOf(26.1, 31.0, 35.8, 41.0, 45.4),
        listOf(20.5, 26.1, 32.3, 36.5, 44.3),
    )
    private val women = listOf(
        listOf(23.6, 29.0, 33.0, 37.0, 41.1),
        listOf(22.8, 27.0, 31.5, 35.7, 40.1),
        listOf(21.0, 24.5, 29.0, 32.9, 37.0),
        listOf(20.2, 22.8, 27.0, 31.5, 35.8),
        listOf(17.5, 20.2, 24.5, 30.3, 31.5),
    )

    fun norms(sex: Sex, age: Int): List<Double> = (if (sex == Sex.MALE) men else women)[((age - 20) / 10).coerceIn(0, 4)]

    // 0 is very poor, 5 is superior
    fun rating(value: Double, sex: Sex, age: Int): Int = norms(sex, age).count { value >= it }
}
