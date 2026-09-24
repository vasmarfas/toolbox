package com.vasmarfas.card.tools.documents.pdf

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt

// a baseline shift over half the font size starts a new line, a gap wider than half the space glyph
// (clamped to 0.1..0.3 of the font size) inserts a space
object PdfText {
    fun extract(document: PdfDocument, pageIndex: Int): String {
        val page = document.page(pageIndex)
        val lines = TextLines()
        ContentInterpreter(document, lines).run(page)
        return lines.result()
    }
}

private class Matrix(val a: Double, val b: Double, val c: Double, val d: Double, val e: Double, val f: Double) {
    operator fun times(o: Matrix) = Matrix(
        a * o.a + b * o.c,
        a * o.b + b * o.d,
        c * o.a + d * o.c,
        c * o.b + d * o.d,
        e * o.a + f * o.c + o.e,
        e * o.b + f * o.d + o.f,
    )

    fun translate(tx: Double, ty: Double) = Matrix(a, b, c, d, tx * a + ty * c + e, tx * b + ty * d + f)

    companion object {
        val IDENTITY = Matrix(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
    }
}

private class GraphicsState(
    var ctm: Matrix = Matrix.IDENTITY,
    var font: PdfFont? = null,
    var fontSize: Double = 0.0,
    var charSpacing: Double = 0.0,
    var wordSpacing: Double = 0.0,
    var scale: Double = 1.0,
    var leading: Double = 0.0,
    var rise: Double = 0.0,
) {
    fun copy() = GraphicsState(ctm, font, fontSize, charSpacing, wordSpacing, scale, leading, rise)
}

private class TextLines {
    private val out = StringBuilder()
    private var started = false
    private var endX = 0.0
    private var endY = 0.0
    private var dirX = 1.0
    private var dirY = 0.0
    private var size = 0.0
    private var lastSpace = 0.0
    private val shown = HashMap<Long, Points>()

    fun glyph(text: String?, startX: Double, startY: Double, stopX: Double, stopY: Double, fontSize: Double, spaceWidth: Double, dx: Double, dy: Double) {
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

private val operators = setOf(
    "b", "B", "b*", "B*", "BDC", "BI", "BMC", "BT", "BX", "c", "cm", "CS", "cs", "d", "d0", "d1", "Do", "DP", "EI", "EMC", "ET", "EX",
    "f", "F", "f*", "G", "g", "gs", "h", "i", "ID", "j", "J", "K", "k", "l", "m", "M", "MP", "n", "q", "Q", "re", "RG", "rg", "ri", "s",
    "S", "SC", "sc", "SCN", "scn", "sh", "T*", "Tc", "Td", "TD", "Tf", "Tj", "TJ", "TL", "Tm", "Tr", "Ts", "Tw", "Tz", "v", "w", "W", "W*",
    "y", "'", "\"",
)

private class ContentInterpreter(private val doc: PdfDocument, private val lines: TextLines) {
    private var state = GraphicsState()
    private val saved = ArrayList<GraphicsState>()
    private var tm = Matrix.IDENTITY
    private var tlm = Matrix.IDENTITY
    private val forms = ArrayList<PdfStream>()

    fun run(page: PdfPage) {
        val contents = when (val value = doc.resolve(page.dict["Contents"])) {
            is PdfStream -> listOf(value)
            is PdfArray -> value.items.mapNotNull { doc.resolve(it) as? PdfStream }
            else -> emptyList()
        }
        val data = if (contents.size == 1) {
            decode(contents[0])
        } else {
            val sink = ByteSink()
            for (stream in contents) {
                sink.write(decode(stream))
                sink.write('\n'.code)
            }
            sink.toByteArray()
        }
        execute(data, page.resources)
    }

    private fun decode(stream: PdfStream): ByteArray = try {
        doc.decodedStream(stream)
    } catch (_: PdfException) {
        ByteArray(0)
    }

    private fun execute(data: ByteArray, resources: PdfDict) {
        val parser = PdfParser(data, doc.names, allowRefs = false)
        val lexer = parser.lexer
        val operands = ArrayList<PdfObject>()
        while (true) {
            lexer.skipWhitespace()
            val c = lexer.peek()
            if (c < 0) break
            if (c == '{'.code || c == '}'.code) {
                lexer.pos++
                continue
            }
            if (!isRegular(c) || lexer.startsNumber(c)) {
                operands.add(parser.parseObject())
                if (operands.size > 10_000) operands.clear()
                continue
            }
            when (val op = lexer.readRegular()) {
                "true" -> operands.add(PdfBoolean.TRUE)
                "false" -> operands.add(PdfBoolean.FALSE)
                "null" -> operands.add(PdfNull)
                "BI" -> {
                    skipInlineImage(parser)
                    operands.clear()
                }
                else -> {
                    operator(op, operands, resources)
                    operands.clear()
                }
            }
        }
    }

    private fun number(args: List<PdfObject>, fromEnd: Int): Double = args.getOrNull(args.size - 1 - fromEnd).asDouble() ?: 0.0

    private fun matrix(args: List<PdfObject>): Matrix? {
        if (args.size < 6) return null
        val v = DoubleArray(6) { args[args.size - 6 + it].asDouble() ?: return null }
        return Matrix(v[0], v[1], v[2], v[3], v[4], v[5])
    }

    private fun operator(op: String, args: List<PdfObject>, resources: PdfDict) {
        when (op) {
            "q" -> saved.add(state.copy())
            "Q" -> if (saved.isNotEmpty()) state = saved.removeAt(saved.size - 1)
            "cm" -> matrix(args)?.let { state.ctm = it * state.ctm }
            "BT" -> {
                tm = Matrix.IDENTITY
                tlm = Matrix.IDENTITY
            }
            "Tf" -> {
                state.font = font(resources, args.getOrNull(args.size - 2) as? PdfName)
                state.fontSize = number(args, 0)
            }
            "Td" -> moveText(number(args, 1), number(args, 0))
            "TD" -> {
                state.leading = -number(args, 0)
                moveText(number(args, 1), number(args, 0))
            }
            "Tm" -> matrix(args)?.let {
                tm = it
                tlm = it
            }
            "T*" -> moveText(0.0, -state.leading)
            "TL" -> state.leading = number(args, 0)
            "Tc" -> state.charSpacing = number(args, 0)
            "Tw" -> state.wordSpacing = number(args, 0)
            "Tz" -> state.scale = number(args, 0) / 100
            "Ts" -> state.rise = number(args, 0)
            "Tj" -> (args.lastOrNull() as? PdfString)?.let { show(it.bytes) }
            "TJ" -> (args.lastOrNull() as? PdfArray)?.let { showArray(it) }
            "'" -> {
                moveText(0.0, -state.leading)
                (args.lastOrNull() as? PdfString)?.let { show(it.bytes) }
            }
            "\"" -> {
                state.wordSpacing = number(args, 2)
                state.charSpacing = number(args, 1)
                moveText(0.0, -state.leading)
                (args.lastOrNull() as? PdfString)?.let { show(it.bytes) }
            }
            "Do" -> (args.lastOrNull() as? PdfName)?.let { form(resources, it.name) }
        }
    }

    private fun moveText(tx: Double, ty: Double) {
        tlm = tlm.translate(tx, ty)
        tm = tlm
    }

    private fun font(resources: PdfDict, name: PdfName?): PdfFont? {
        if (name == null) return null
        val entry = resources.dict("Font", doc)?.get(name.name) ?: return null
        return try {
            PdfFonts.load(doc, entry)
        } catch (_: PdfException) {
            null
        }
    }

    private fun show(bytes: ByteArray) {
        val font = state.font ?: return
        val size = state.fontSize
        val scale = state.scale
        val rise = state.rise
        val ctm = state.ctm
        var pos = 0
        while (pos < bytes.size) {
            val length = font.read(bytes, pos)
            val code = font.code
            pos += length
            val w0 = font.width(code, length)
            val spacing = state.charSpacing + if (length == 1 && code == 32L) state.wordSpacing else 0.0
            val m = tm * ctm
            val unit = sqrt(m.c * m.c + m.d * m.d)
            val userSize = abs(size) * (if (unit > 0) unit else 1.0)
            val startX = rise * m.c + m.e
            val startY = rise * m.d + m.f
            val space = (if (font.spaceWidth > 0) font.spaceWidth else 0.25) * userSize * scale
            if (font.vertical) {
                val advance = font.advanceY(code) * size + spacing
                val len = if (unit > 0) unit else 1.0
                lines.glyph(font.text(code, length), startX, startY, startX + advance * m.c, startY + advance * m.d, userSize, space, -m.c / len, -m.d / len)
                tm = tm.translate(0.0, advance)
            } else {
                val width = (w0 * size + max(state.charSpacing, 0.0)) * scale
                val len = sqrt(m.a * m.a + m.b * m.b).let { if (it > 0) it else 1.0 }
                lines.glyph(font.text(code, length), startX, startY, startX + width * m.a, startY + width * m.b, userSize, space, m.a / len, m.b / len)
                tm = tm.translate((w0 * size + spacing) * scale, 0.0)
            }
        }
    }

    private fun showArray(array: PdfArray) {
        for (item in array.items) {
            when (item) {
                is PdfString -> show(item.bytes)
                is PdfInt, is PdfReal -> {
                    val adjust = (item.asDouble() ?: 0.0) / 1000 * state.fontSize
                    tm = if (state.font?.vertical == true) tm.translate(0.0, -adjust) else tm.translate(-adjust * state.scale, 0.0)
                }
                else -> Unit
            }
        }
    }

    private fun form(resources: PdfDict, name: String) {
        val stream = resources.dict("XObject", doc)?.resolved(name, doc) as? PdfStream ?: return
        if (stream.dict.name("Subtype", doc) != "Form" || forms.size >= 12 || forms.any { it === stream }) return
        val data = decode(stream)
        val outer = state.copy()
        val depth = saved.size
        val outerTm = tm
        val outerTlm = tlm
        forms.add(stream)
        stream.dict.array("Matrix", doc)?.let { matrix(it.items) }?.let { state.ctm = it * state.ctm }
        execute(data, stream.dict.dict("Resources", doc) ?: resources)
        forms.removeAt(forms.size - 1)
        state = outer
        while (saved.size > depth) saved.removeAt(saved.size - 1)
        tm = outerTm
        tlm = outerTlm
    }

    private fun skipInlineImage(parser: PdfParser) {
        val lexer = parser.lexer
        val dict = PdfDict()
        while (true) {
            lexer.skipWhitespace()
            val c = lexer.peek()
            if (c < 0) return
            if (c == '/'.code) {
                val key = lexer.readName()
                dict[key] = parser.parseObject()
                continue
            }
            if (lexer.readKeyword("ID")) break
            parser.parseObject()
        }
        if (lexer.pos < lexer.end && isWhitespace(lexer.peek())) lexer.pos++
        lexer.pos = inlineImageEnd(lexer, dict)
    }

    private fun inlineImageEnd(lexer: PdfLexer, dict: PdfDict): Int {
        val data = lexer.data
        val start = lexer.pos
        val end = lexer.end
        val filters = PdfFilters.filterNames(dict, null)
        val known = when {
            filters.isEmpty() -> rawImageLength(dict).let { if (it >= 0 && start + it <= end) (start + it).toInt() else -1 }
            filters[0] == "ASCIIHexDecode" || filters[0] == "AHx" -> lexer.indexOf(">", start).let { if (it >= 0) it + 1 else -1 }
            filters[0] == "ASCII85Decode" || filters[0] == "A85" -> lexer.indexOf("~>", start).let { if (it >= 0) it + 2 else -1 }
            else -> -1
        }
        if (known >= 0) {
            var i = known
            while (i < end && isWhitespace(data[i].toInt())) i++
            if (lexer.matchesAt(i, "EI")) return i + 2
        }
        var from = start
        while (true) {
            val at = lexer.indexOf("EI", from)
            if (at < 0) return end
            val before = at == start || isWhitespace(data[at - 1].toInt())
            if (before && lexer.matchesAt(at, "EI") && plausibleContent(data, at + 2, end)) return at + 2
            from = at + 1
        }
    }

    private fun rawImageLength(dict: PdfDict): Long {
        val width = (dict["W"] ?: dict["Width"]).asInt() ?: return -1
        val height = (dict["H"] ?: dict["Height"]).asInt() ?: return -1
        val mask = (dict["IM"] ?: dict["ImageMask"]) == PdfBoolean.TRUE
        val bits = if (mask) 1 else (dict["BPC"] ?: dict["BitsPerComponent"]).asInt() ?: 8
        val components = if (mask) {
            1
        } else {
            when (val space = dict["CS"] ?: dict["ColorSpace"]) {
                is PdfName -> when (space.name) {
                    "G", "DeviceGray", "CalGray", "I", "Indexed" -> 1
                    "RGB", "DeviceRGB", "CalRGB" -> 3
                    "CMYK", "DeviceCMYK" -> 4
                    else -> return -1
                }
                is PdfArray -> if ((space.items.firstOrNull() as? PdfName)?.name in setOf("I", "Indexed")) 1 else return -1
                else -> return -1
            }
        }
        return height.toLong() * ((width.toLong() * components * bits + 7) / 8)
    }

    // after a candidate EI the stream must go on with ordinary operands and a known operator, not binary data
    private fun plausibleContent(data: ByteArray, from: Int, end: Int): Boolean {
        val lexer = PdfLexer(data, doc.names, from, end)
        repeat(8) {
            lexer.skipWhitespace()
            val c = lexer.peek()
            if (c < 0) return true
            if (c < 0x20 || c > 0x7E) return false
            when {
                lexer.startsNumber(c) -> if (!lexer.readNumber()) return false
                c == '/'.code -> lexer.readName()
                c == '('.code -> lexer.readLiteralString()
                c == '<'.code -> if (lexer.peekAt(1) == '<'.code) lexer.pos += 2 else lexer.readHexString()
                isRegular(c) -> return lexer.readRegular() in operators
                else -> lexer.pos++
            }
        }
        return true
    }
}
