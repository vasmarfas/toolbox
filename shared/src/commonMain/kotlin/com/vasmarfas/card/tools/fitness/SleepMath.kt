package com.vasmarfas.card.tools.fitness

import com.vasmarfas.card.tools.time.pad2

data class SleepOption(val cycles: Int, val minutesOfDay: Int) {
    val hours: Double get() = cycles * SleepMath.CYCLE_MINUTES / 60.0
    val time: String get() = "${(minutesOfDay / 60).pad2()}:${(minutesOfDay % 60).pad2()}"
}
object SleepMath {
    const val CYCLE_MINUTES = 90
    const val FALL_ASLEEP_MINUTES = 14
    fun bedtimes(wakeMinutes: Int): List<SleepOption> =
        (6 downTo 3).map { SleepOption(it, (wakeMinutes - it * CYCLE_MINUTES - FALL_ASLEEP_MINUTES).mod(1440)) }
    fun wakeTimes(bedMinutes: Int): List<SleepOption> =
        (3..6).map { SleepOption(it, (bedMinutes + FALL_ASLEEP_MINUTES + it * CYCLE_MINUTES).mod(1440)) }
}