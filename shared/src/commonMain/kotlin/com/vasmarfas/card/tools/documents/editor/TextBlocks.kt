package com.vasmarfas.card.tools.documents.editor

import com.vasmarfas.card.tools.documents.pdf.FontStyle
import com.vasmarfas.card.tools.documents.pdf.GraphicKind
import com.vasmarfas.card.tools.documents.pdf.PageGraphic
import com.vasmarfas.card.tools.documents.pdf.PageGlyph
import com.vasmarfas.card.tools.documents.pdf.PdfRect
import com.vasmarfas.card.tools.documents.pdf.TextRun
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private val listMarker = Regex("^([•◦▪‣·*-]|\\d{1,3}[.)])\\s")

class TextBlock internal constructor(val lines: List<TextLine>, val ops: Set<Int>) {
    private val first = lines.first().glyphs.first()
    private val dx = first.dx
    private val dy = first.dy

    val angle: Int? = lines.first().angle

    val size: Double = lines.flatMap { line -> line.glyphs.map { it.size } }.sorted().let { it[it.size / 2] }

    val color: Int = lines.flatMap { line -> line.glyphs.map { it.color } }.groupingBy { it }.eachCount().maxBy { it.value }.key

    val style: FontStyle = lines.flatMap { line -> line.glyphs.filter { !it.text.isBlank() } }.ifEmpty { listOf(first) }.let { glyphs ->
        fun most(test: (FontStyle) -> Boolean) = glyphs.count { test(it.style) } * 2 > glyphs.size
        FontStyle(most { it.bold }, most { it.italic }, most { it.serif }, most { it.mono })
    }

    val bounds: PdfRect = lines.map { it.bounds }.reduce { a, b -> a.union(b) }

    val text: String = joined(null)

    internal val spans: List<StyleSpan> by lazy {
        val looks = ArrayList<Look>()
        joined(looks)
        spansOf(looks, Look(style.bold, style.italic, color))
    }

    private val starts = lines.map { line -> along(line.glyphs.first().x0, line.glyphs.first().y0) }
    private val ends = lines.map { line -> along(line.glyphs.last().x1, line.glyphs.last().y1) }
    private val start = starts.min()

    val width: Double = ends.max() - start

    val spacing: Float = if (lines.size < 2) {
        LINE_SPACING
    } else {
        val steps = lines.zipWithNext { a, b -> across(a) - across(b) }.sorted()
        (steps[steps.size / 2] / size).toFloat().coerceIn(0.9f, 3f)
    }

    val align: MarkAlign = when {
        lines.size < 2 || spread(starts) < size * 0.15 -> MarkAlign.START
        spread(ends) < size * 0.15 -> MarkAlign.END
        spread(starts.zip(ends) { a, b -> (a + b) / 2 }) < size * 0.15 -> MarkAlign.CENTER
        else -> MarkAlign.START
    }

    val anchorX: Double get() = start * dx - across(lines.first()) * dy
    val anchorY: Double get() = start * dy + across(lines.first()) * dx

    private fun joined(looks: MutableList<Look>?): String {
        val out = StringBuilder()
        for (line in lines) {
            if (out.isNotEmpty() && out.last() != '-') {
                out.append(' ')
                looks?.add(looks.last())
            }
            out.append(lineText(line, looks))
        }
        val raw = out.toString()
        val lead = raw.length - raw.trimStart().length
        val trimmed = raw.trim()
        looks?.run {
            subList(lead + trimmed.length, size).clear()
            subList(0, lead).clear()
        }
        return trimmed
    }

    private fun along(x: Double, y: Double) = x * dx + y * dy

    private fun across(line: TextLine) = line.glyphs.first().let { -it.x0 * dy + it.y0 * dx }

    private fun spread(values: List<Double>) = values.max() - values.min()
}

object TextBlocks {
    fun build(runs: List<TextRun>, graphics: List<PageGraphic> = emptyList()): List<TextBlock> {
        val markers = graphics.filter { it.kind == GraphicKind.PATH && it.bounds.width < 12 && it.bounds.height < 12 }.map { it.bounds }
        val lines = ArrayList<Pair<TextLine, Set<Int>>>()
        var glyphs = ArrayList<PageGlyph>()
        var ops = HashSet<Int>()
        fun flush() {
            if (glyphs.isNotEmpty()) lines += TextLine(glyphs) to ops
            glyphs = ArrayList()
            ops = HashSet()
        }
        for (run in runs) {
            for (glyph in run.glyphs) {
                val last = glyphs.lastOrNull()
                if (last != null && !continues(last, glyph)) flush()
                if (glyph.text.isBlank() && glyphs.isEmpty()) continue
                glyphs += glyph
                ops += run.op
            }
        }
        flush()
        val groups = ArrayList<MutableList<Pair<TextLine, Set<Int>>>>()
        for (line in lines) {
            val group = groups.lastOrNull()
            if (group != null && follows(group, line.first) && !marked(line.first, markers)) group += line else groups += mutableListOf(line)
        }
        return merged(groups).map { group -> TextBlock(group.map { it.first }, group.flatMap { it.second }.toSet()) }
    }

    private fun follows(group: List<Pair<TextLine, Set<Int>>>, next: TextLine): Boolean {
        val prev = group.last().first
        val a = prev.glyphs.first()
        val b = next.glyphs.first()
        if (a.dx * b.dx + a.dy * b.dy < 0.99) return false
        val size = prev.size
        if (next.size / size !in 0.9..1.11) return false
        if (look(prev) != look(next)) return false
        if (listMarker.containsMatchIn(lineText(next))) return false
        val step = across(a, a) - across(a, b)
        if (step < size * 0.9 || step > size * 2.2) return false
        if (group.size >= 2) {
            val first = across(a, group[0].first.glyphs.first()) - across(a, group[1].first.glyphs.first())
            if (abs(step - first) > first * 0.25) return false
        }
        val prevStart = along(a, a.x0, a.y0)
        val prevEnd = along(a, prev.glyphs.last().x1, prev.glyphs.last().y1)
        val nextStart = along(a, b.x0, b.y0)
        val nextEnd = along(a, next.glyphs.last().x1, next.glyphs.last().y1)
        if (nextStart > prevStart + size * 1.2 && abs((nextStart + nextEnd) - (prevStart + prevEnd)) > size * 0.6) return false
        val starts = group.map { (line, _) -> along(a, line.glyphs.first().x0, line.glyphs.first().y0) }
        if (group.size >= 2 && starts.max() - starts.min() < size * 0.3 && nextStart < starts.min() - size * 1.2) return false
        return nextStart < prevEnd + size && nextEnd > prevStart - size
    }

    private fun look(line: TextLine): Pair<Boolean, Int> {
        val glyphs = line.glyphs.filter { !it.text.isBlank() }.ifEmpty { line.glyphs }
        return (glyphs.count { it.style.bold } * 2 > glyphs.size) to glyphs.groupingBy { it.color }.eachCount().maxBy { it.value }.key
    }

    private fun marked(line: TextLine, markers: List<PdfRect>): Boolean {
        val first = line.glyphs.first()
        val size = line.size
        val start = along(first, first.x0, first.y0)
        val base = across(first, first)
        return markers.any { m ->
            val cx = (m.left + m.right) / 2
            val cy = (m.bottom + m.top) / 2
            val at = cx * first.dx + cy * first.dy
            val up = -cx * first.dy + cy * first.dx - base
            m.width < size && m.height < size && at in start - size * 2.5..start && up in -size * 0.2..size * 0.9
        }
    }

    private fun along(dir: PageGlyph, x: Double, y: Double) = x * dir.dx + y * dir.dy

    private fun across(dir: PageGlyph, glyph: PageGlyph) = -glyph.x0 * dir.dy + glyph.y0 * dir.dx

    private fun merged(groups: List<List<Pair<TextLine, Set<Int>>>>): List<List<Pair<TextLine, Set<Int>>>> {
        val parent = IntArray(groups.size) { it }
        fun root(i: Int): Int {
            var r = i
            while (parent[r] != r) r = parent[r]
            parent[i] = r
            return r
        }
        val owner = HashMap<Int, Int>()
        groups.forEachIndexed { i, group ->
            for (op in group.flatMap { it.second }) {
                val other = owner.put(op, i)
                if (other != null) parent[root(i)] = root(other)
            }
        }
        return groups.indices.groupBy { root(it) }.values.map { members -> members.flatMap { groups[it] } }
    }
}

internal fun lineText(line: TextLine, looks: MutableList<Look>? = null): String {
    val out = StringBuilder()
    line.glyphs.forEachIndexed { g, glyph ->
        val prev = line.glyphs.getOrNull(g - 1)
        if (prev != null && gap(prev, glyph) && !glyph.text.first().isWhitespace() && !prev.text.last().isWhitespace()) {
            out.append(' ')
            looks?.add(looks.last())
        }
        out.append(glyph.text)
        if (looks != null) {
            val look = Look(glyph.style.bold, glyph.style.italic, glyph.color)
            repeat(glyph.text.length) { looks += look }
        }
    }
    return out.toString()
}

internal fun TextBlock.contains(x: Double, y: Double, tolerance: Double): Boolean = lines.any { it.bounds.inflated(tolerance).contains(x, y) }

internal fun PdfRect.area(): Double = max(0.0, width) * max(0.0, height)
