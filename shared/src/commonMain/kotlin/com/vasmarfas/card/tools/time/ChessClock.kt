package com.vasmarfas.card.tools.time

enum class ChessSide {
    TOP,
    BOTTOM,
    ;

    val other: ChessSide get() = if (this == TOP) BOTTOM else TOP
}

// times are kept as of since, a monotonic ms reading, and the running side is worked out from the
// current one, so a late frame never shifts them
data class ChessClock(
    val baseMs: Long,
    val incrementMs: Long,
    val top: Long = baseMs,
    val bottom: Long = baseMs,
    val turn: ChessSide? = null,
    val running: Boolean = false,
    val since: Long = 0,
    val topMoves: Int = 0,
    val bottomMoves: Int = 0,
    val flagged: ChessSide? = null,
) {
    val started: Boolean get() = turn != null

    fun remaining(side: ChessSide, now: Long): Long {
        val stored = if (side == ChessSide.TOP) top else bottom
        return if (running && turn == side) maxOf(0, stored - (now - since)) else stored
    }

    fun moves(side: ChessSide): Int = if (side == ChessSide.TOP) topMoves else bottomMoves

    fun press(side: ChessSide, now: Long): ChessClock = when {
        flagged != null -> this
        turn == null -> copy(turn = side.other, running = true, since = now)
        !running || turn != side -> this
        else -> {
            val left = remaining(side, now) + incrementMs
            val moved = if (side == ChessSide.TOP) copy(top = left, topMoves = topMoves + 1) else copy(bottom = left, bottomMoves = bottomMoves + 1)
            moved.copy(turn = side.other, since = now)
        }
    }

    fun pause(now: Long): ChessClock {
        val side = turn ?: return this
        if (!running) return this
        return store(side, remaining(side, now)).copy(running = false)
    }

    fun resume(now: Long): ChessClock = if (started && !running && flagged == null) copy(running = true, since = now) else this

    fun tick(now: Long): ChessClock {
        val side = turn ?: return this
        if (!running || remaining(side, now) > 0) return this
        return store(side, 0).copy(running = false, flagged = side)
    }

    private fun store(side: ChessSide, value: Long): ChessClock = if (side == ChessSide.TOP) copy(top = value) else copy(bottom = value)
}

fun formatChessTime(ms: Long): String {
    val total = ms / 1000
    val hours = total / 3600
    val minutes = total / 60 % 60
    val seconds = (total % 60).toString().padStart(2, '0')
    return when {
        hours > 0 -> "$hours:${minutes.toString().padStart(2, '0')}:$seconds"
        ms < 20_000 -> "$minutes:$seconds.${ms % 1000 / 100}"
        else -> "$minutes:$seconds"
    }
}
