package com.vasmarfas.card.tools.text

enum class DiffKind { EQUAL, ADDED, REMOVED, CHANGED }

data class DiffRow(
    val kind: DiffKind,
    val oldNumber: Int?,
    val newNumber: Int?,
    val oldText: String?,
    val newText: String?,
    val oldSpans: List<IntRange> = emptyList(),
    val newSpans: List<IntRange> = emptyList(),
)

data class DiffResult(
    val rows: List<DiffRow>,
    val added: Int,
    val removed: Int,
    val changed: Int,
    val similarity: Double,
) {
    val identical: Boolean get() = added == 0 && removed == 0 && changed == 0
}

data class DiffOptions(val ignoreCase: Boolean = false, val ignoreWhitespace: Boolean = false)

object TextDiff {
    private const val PAIRING_CELLS = 40_000
    private val whitespace = Regex("\\s+")

    fun lines(text: String): List<String> {
        if (text.isEmpty()) return emptyList()
        val lines = text.split('\n').map { it.removeSuffix("\r") }
        return if (text.endsWith('\n')) lines.dropLast(1) else lines
    }

    fun diff(old: String, new: String, options: DiffOptions = DiffOptions()): DiffResult {
        val a = lines(old)
        val b = lines(new)
        val ids = HashMap<String, Int>()
        val x = IntArray(a.size) { ids.getOrPut(normalize(a[it], options)) { ids.size } }
        val y = IntArray(b.size) { ids.getOrPut(normalize(b[it], options)) { ids.size } }
        val (gone, came) = Myers.marks(x, y)

        val rows = ArrayList<DiffRow>(maxOf(a.size, b.size))
        var i = 0
        var j = 0
        while (i < a.size || j < b.size) {
            if ((i < a.size && gone[i]) || (j < b.size && came[j])) {
                val i0 = i
                val j0 = j
                while (i < a.size && gone[i]) i++
                while (j < b.size && came[j]) j++
                block(a, i0, i, b, j0, j, options, rows)
            } else {
                rows += DiffRow(DiffKind.EQUAL, i + 1, j + 1, a[i], b[j])
                i++
                j++
            }
        }
        val equal = rows.count { it.kind == DiffKind.EQUAL }
        return DiffResult(
            rows = rows,
            added = rows.count { it.kind == DiffKind.ADDED },
            removed = rows.count { it.kind == DiffKind.REMOVED },
            changed = rows.count { it.kind == DiffKind.CHANGED },
            similarity = if (a.isEmpty() && b.isEmpty()) 100.0 else 200.0 * equal / (a.size + b.size),
        )
    }

    // unified diff, the format git apply and patch read
    fun patch(result: DiffResult, context: Int = 3, oldName: String = "original", newName: String = "modified"): String {
        val rows = result.rows
        if (result.identical) return ""
        val out = StringBuilder()
        out.append("--- ").append(oldName).append('\n')
        out.append("+++ ").append(newName).append('\n')
        var start = 0
        while (start < rows.size) {
            val firstChange = (start until rows.size).firstOrNull { rows[it].kind != DiffKind.EQUAL } ?: break
            var end = firstChange
            var lastChange = firstChange
            while (end < rows.size) {
                if (rows[end].kind != DiffKind.EQUAL) lastChange = end
                if (end - lastChange > 2 * context) break
                end++
            }
            val from = maxOf(start, firstChange - context)
            val to = minOf(rows.size, lastChange + context + 1)
            appendHunk(out, rows, from, to)
            start = to
        }
        return out.toString()
    }

    private fun appendHunk(out: StringBuilder, rows: List<DiffRow>, from: Int, to: Int) {
        val slice = rows.subList(from, to)
        val oldCount = slice.count { it.oldText != null }
        val newCount = slice.count { it.newText != null }
        val oldStart = slice.firstNotNullOfOrNull { it.oldNumber } ?: ((rows.subList(0, from).lastOrNull { it.oldNumber != null }?.oldNumber ?: 0))
        val newStart = slice.firstNotNullOfOrNull { it.newNumber } ?: ((rows.subList(0, from).lastOrNull { it.newNumber != null }?.newNumber ?: 0))
        out.append("@@ -").append(oldStart).append(',').append(oldCount)
            .append(" +").append(newStart).append(',').append(newCount).append(" @@\n")
        var k = 0
        while (k < slice.size) {
            val row = slice[k]
            if (row.kind == DiffKind.EQUAL) {
                out.append(' ').append(row.newText).append('\n')
                k++
                continue
            }
            val blockEnd = (k until slice.size).firstOrNull { slice[it].kind == DiffKind.EQUAL } ?: slice.size
            for (r in k until blockEnd) slice[r].oldText?.let { out.append('-').append(it).append('\n') }
            for (r in k until blockEnd) slice[r].newText?.let { out.append('+').append(it).append('\n') }
            k = blockEnd
        }
    }

    private fun normalize(line: String, options: DiffOptions): String {
        var s = line
        if (options.ignoreWhitespace) s = s.trim().replace(whitespace, " ")
        if (options.ignoreCase) s = s.lowercase()
        return s
    }

    // lines are paired in order, preferring the most similar text, so an edit next to an insertion still
    // reads as an edit
    private fun block(
        a: List<String>, i0: Int, i1: Int,
        b: List<String>, j0: Int, j1: Int,
        options: DiffOptions,
        rows: MutableList<DiffRow>,
    ) {
        val d = i1 - i0
        val n = j1 - j0
        if (d == 0 || n == 0 || d.toLong() * n > PAIRING_CELLS) {
            val paired = if (d.toLong() * n > PAIRING_CELLS) minOf(d, n) else 0
            for (k in 0 until paired) rows += changedRow(a, i0 + k, b, j0 + k, options)
            for (k in paired until d) rows += DiffRow(DiffKind.REMOVED, i0 + k + 1, null, a[i0 + k], null)
            for (k in paired until n) rows += DiffRow(DiffKind.ADDED, null, j0 + k + 1, null, b[j0 + k])
            return
        }
        val grams = List(d) { bigrams(normalize(a[i0 + it], options)) }
        val gramsNew = List(n) { bigrams(normalize(b[j0 + it], options)) }
        val weight = Array(d) { p -> DoubleArray(n) { q -> 0.25 + dice(grams[p], gramsNew[q]) } }
        val best = Array(d + 1) { DoubleArray(n + 1) }
        for (p in d - 1 downTo 0) {
            for (q in n - 1 downTo 0) {
                best[p][q] = maxOf(weight[p][q] + best[p + 1][q + 1], best[p + 1][q], best[p][q + 1])
            }
        }
        var p = 0
        var q = 0
        while (p < d || q < n) {
            when {
                p < d && q < n && best[p][q] == weight[p][q] + best[p + 1][q + 1] -> {
                    rows += changedRow(a, i0 + p, b, j0 + q, options)
                    p++
                    q++
                }
                p < d && (q == n || best[p][q] == best[p + 1][q]) -> {
                    rows += DiffRow(DiffKind.REMOVED, i0 + p + 1, null, a[i0 + p], null)
                    p++
                }
                else -> {
                    rows += DiffRow(DiffKind.ADDED, null, j0 + q + 1, null, b[j0 + q])
                    q++
                }
            }
        }
    }

    private fun changedRow(a: List<String>, i: Int, b: List<String>, j: Int, options: DiffOptions): DiffRow {
        val (oldSpans, newSpans) = inlineSpans(a[i], b[j], options)
        return DiffRow(DiffKind.CHANGED, i + 1, j + 1, a[i], b[j], oldSpans, newSpans)
    }

    fun inlineSpans(old: String, new: String, options: DiffOptions = DiffOptions()): Pair<List<IntRange>, List<IntRange>> {
        val x = tokens(old)
        val y = tokens(new)
        val ids = HashMap<String, Int>()
        fun id(text: String, range: IntRange): Int {
            var token = text.substring(range)
            if (options.ignoreWhitespace && token.isBlank()) token = " "
            if (options.ignoreCase) token = token.lowercase()
            return ids.getOrPut(token) { ids.size }
        }
        val (gone, came) = Myers.marks(IntArray(x.size) { id(old, x[it]) }, IntArray(y.size) { id(new, y[it]) })
        return spans(x, gone, old, options) to spans(y, came, new, options)
    }

    private fun spans(tokens: List<IntRange>, marked: BooleanArray, text: String, options: DiffOptions): List<IntRange> {
        val out = mutableListOf<IntRange>()
        tokens.forEachIndexed { k, range ->
            if (!marked[k] || (options.ignoreWhitespace && text.substring(range).isBlank())) return@forEachIndexed
            val last = out.lastOrNull()
            if (last != null && last.last + 1 == range.first) out[out.lastIndex] = last.first..range.last else out += range
        }
        return out
    }

    private fun tokens(s: String): List<IntRange> {
        val out = mutableListOf<IntRange>()
        var k = 0
        while (k < s.length) {
            val start = k
            val c = s[k]
            when {
                c.isLetterOrDigit() || c == '_' -> while (k < s.length && (s[k].isLetterOrDigit() || s[k] == '_')) k++
                c.isWhitespace() -> while (k < s.length && s[k].isWhitespace()) k++
                else -> k++
            }
            out += start until k
        }
        return out
    }

    private fun bigrams(s: String): IntArray {
        if (s.length < 2) return if (s.isEmpty()) IntArray(0) else intArrayOf(s[0].code)
        val out = IntArray(s.length - 1) { (s[it].code shl 16) or s[it + 1].code }
        out.sort()
        return out
    }

    private fun dice(x: IntArray, y: IntArray): Double {
        if (x.isEmpty() && y.isEmpty()) return 1.0
        var p = 0
        var q = 0
        var common = 0
        while (p < x.size && q < y.size) {
            when {
                x[p] == y[q] -> {
                    common++
                    p++
                    q++
                }
                x[p] < y[q] -> p++
                else -> q++
            }
        }
        return 2.0 * common / (x.size + y.size)
    }
}

// Myers' O(ND) diff in its linear-space form: the middle snake splits the problem until one side is empty
internal object Myers {
    private const val STEP_BUDGET = 30_000_000L

    fun marks(a: IntArray, b: IntArray): Pair<BooleanArray, BooleanArray> {
        val search = Search(a, b)
        search.compare(0, a.size, 0, b.size)
        return search.removed to search.added
    }

    private class Search(val a: IntArray, val b: IntArray) {
        val removed = BooleanArray(a.size)
        val added = BooleanArray(b.size)
        var budget = STEP_BUDGET

        fun compare(aFrom: Int, aTo: Int, bFrom: Int, bTo: Int) {
            var aLo = aFrom
            var bLo = bFrom
            var aHi = aTo
            var bHi = bTo
            while (aLo < aHi && bLo < bHi && a[aLo] == b[bLo]) {
                aLo++
                bLo++
            }
            while (aHi > aLo && bHi > bLo && a[aHi - 1] == b[bHi - 1]) {
                aHi--
                bHi--
            }
            if (aLo == aHi || bLo == bHi) {
                for (k in aLo until aHi) removed[k] = true
                for (k in bLo until bHi) added[k] = true
                return
            }
            val split = if (budget > 0) bisect(aLo, aHi, bLo, bHi) else null
            if (split == null) {
                for (k in aLo until aHi) removed[k] = true
                for (k in bLo until bHi) added[k] = true
                return
            }
            compare(aLo, split.first, bLo, split.second)
            compare(split.first, aHi, split.second, bHi)
        }

        private fun bisect(aLo: Int, aHi: Int, bLo: Int, bHi: Int): Pair<Int, Int>? {
            val n = aHi - aLo
            val m = bHi - bLo
            val maxD = (n + m + 1) / 2
            val offset = maxD
            val length = 2 * maxD + 2
            val forward = IntArray(length) { -1 }
            val backward = IntArray(length) { -1 }
            forward[offset + 1] = 0
            backward[offset + 1] = 0
            val delta = n - m
            val odd = delta % 2 != 0
            var k1Start = 0
            var k1End = 0
            var k2Start = 0
            var k2End = 0
            for (d in 0 until maxD) {
                var k1 = -d + k1Start
                while (k1 <= d - k1End) {
                    val i1 = offset + k1
                    var x1 = if (k1 == -d || (k1 != d && forward[i1 - 1] < forward[i1 + 1])) forward[i1 + 1] else forward[i1 - 1] + 1
                    var y1 = x1 - k1
                    while (x1 < n && y1 < m && a[aLo + x1] == b[bLo + y1]) {
                        x1++
                        y1++
                        budget--
                    }
                    budget--
                    forward[i1] = x1
                    if (x1 > n) {
                        k1End += 2
                    } else if (y1 > m) {
                        k1Start += 2
                    } else if (odd) {
                        val i2 = offset + delta - k1
                        if (i2 in 0 until length && backward[i2] != -1 && x1 >= n - backward[i2]) {
                            return aLo + x1 to bLo + y1
                        }
                    }
                    k1 += 2
                }
                var k2 = -d + k2Start
                while (k2 <= d - k2End) {
                    val i2 = offset + k2
                    var x2 = if (k2 == -d || (k2 != d && backward[i2 - 1] < backward[i2 + 1])) backward[i2 + 1] else backward[i2 - 1] + 1
                    var y2 = x2 - k2
                    while (x2 < n && y2 < m && a[aLo + n - x2 - 1] == b[bLo + m - y2 - 1]) {
                        x2++
                        y2++
                        budget--
                    }
                    budget--
                    backward[i2] = x2
                    if (x2 > n) {
                        k2End += 2
                    } else if (y2 > m) {
                        k2Start += 2
                    } else if (!odd) {
                        val i1 = offset + delta - k2
                        if (i1 in 0 until length && forward[i1] != -1) {
                            val x1 = forward[i1]
                            val y1 = offset + x1 - i1
                            if (x1 >= n - x2) return aLo + x1 to bLo + y1
                        }
                    }
                    k2 += 2
                }
                if (budget <= 0) return null
            }
            return null
        }
    }
}
