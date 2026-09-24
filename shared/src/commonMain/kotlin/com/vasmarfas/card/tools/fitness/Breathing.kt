package com.vasmarfas.card.tools.fitness

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor

enum class BreathPhase { INHALE, HOLD_IN, EXHALE, HOLD_OUT }

enum class BreathPattern(val inhale: Double, val holdIn: Double, val exhale: Double, val holdOut: Double) {
    BOX(4.0, 4.0, 4.0, 4.0),
    RELAX(4.0, 7.0, 8.0, 0.0),
    COHERENT(5.5, 0.0, 5.5, 0.0),
    CALM(4.0, 0.0, 6.0, 0.0),
    ;

    val cycleSeconds: Double get() = inhale + holdIn + exhale + holdOut

    val phases: List<Pair<BreathPhase, Double>>
        get() = listOf(
            BreathPhase.INHALE to inhale,
            BreathPhase.HOLD_IN to holdIn,
            BreathPhase.EXHALE to exhale,
            BreathPhase.HOLD_OUT to holdOut,
        ).filter { it.second > 0 }
}

class BreathMoment(val phase: BreathPhase, val secondsLeft: Double, val cycle: Int, val fill: Float)

object Breathing {
    fun at(pattern: BreathPattern, elapsedSeconds: Double): BreathMoment {
        val cycle = floor(elapsedSeconds / pattern.cycleSeconds).toInt()
        var t = elapsedSeconds - cycle * pattern.cycleSeconds
        for ((phase, length) in pattern.phases) {
            if (t < length) {
                val progress = t / length
                val eased = ((1 - cos(PI * progress)) / 2).toFloat()
                val fill = when (phase) {
                    BreathPhase.INHALE -> eased
                    BreathPhase.HOLD_IN -> 1f
                    BreathPhase.EXHALE -> 1f - eased
                    BreathPhase.HOLD_OUT -> 0f
                }
                return BreathMoment(phase, length - t, cycle, fill)
            }
            t -= length
        }
        return BreathMoment(BreathPhase.INHALE, pattern.inhale, cycle + 1, 0f)
    }
}
