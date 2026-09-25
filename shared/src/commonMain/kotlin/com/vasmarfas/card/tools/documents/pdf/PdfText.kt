package com.vasmarfas.card.tools.documents.pdf

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max

// a baseline shift over half the font size starts a new line, a gap wider than half the space glyph
// (clamped to 0.1..0.3 of the font size) inserts a space
object PdfText {
    fun extract(document: PdfDocument, pageIndex: Int): String {
        val page = document.page(pageIndex)
        val lines = TextLines()
        ContentInterpreter(document, lines).run(page)
        return lines.result()
    }

    fun glyphs(document: PdfDocument, pageIndex: Int): List<PageGlyph> {
        val sink = GlyphList()
        ContentInterpreter(document, sink).run(document.page(pageIndex))
        return sink.glyphs
    }
}

// one shown character in user space: the baseline from (x0, y0) to (x1, y1), dx and dy its unit direction
class PageGlyph(
    val text: String,
    val x0: Double,
    val y0: Double,
    val x1: Double,
    val y1: Double,
    val size: Double,
    val dx: Double,
    val dy: Double,
    val color: Int,
    val style: FontStyle,
)

class FontStyle(val bold: Boolean, val italic: Boolean, val serif: Boolean, val mono: Boolean)

internal interface GlyphSink {
    fun glyph(text: String?, startX: Double, startY: Double, stopX: Double, stopY: Double, fontSize: Double, spaceWidth: Double, dx: Double, dy: Double, color: Int, style: FontStyle)
}

private class GlyphList : GlyphSink {
    val glyphs = ArrayList<PageGlyph>()

    override fun glyph(text: String?, startX: Double, startY: Double, stopX: Double, stopY: Double, fontSize: Double, spaceWidth: Double, dx: Double, dy: Double, color: Int, style: FontStyle) {
        if (text.isNullOrEmpty() || glyphs.size >= 200_000) return
        glyphs += PageGlyph(text, startX, startY, stopX, stopY, fontSize, dx, dy, color, style)
    }
}

private class TextLines : GlyphSink {
    private val out = StringBuilder()
    private var started = false
    private var endX = 0.0
    private var endY = 0.0
    private var dirX = 1.0
    private var dirY = 0.0
    private var size = 0.0
    private var lastSpace = 0.0
    private val shown = HashMap<Long, Points>()

    override fun glyph(text: String?, startX: Double, startY: Double, stopX: Double, stopY: Double, fontSize: Double, spaceWidth: Double, dx: Double, dy: Double, color: Int, style: FontStyle) {
        if (text != null && text.length == 1 && overprinted(text[0], startX, startY, abs(stopX - startX) + abs(stopY - startY), fontSize)) return
        if (started) {
            val gapX = startX - endX
            val gapY = startY - endY
            val along = gapX * dirX + gapY * dirY
            val across = gapY * dirX - gapX * dirY
            val scale = max(max(size, fontSize), 0.01)
            val gap = ((lastSpace + spaceWidth) / 4).coerceIn(0.1 * scale, 0.3 * scale)
            if (dx * dirX + dy * dirY < 0.95 || abs(across) > 0.5 * scale) {
                newLine()
            } else if ((along > gap || along < -scale) && text?.firstOrNull()?.isWhitespace() != true) {
                space()
            }
        }
        if (text != null) {
            for (ch in text) {
                if (ch == '\t' || ch == '\u00A0') space() else if (ch >= ' ') out.append(ch)
            }
        }
        started = true
        endX = stopX
        endY = stopY
        dirX = dx
        dirY = dy
        size = fontSize
        lastSpace = spaceWidth
    }

    // fake bold draws a glyph twice with a small offset. An identical character within a third of its
    // width and a third of the font size of one already shown is dropped, as PDFBox does
    private fun overprinted(ch: Char, x: Double, y: Double, width: Double, fontSize: Double): Boolean {
        val cell = max(fontSize, 1.0)
        val cellX = floor(x / cell).toLong()
        val cellY = floor(y / cell).toLong()
        for (i in -1L..1L) {
            for (j in -1L..1L) {
                val near = shown[cellKey(ch, cellX + i, cellY + j)] ?: continue
                for (k in 0 until near.size step 2) {
                    if (abs(near.xy[k] - x) < width / 3 && abs(near.xy[k + 1] - y) < fontSize / 3) return true
                }
            }
        }
        shown.getOrPut(cellKey(ch, cellX, cellY)) { Points() }.add(x, y)
        return false
    }

    private class Points {
        var xy = DoubleArray(4)
        var size = 0

        fun add(x: Double, y: Double) {
            if (size + 2 > xy.size) xy = xy.copyOf(xy.size * 2)
            xy[size++] = x
            xy[size++] = y
        }
    }

    private fun cellKey(ch: Char, x: Long, y: Long): Long = (ch.code.toLong() shl 42) xor ((x and 0x1FFFFF) shl 21) xor (y and 0x1FFFFF)

    private fun space() {
        if (out.isNotEmpty() && out[out.length - 1] != ' ' && out[out.length - 1] != '\n') out.append(' ')
    }

    private fun newLine() {
        if (out.isNotEmpty() && out[out.length - 1] != '\n') out.append('\n')
    }

    fun result(): String = out.split('\n').map { it.trimEnd() }.filter { it.isNotEmpty() }.joinToString("\n")
}
