package com.vasmarfas.card.tools.text

enum class DiffKind { EQUAL, ADDED, REMOVED }

data class DiffLine(val kind: DiffKind, val text: String)

data class DiffResult(
    val lines: List<DiffLine>,
    val added: Int,
    val removed: Int,
    val similarity: Double,
)

object TextDiff {
    fun diffLines(a: String, b: String): DiffResult {
        val x = if (a.isEmpty()) emptyList() else a.lines()
        val y = if (b.isEmpty()) emptyList() else b.lines()
        var prefix = 0
        while (prefix < x.size && prefix < y.size && x[prefix] == y[prefix]) prefix++
        var suffix = 0
        while (suffix < x.size - prefix && suffix < y.size - prefix && x[x.size - 1 - suffix] == y[y.size - 1 - suffix]) suffix++
        val out = ArrayList<DiffLine>(x.size + y.size)
        for (i in 0 until prefix) out += DiffLine(DiffKind.EQUAL, x[i])
        out += middle(x.subList(prefix, x.size - suffix), y.subList(prefix, y.size - suffix))
        for (i in x.size - suffix until x.size) out += DiffLine(DiffKind.EQUAL, x[i])
        val added = out.count { it.kind == DiffKind.ADDED }
        val removed = out.count { it.kind == DiffKind.REMOVED }
        val equal = out.size - added - removed
        val similarity = if (x.isEmpty() && y.isEmpty()) 100.0 else 200.0 * equal / (x.size + y.size)
        return DiffResult(out, added, removed, similarity)
    }

    private fun middle(x: List<String>, y: List<String>): List<DiffLine> {
        val n = x.size
        val m = y.size
        if (n == 0) return y.map { DiffLine(DiffKind.ADDED, it) }
        if (m == 0) return x.map { DiffLine(DiffKind.REMOVED, it) }
        if (n.toLong() * m > 4_000_000L) {
            return x.map { DiffLine(DiffKind.REMOVED, it) } + y.map { DiffLine(DiffKind.ADDED, it) }
        }
        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in n - 1 downTo 0) {
            for (j in m - 1 downTo 0) {
                dp[i][j] = if (x[i] == y[j]) dp[i + 1][j + 1] + 1 else maxOf(dp[i + 1][j], dp[i][j + 1])
            }
        }
        val out = mutableListOf<DiffLine>()
        var i = 0
        var j = 0
        while (i < n && j < m) {
            when {
                x[i] == y[j] -> {
                    out += DiffLine(DiffKind.EQUAL, x[i])
                    i++
                    j++
                }
                dp[i + 1][j] >= dp[i][j + 1] -> {
                    out += DiffLine(DiffKind.REMOVED, x[i])
                    i++
                }
                else -> {
                    out += DiffLine(DiffKind.ADDED, y[j])
                    j++
                }
            }
        }
        while (i < n) out += DiffLine(DiffKind.REMOVED, x[i++])
        while (j < m) out += DiffLine(DiffKind.ADDED, y[j++])
        return out
    }
}
