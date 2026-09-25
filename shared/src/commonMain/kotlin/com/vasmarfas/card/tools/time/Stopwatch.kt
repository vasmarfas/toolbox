package com.vasmarfas.card.tools.time

data class Lap(val index: Int, val lap: Long, val total: Long)

fun lapSplits(totals: List<Long>): List<Lap> =
    totals.mapIndexed { i, total -> Lap(i + 1, total - (if (i == 0) 0L else totals[i - 1]), total) }

// kept in Prefs, so the stopwatch goes on while the tool is closed, like the one in the phone
data class StopwatchState(val running: Boolean = false, val startedAt: Long = 0, val accumulated: Long = 0, val laps: List<Long> = emptyList()) {
    fun elapsed(now: Long): Long = if (running) accumulated + (now - startedAt) else accumulated

    fun encode(): String = listOf(running, startedAt, accumulated, laps.joinToString(",")).joinToString(";")

    companion object {
        const val PREF_KEY = "stopwatch.state"

        fun decode(text: String?): StopwatchState? {
            val parts = text?.split(';') ?: return null
            if (parts.size != 4) return null
            return StopwatchState(
                running = parts[0].toBooleanStrictOrNull() ?: return null,
                startedAt = parts[1].toLongOrNull() ?: return null,
                accumulated = parts[2].toLongOrNull() ?: return null,
                laps = parts[3].split(',').filter { it.isNotEmpty() }.map { it.toLongOrNull() ?: return null },
            )
        }
    }
}

// one line per lap with tabs between the columns, so it pastes into a spreadsheet as a table
fun lapsTable(splits: List<Lap>, header: List<String>): String =
    (listOf(header.joinToString("\t")) + splits.map { "${it.index}\t${formatStopwatch(it.lap)}\t${formatStopwatch(it.total)}" }).joinToString("\n")
