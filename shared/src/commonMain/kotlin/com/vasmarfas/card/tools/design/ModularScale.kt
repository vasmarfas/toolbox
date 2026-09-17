package com.vasmarfas.card.tools.design

import kotlin.math.pow

enum class ScaleRatio(val label: String, val value: Double) {
    MINOR_SECOND("Minor second", 1.067),
    MAJOR_SECOND("Major second", 1.125),
    MINOR_THIRD("Minor third", 1.2),
    MAJOR_THIRD("Major third", 1.25),
    PERFECT_FOURTH("Perfect fourth", 1.333),
    AUGMENTED_FOURTH("Augmented fourth", 1.414),
    PERFECT_FIFTH("Perfect fifth", 1.5),
    GOLDEN("Golden ratio", 1.618),
}
object ModularScale {
    fun steps(base: Double, ratio: Double, from: Int = -2, to: Int = 6): List<Pair<Int, Double>> =
        (from..to).map { it to base * ratio.pow(it) }
    fun lineHeight(size: Double, factor: Double = 1.4): Double = size * factor
}