package com.vasmarfas.card.tools.everyday

import kotlin.math.pow

object DecisionWheel {
    fun parseOptions(text: String): List<String> = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
    fun stepsToReach(current: Int, target: Int, size: Int, minSteps: Int = 24): Int {
        val direct = (target - current).mod(size)
        var steps = direct
        while (steps < minSteps) steps += size
        return steps
    }
    fun delays(steps: Int): List<Long> = List(steps) { i -> (35.0 * 1.13.pow(i)).toLong().coerceAtMost(650L) }
}