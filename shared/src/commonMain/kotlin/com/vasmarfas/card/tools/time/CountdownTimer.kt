package com.vasmarfas.card.tools.time

data class TimerState(
    val pomodoro: Boolean = false,
    val running: Boolean = false,
    val started: Boolean = false,
    val endAt: Long = 0,
    val remaining: Long = 0,
    val total: Long = 0,
    val work: Boolean = true,
    val cycles: Int = 0,
) {
    fun left(now: Long): Long = when {
        running -> maxOf(0L, endAt - now)
        started -> remaining
        else -> 0
    }

    fun plus(ms: Long): TimerState = if (running) copy(endAt = endAt + ms, total = total + ms) else copy(remaining = remaining + ms, total = total + ms)

    fun encode(): String = listOf(pomodoro, running, started, endAt, remaining, total, work, cycles).joinToString(";")

    companion object {
        const val PREF_KEY = "timer.state"

        const val LONG_BREAK_EVERY = 4

        fun decode(text: String?): TimerState? {
            val p = text?.split(';')?.takeIf { it.size == 8 } ?: return null
            return TimerState(
                pomodoro = p[0].toBooleanStrictOrNull() ?: return null,
                running = p[1].toBooleanStrictOrNull() ?: return null,
                started = p[2].toBooleanStrictOrNull() ?: return null,
                endAt = p[3].toLongOrNull() ?: return null,
                remaining = p[4].toLongOrNull() ?: return null,
                total = p[5].toLongOrNull() ?: return null,
                work = p[6].toBooleanStrictOrNull() ?: return null,
                cycles = p[7].toIntOrNull() ?: return null,
            )
        }

        fun isLongBreak(work: Boolean, cycles: Int, longBreakMs: Long): Boolean =
            !work && longBreakMs > 0 && cycles > 0 && cycles % LONG_BREAK_EVERY == 0
    }
}
