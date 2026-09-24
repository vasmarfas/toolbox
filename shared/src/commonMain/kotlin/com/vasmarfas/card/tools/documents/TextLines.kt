package com.vasmarfas.card.tools.documents

import com.vasmarfas.card.tools.documents.pdf.PdfWriter
import com.vasmarfas.card.tools.documents.pdf.TrueTypeFont

internal class RunStyle(
    val family: FontFamily,
    val size: Float,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strike: Boolean = false,
    val rise: Float = 0f,
    val link: String? = null,
    val gray: Float = 0f,
)

// kerns are thousandths of an em after each glyph, as TJ wants them
internal class Box(val face: Face, val style: RunStyle, val glyphs: IntArray, val kerns: IntArray, val width: Float)

internal sealed interface Piece

internal class Word(val boxes: List<Box>) : Piece {
    val width: Float = boxes.sumOf { it.width.toDouble() }.toFloat()
}

internal class Glue(val width: Float) : Piece

internal data object HardBreak : Piece

internal class Line(val boxes: List<Box>, val xs: FloatArray, val height: Float, val baseline: Float)

internal const val RUBLE = 0x20BD

internal class Faces(private val writer: PdfWriter, private val fallbacks: List<FontFamily>) {
    private val byFont = LinkedHashMap<TrueTypeFont, Face>()
    private val rubles = LinkedHashMap<TrueTypeFont, Face>()

    val all: Collection<Face> get() = byFont.values + rubles.values

    fun face(font: TrueTypeFont): Face = byFont.getOrPut(font) { Face(font, "F${byFont.size + 1}", writer.reserve()) }

    private fun ruble(font: TrueTypeFont): Face = rubles.getOrPut(font) { Face(font, "R${rubles.size + 1}", writer.reserve(), ruble = true) }

    fun resolve(codePoint: Int, style: RunStyle): Pair<Face, Int> {
        val preferred = style.family.pick(style.bold, style.italic)
        preferred.glyphId(codePoint).takeIf { it != 0 }?.let { return face(preferred) to it }
        for (family in fallbacks) {
            val font = family.pick(style.bold, style.italic)
            font.glyphId(codePoint).takeIf { it != 0 }?.let { return face(font) to it }
        }
        if (codePoint == RUBLE) return ruble(preferred) to preferred.glyphId('Р'.code)
        return face(preferred) to preferred.glyphId('?'.code)
    }
}

internal class PieceBuilder(private val faces: Faces) {
    val pieces = ArrayList<Piece>()
    private val word = ArrayList<Box>()
    private var face: Face? = null
    private var style: RunStyle? = null
    private val glyphs = ArrayList<Int>()
    private val codes = ArrayList<Int>()

    fun text(text: String, runStyle: RunStyle, keepSpaces: Boolean = false) {
        var i = 0
        while (i < text.length) {
            val c = text[i]
            val cp = if (c.isHighSurrogate() && i + 1 < text.length && text[i + 1].isLowSurrogate()) {
                ((c.code - 0xD800) shl 10) + (text[i + 1].code - 0xDC00) + 0x10000
            } else {
                c.code
            }
            i += if (cp >= 0x10000) 2 else 1
            when (cp) {
                ' '.code, '\t'.code, 0x200B -> if (keepSpaces && cp != 0x200B) glyph(' '.code, runStyle) else glue(if (cp == 0x200B) 0f else space(runStyle))
                '\n'.code, 0x2028, 0x2029 -> hardBreak()
                0xAD, '\r'.code, 0xFEFF -> Unit
                0xA0 -> glyph(' '.code, runStyle)
                else -> glyph(cp, runStyle)
            }
        }
    }

    fun hardBreak() {
        endWord()
        pieces += HardBreak
    }

    fun finish(): List<Piece> {
        endWord()
        return pieces
    }

    private fun space(runStyle: RunStyle): Float {
        val (spaceFace, glyph) = faces.resolve(' '.code, runStyle)
        return spaceFace.font.advanceWidth(glyph) * spaceFace.scale(runStyle.size)
    }

    private fun glue(width: Float) {
        endWord()
        if (pieces.lastOrNull() !is Glue && pieces.isNotEmpty() && pieces.last() !is HardBreak) pieces += Glue(width)
    }

    private fun glyph(cp: Int, runStyle: RunStyle) {
        val (target, glyph) = faces.resolve(cp, runStyle)
        if (target !== face || runStyle !== style) endBox()
        face = target
        style = runStyle
        glyphs += glyph
        codes += cp
        if (target.ruble) endBox()
    }

    private fun endBox() {
        val boxFace = face ?: return
        val boxStyle = style ?: return
        if (glyphs.isEmpty()) return
        val scale = boxFace.scale(boxStyle.size)
        val ids = glyphs.toIntArray()
        val kerns = IntArray(ids.size)
        var width = 0f
        for (k in ids.indices) {
            boxFace.use(ids[k], codes[k])
            width += boxFace.font.advanceWidth(ids[k]) * scale
            if (k + 1 < ids.size) {
                val kern = boxFace.font.kerning(ids[k], ids[k + 1])
                if (kern != 0) {
                    kerns[k] = -kern * 1000 / boxFace.font.unitsPerEm
                    width += kern * scale
                }
            }
        }
        word += Box(boxFace, boxStyle, ids, kerns, width)
        glyphs.clear()
        codes.clear()
        face = null
        style = null
    }

    private fun endWord() {
        endBox()
        if (word.isNotEmpty()) {
            pieces += Word(word.toList())
            word.clear()
        }
    }
}

internal object LineBreaker {
    fun lines(pieces: List<Piece>, width: Float, align: Align, spacing: Float, emptySize: Float): List<Line> {
        val out = ArrayList<Line>()
        val current = ArrayList<Box>()
        val xs = ArrayList<Float>()
        val glueAt = ArrayList<Int>()
        var x = 0f
        var pendingGlue = 0f

        fun emit(justify: Boolean) {
            out += build(current, xs, glueAt, x, width, if (justify || align != Align.JUSTIFY) align else Align.START, spacing, emptySize)
            current.clear()
            xs.clear()
            glueAt.clear()
            x = 0f
            pendingGlue = 0f
        }

        fun place(word: Word) {
            for (box in word.boxes) {
                current += box
                xs += x
                x += box.width
            }
        }

        for (piece in pieces) {
            when (piece) {
                is Glue -> if (current.isNotEmpty()) pendingGlue = piece.width
                is HardBreak -> emit(justify = false)
                is Word -> {
                    if (current.isNotEmpty() && x + pendingGlue + piece.width > width) emit(justify = true)
                    if (current.isEmpty() && piece.width > width) {
                        for (chunk in split(piece, width)) {
                            if (current.isNotEmpty()) emit(justify = false)
                            place(chunk)
                        }
                    } else {
                        if (current.isNotEmpty()) {
                            x += pendingGlue
                            glueAt += current.size
                        }
                        place(piece)
                    }
                    pendingGlue = 0f
                }
            }
        }
        if (current.isNotEmpty() || out.isEmpty() || pieces.lastOrNull() is HardBreak) emit(justify = false)
        return out
    }

    private fun split(word: Word, width: Float): List<Word> {
        val chunks = ArrayList<Word>()
        var boxes = ArrayList<Box>()
        var used = 0f
        for (box in word.boxes) {
            val scale = box.face.scale(box.style.size)
            var start = 0
            var chunkWidth = 0f
            for (k in box.glyphs.indices) {
                val w = box.face.font.advanceWidth(box.glyphs[k]) * scale
                if (used + chunkWidth + w > width && (used + chunkWidth) > 0f) {
                    if (k > start) boxes += slice(box, start, k, chunkWidth)
                    chunks += Word(boxes)
                    boxes = ArrayList()
                    used = 0f
                    start = k
                    chunkWidth = 0f
                }
                chunkWidth += w
            }
            if (box.glyphs.size > start) boxes += slice(box, start, box.glyphs.size, chunkWidth)
            used += chunkWidth
        }
        if (boxes.isNotEmpty()) chunks += Word(boxes)
        return chunks
    }

    private fun slice(box: Box, from: Int, to: Int, width: Float): Box =
        Box(box.face, box.style, box.glyphs.copyOfRange(from, to), IntArray(to - from), width)

    private fun build(boxes: List<Box>, xs: List<Float>, glueAt: List<Int>, natural: Float, width: Float, align: Align, spacing: Float, emptySize: Float): Line {
        var ascent = 0f
        var descent = 0f
        var size = 0f
        for (box in boxes) {
            val scale = box.face.scale(box.style.size)
            ascent = maxOf(ascent, box.face.font.ascent * scale + maxOf(0f, box.style.rise))
            descent = maxOf(descent, -box.face.font.descent * scale - minOf(0f, box.style.rise))
            size = maxOf(size, box.style.size)
        }
        if (boxes.isEmpty()) {
            return Line(emptyList(), FloatArray(0), emptySize * spacing, emptySize)
        }
        val height = maxOf(size * spacing, ascent + descent)
        val positions = FloatArray(boxes.size)
        val shift = when (align) {
            Align.CENTER -> (width - natural) / 2
            Align.END -> width - natural
            else -> 0f
        }
        val stretch = if (align == Align.JUSTIFY && glueAt.isNotEmpty()) (width - natural) / glueAt.size else 0f
        var g = 0
        for (k in boxes.indices) {
            while (g < glueAt.size && glueAt[g] <= k) g++
            positions[k] = xs[k] + shift + stretch * g
        }
        return Line(boxes.toList(), positions, height, ascent + (height - ascent - descent) / 2)
    }
}
