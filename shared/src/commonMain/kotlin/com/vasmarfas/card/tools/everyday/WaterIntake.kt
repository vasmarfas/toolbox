package com.vasmarfas.card.tools.everyday

import kotlin.math.roundToInt

enum class Activity(val extraMl: Int) {
    LOW(0),
    MODERATE(350),
    HIGH(700),
}
enum class Climate(val extraMl: Int) {
    TEMPERATE(0),
    HOT(500),
}
object WaterIntake {
    fun dailyMl(weightKg: Double, activity: Activity, climate: Climate): Int =
        (weightKg * 33 + activity.extraMl + climate.extraMl).roundToInt()
    fun glasses(ml: Int, glassMl: Int): Int = (ml + glassMl - 1) / glassMl
}