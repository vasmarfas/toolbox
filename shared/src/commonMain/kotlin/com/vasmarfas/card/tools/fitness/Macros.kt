package com.vasmarfas.card.tools.fitness

import com.vasmarfas.card.resources.Res
import com.vasmarfas.card.resources.bulk
import com.vasmarfas.card.resources.cut
import com.vasmarfas.card.resources.maintain
import org.jetbrains.compose.resources.StringResource

enum class NutritionGoal(val title: StringResource, val defaultPercent: Double) {
    CUT(Res.string.cut, -20.0),
    MAINTAIN(Res.string.maintain, 0.0),
    BULK(Res.string.bulk, 10.0),
}

class MacroSplit(
    val targetKcal: Double,
    val proteinG: Double,
    val fatG: Double,
    val carbsG: Double,
    val proteinKcal: Double,
    val fatKcal: Double,
    val carbsKcal: Double,
)

object Macros {
    const val KCAL_PER_G_PROTEIN = 4.0
    const val KCAL_PER_G_FAT = 9.0
    const val KCAL_PER_G_CARB = 4.0

    fun split(
        tdee: Double,
        goalPercent: Double,
        weightKg: Double,
        proteinPerKg: Double,
        fatPerKg: Double,
    ): MacroSplit {
        val target = tdee * (1.0 + goalPercent / 100.0)
        val protein = proteinPerKg * weightKg
        val fat = fatPerKg * weightKg
        val proteinKcal = protein * KCAL_PER_G_PROTEIN
        val fatKcal = fat * KCAL_PER_G_FAT
        val carbsKcal = target - proteinKcal - fatKcal
        return MacroSplit(
            targetKcal = target,
            proteinG = protein,
            fatG = fat,
            carbsG = carbsKcal / KCAL_PER_G_CARB,
            proteinKcal = proteinKcal,
            fatKcal = fatKcal,
            carbsKcal = carbsKcal,
        )
    }

    fun mealShares(meals: Int): List<Double> = when (meals) {
        3 -> listOf(0.35, 0.40, 0.25)
        4 -> listOf(0.30, 0.30, 0.25, 0.15)
        5 -> listOf(0.25, 0.30, 0.20, 0.15, 0.10)
        6 -> listOf(0.22, 0.25, 0.18, 0.15, 0.10, 0.10)
        else -> List(meals) { 1.0 / meals }
    }
}
