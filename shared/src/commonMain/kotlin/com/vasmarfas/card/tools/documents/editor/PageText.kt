package com.vasmarfas.card.tools.documents.editor

import com.vasmarfas.card.tools.documents.pdf.PageGlyph
import com.vasmarfas.card.tools.documents.pdf.PdfRect
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

private const val ASCENT = 0.8
private const val DESCENT = 0.22

class TextLine internal constructor(val glyphs: List<PageGlyph>) {
    val text: String = glyphs.joinToString("") { it.text }

    val size: Double get() = glyphs.maxOf { it.size }

    fun quad(from: Int = 0, to: Int = glyphs.size): Quad {
        val first = glyphs[from]
        val last = glyphs[to - 1]
        val nx = -first.dy
        val ny = first.dx
        val up = size * ASCENT
        val down = size * DESCENT
        return Quad(
            doubleArrayOf(
                first.x0 + nx * up, first.y0 + ny * up,
                last.x1 + nx * up, last.y1 + ny * up,
                first.x0 - nx * down, first.y0 - ny * down,
                last.x1 - nx * down, last.y1 - ny * down,
            ),
        )
    }

    val bounds: PdfRect get() = quad().bounds()

    val angle: Int?
        get() {
            val first = glyphs.first()
            return when {
                first.dx > 0.99 -> 0
                first.dy > 0.99 -> 90
                first.dx < -0.99 -> 180
                first.dy < -0.99 -> 270
                else -> null
            }
        }
}

internal fun lineBox(x: Double, y: Double, angle: Int, inset: Double, baseline: Double, width: Double, height: Double): PdfRect {
    val along = height - inset - baseline
    return when (angle) {
        90 -> PdfRect(x + along - height, y - inset, x + along, y - inset + width)
        180 -> PdfRect(x + inset - width, y + along - height, x + inset, y + along)
        270 -> PdfRect(x - along, y + inset - width, x - along + height, y + inset)
        else -> PdfRect(x - inset, y - along, x - inset + width, y - along + height)
    }
}

class TextMatch(val quads: List<Quad>, val text: String) {
    val bounds: PdfRect get() = quads.map { it.bounds() }.reduce { a, b -> a.union(b) }
}

class PageText(glyphs: List<PageGlyph>) {
    val lines: List<TextLine>
    val text: String
    private val owners: IntArray
    private val offsets: IntArray

    init {
        val built = ArrayList<TextLine>()
        var current = ArrayList<PageGlyph>()
        fun flush() {
            if (current.isNotEmpty()) built += TextLine(current)
            current = ArrayList()
        }
        for (glyph in glyphs) {
            val last = current.lastOrNull()
            if (last != null && !continues(last, glyph)) flush()
            if (glyph.text.isBlank() && current.isEmpty()) continue
            current += glyph
        }
        flush()
        lines = built
        val sb = StringBuilder()
        val owner = ArrayList<Int>()
        val offset = ArrayList<Int>()
        for ((l, line) in lines.withIndex()) {
            if (l > 0) {
                sb.append('\n')
                owner += -1
                offset += -1
            }
            for ((g, glyph) in line.glyphs.withIndex()) {
                if (g > 0 && gap(line.glyphs[g - 1], glyph) && !glyph.text.first().isWhitespace() && !line.glyphs[g - 1].text.last().isWhitespace()) {
                    sb.append(' ')
                    owner += -1
                    offset += -1
                }
                for (ch in glyph.text) {
                    sb.append(ch)
                    owner += l
                    offset += g
                }
            }
        }
        text = sb.toString()
        owners = owner.toIntArray()
        offsets = offset.toIntArray()
    }

    fun search(query: String, limit: Int = 500): List<TextMatch> {
        val needle = query.trim().replace(Regex("\\s+"), " ")
        if (needle.isEmpty()) return emptyList()
        val haystack = text.replace('\n', ' ')
        val out = ArrayList<TextMatch>()
        var from = 0
        while (out.size < limit) {
            val at = haystack.indexOf(needle, from, ignoreCase = true)
            if (at < 0) break
            range(at, at + needle.length)?.let { out += it }
            from = at + max(needle.length, 1)
        }
        return out
    }

    fun range(from: Int, to: Int): TextMatch? {
        val quads = ArrayList<Quad>()
        var line = -1
        var first = 0
        var last = 0
        fun close() {
            if (line >= 0) quads += lines[line].quad(first, last + 1)
        }
        for (i in max(from, 0) until min(to, owners.size)) {
            val l = owners[i]
            if (l < 0) continue
            val g = offsets[i]
            if (l != line) {
                close()
                line = l
                first = g
            }
            last = g
        }
        close()
        if (quads.isEmpty()) return null
        return TextMatch(quads, text.substring(max(from, 0), min(to, text.length)).replace('\n', ' ').trim())
    }

    fun offsetAt(x: Double, y: Double): Int? {
        var best = -1
        var bestDistance = Double.MAX_VALUE
        for (i in owners.indices) {
            val l = owners[i]
            if (l < 0) continue
            val glyph = lines[l].glyphs[offsets[i]]
            val cx = (glyph.x0 + glyph.x1) / 2 - glyph.dy * glyph.size * 0.3
            val cy = (glyph.y0 + glyph.y1) / 2 + glyph.dx * glyph.size * 0.3
            val distance = hypot(x - cx, y - cy)
            if (distance < bestDistance) {
                bestDistance = distance
                best = i
            }
        }
        return if (best < 0) null else best
    }

    fun lineAt(x: Double, y: Double): TextLine? = lines.firstOrNull { it.bounds.inflated(1.0).contains(x, y) }
}

internal fun continues(a: PageGlyph, b: PageGlyph): Boolean {
    if (a.dx * b.dx + a.dy * b.dy < 0.95) return false
    val size = max(max(a.size, b.size), 0.01)
    val gx = b.x0 - a.x1
    val gy = b.y0 - a.y1
    val across = gy * a.dx - gx * a.dy
    val along = gx * a.dx + gy * a.dy
    return abs(across) < size * 0.5 && along > -size && along < size * 3
}

internal fun gap(a: PageGlyph, b: PageGlyph): Boolean {
    val along = (b.x0 - a.x1) * a.dx + (b.y0 - a.y1) * a.dy
    return along > max(a.size, b.size) * 0.18
}

private const val NO_GLYPH = " \t\n\r\u200B\u2028\u2029\u00AD\uFEFF"

internal fun markMatches(layout: TextLayout, mark: TextMark, query: String): List<TextMatch> {
    val needle = query.trim().replace(Regex("\\s+"), " ")
    if (needle.isEmpty()) return emptyList()
    val slotLine = ArrayList<Int>()
    val slotStart = ArrayList<Double>()
    val slotEnd = ArrayList<Double>()
    for ((l, line) in layout.lines.withIndex()) {
        for (k in line.boxes.indices) {
            val box = line.boxes[k]
            val scale = box.style.size.toDouble() / box.face.font.unitsPerEm
            var x = layout.inset + line.xs[k].toDouble()
            for (g in box.glyphs.indices) {
                val advance = box.face.font.advanceWidth(box.glyphs[g]) * scale - box.kerns[g] * box.style.size / 1000.0
                slotLine += l
                slotStart += x
                slotEnd += x + advance
                x += advance
            }
        }
    }
    val text = mark.text
    val slotOf = IntArray(text.length) { -1 }
    var slot = 0
    var i = 0
    while (i < text.length) {
        val pair = text[i].isHighSurrogate() && i + 1 < text.length && text[i + 1].isLowSurrogate()
        if (text[i] !in NO_GLYPH) {
            slotOf[i] = slot
            if (pair) slotOf[i + 1] = slot
            slot++
        }
        i += if (pair) 2 else 1
    }
    val (_, height) = Affine.frameSize(mark.box, mark.angle)
    val frame = Affine.frameOf(mark.box, mark.angle)
    val tops = DoubleArray(layout.lines.size)
    var top = height - layout.inset
    for ((l, line) in layout.lines.withIndex()) {
        tops[l] = top
        top -= line.height
    }
    val haystack = text.replace('\n', ' ')
    val out = ArrayList<TextMatch>()
    var from = 0
    while (true) {
        val at = haystack.indexOf(needle, from, ignoreCase = true)
        if (at < 0) break
        from = at + needle.length
        val slots = (at until at + needle.length).map { slotOf[it] }.filter { it in slotLine.indices }.distinct()
        if (slots.isEmpty()) continue
        val quads = slots.groupBy { slotLine[it] }.map { (l, inLine) ->
            val x0 = inLine.minOf { slotStart[it] }
            val x1 = inLine.maxOf { slotEnd[it] }
            val y1 = tops[l]
            val y0 = y1 - layout.lines[l].height
            Quad(doubleArrayOf(frame.x(x0, y1), frame.y(x0, y1), frame.x(x1, y1), frame.y(x1, y1), frame.x(x0, y0), frame.y(x0, y0), frame.x(x1, y0), frame.y(x1, y0)))
        }
        out += TextMatch(quads, haystack.substring(at, at + needle.length))
    }
    return out
}
