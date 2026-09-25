package com.vasmarfas.card.tools.documents.pdf

internal abstract class PdfFont {
    abstract val vertical: Boolean

    // NaN when the font has no space glyph
    abstract val spaceWidth: Double

    var code = 0L
        protected set

    var style = FontStyle(bold = false, italic = false, serif = false, mono = false)
        internal set

    abstract fun read(bytes: ByteArray, pos: Int): Int

    abstract fun text(code: Long, length: Int): String?

    abstract fun width(code: Long, length: Int): Double

    open fun advanceY(code: Long): Double = -1.0
}

private class SimpleFont(private val texts: Array<String?>, private val widths: DoubleArray) : PdfFont() {
    override val vertical = false

    override val spaceWidth: Double = (if (texts[32] == " ") 32 else texts.indexOf(" ")).let { if (it >= 0) widths[it] else Double.NaN }

    override fun read(bytes: ByteArray, pos: Int): Int {
        code = (bytes[pos].toInt() and 0xFF).toLong()
        return 1
    }

    override fun text(code: Long, length: Int) = texts[code.toInt() and 0xFF]

    override fun width(code: Long, length: Int) = widths[code.toInt() and 0xFF]
}

private class CompositeFont(
    private val encoding: CMap?,
    private val toUnicode: CMap?,
    private val widths: CidWidths,
    private val verticalAdvance: Double,
    override val vertical: Boolean,
) : PdfFont() {
    private val splitter = if (encoding?.hasCodespaces == true) encoding else if (toUnicode?.hasCodespaces == true) toUnicode else null

    override val spaceWidth: Double = toUnicode?.codeOf(" ")?.let { (code, length) -> width(code, length) } ?: Double.NaN

    override fun read(bytes: ByteArray, pos: Int): Int {
        if (splitter != null) {
            val length = splitter.read(bytes, pos)
            code = splitter.code
            return length
        }
        if (pos + 1 < bytes.size) {
            code = (((bytes[pos].toInt() and 0xFF) shl 8) or (bytes[pos + 1].toInt() and 0xFF)).toLong()
            return 2
        }
        code = (bytes[pos].toInt() and 0xFF).toLong()
        return 1
    }

    override fun text(code: Long, length: Int): String? {
        toUnicode?.unicode(code, length)?.let { return it }
        if (encoding?.unicodeCodes == true) return encoding.unicode(code, length)
        return null
    }

    override fun width(code: Long, length: Int): Double {
        val cid = encoding?.cid(code, length) ?: code.toInt()
        return widths.width(cid) / 1000.0
    }

    override fun advanceY(code: Long): Double = verticalAdvance / 1000.0
}

private class CidWidths(array: PdfArray?, doc: PdfDocument, private val default: Double) {
    private val starts: IntArray
    private val ends: IntArray
    private val values: Array<DoubleArray?>
    private val constants: DoubleArray

    init {
        val s = IntList()
        val e = IntList()
        val v = ArrayList<DoubleArray?>()
        val c = ArrayList<Double>()
        if (array != null) {
            var i = 0
            while (i < array.size) {
                val first = array.resolved(i, doc).asInt() ?: break
                val next = array.resolved(i + 1, doc)
                if (next is PdfArray) {
                    val list = next.numbers(doc) ?: DoubleArray(0)
                    if (list.isNotEmpty()) {
                        s.add(first)
                        e.add(first + list.size - 1)
                        v.add(list)
                        c.add(0.0)
                    }
                    i += 2
                } else {
                    val last = next.asInt() ?: break
                    val width = array.resolved(i + 2, doc).asDouble() ?: break
                    s.add(first)
                    e.add(last)
                    v.add(null)
                    c.add(width)
                    i += 3
                }
            }
        }
        val order = (0 until s.size).sortedBy { s[it] }
        starts = IntArray(order.size) { s[order[it]] }
        ends = IntArray(order.size) { e[order[it]] }
        values = Array(order.size) { v[order[it]] }
        constants = DoubleArray(order.size) { c[order[it]] }
    }

    fun width(cid: Int): Double {
        if (cid < 0) return default
        var low = 0
        var high = starts.size - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            when {
                starts[mid] > cid -> high = mid - 1
                ends[mid] < cid -> low = mid + 1
                else -> return values[mid]?.get(cid - starts[mid]) ?: constants[mid]
            }
        }
        return default
    }
}

internal object PdfFonts {
    fun load(doc: PdfDocument, entry: PdfObject?): PdfFont? {
        val dict = doc.resolve(entry) as? PdfDict ?: return null
        doc.fontCache[dict]?.let { return it }
        val font = if (dict.name("Subtype", doc) == "Type0") composite(doc, dict) else simple(doc, dict)
        font.style = style(doc, dict)
        doc.fontCache[dict] = font
        return font
    }

    private fun style(doc: PdfDocument, dict: PdfDict): FontStyle {
        val descendant = if (dict.name("Subtype", doc) == "Type0") dict.array("DescendantFonts", doc)?.resolved(0, doc) as? PdfDict else null
        val descriptor = (descendant ?: dict).dict("FontDescriptor", doc)
        val name = dict.name("BaseFont", doc)?.lowercase().orEmpty()
        val flags = descriptor?.int("Flags", doc) ?: 0
        val weight = descriptor?.number("FontWeight", doc) ?: 0.0
        val mono = flags and 1 != 0 || "courier" in name || "mono" in name || "consol" in name
        return FontStyle(
            bold = weight >= 600 || flags and (1 shl 18) != 0 || listOf("bold", "black", "heavy", "semibold", "demi").any { it in name },
            italic = flags and (1 shl 6) != 0 || "italic" in name || "oblique" in name,
            serif = !mono && (flags and 2 != 0 || listOf("times", "georgia", "garamond", "cambria", "roman").any { it in name } || ("serif" in name && "sans" !in name)),
            mono = mono,
        )
    }

    private fun toUnicode(doc: PdfDocument, dict: PdfDict): CMap? {
        val stream = dict.stream("ToUnicode", doc) ?: return null
        return try {
            CMap.parse(doc.decodedStream(stream), doc.names)
        } catch (_: PdfException) {
            null
        }
    }

    private fun composite(doc: PdfDocument, dict: PdfDict): PdfFont {
        val descendant = dict.array("DescendantFonts", doc)?.resolved(0, doc) as? PdfDict
        val encoding = when (val value = dict.resolved("Encoding", doc)) {
            is PdfName -> CMap.predefined(value.name)
            is PdfStream -> embeddedCMap(doc, value, 0)
            else -> null
        }
        val widths = CidWidths(descendant?.array("W", doc), doc, descendant?.number("DW", doc) ?: 1000.0)
        val verticalAdvance = descendant?.array("DW2", doc)?.numbers(doc)?.getOrNull(1) ?: -1000.0
        return CompositeFont(encoding, toUnicode(doc, dict), widths, verticalAdvance, encoding?.vertical == true)
    }

    private fun embeddedCMap(doc: PdfDocument, stream: PdfStream, depth: Int): CMap? {
        val data = try {
            doc.decodedStream(stream)
        } catch (_: PdfException) {
            return null
        }
        val cmap = CMap.parse(data, doc.names)
        val parent = when (val use = stream.dict.resolved("UseCMap", doc)) {
            is PdfName -> CMap.predefined(use.name)
            is PdfStream -> if (depth < 4) embeddedCMap(doc, use, depth + 1) else null
            else -> null
        }
        if (parent != null) cmap.inherit(parent)
        return cmap
    }

    private fun simple(doc: PdfDocument, dict: PdfDict): PdfFont {
        val subtype = dict.name("Subtype", doc)
        val baseFont = dict.name("BaseFont", doc)?.let { stripSubset(it) }
        val standard = Standard14.family(baseFont)
        val builtIn = when {
            standard == "Symbol" -> PdfEncodings.symbol
            standard == "ZapfDingbats" -> PdfEncodings.zapfDingbats
            subtype == "TrueType" -> PdfEncodings.winAnsi
            else -> PdfEncodings.standard
        }
        val texts = arrayOfNulls<String>(256)
        val encoding = dict.resolved("Encoding", doc)
        val base = when (encoding) {
            is PdfName -> PdfEncodings.byName(encoding.name) ?: builtIn
            is PdfDict -> PdfEncodings.byName(encoding.name("BaseEncoding", doc)) ?: if (subtype == "TrueType") PdfEncodings.standard else builtIn
            else -> builtIn
        }
        for (c in 0 until 256) if (base[c] != '\u0000') texts[c] = base[c].toString()
        if (encoding is PdfDict) applyDifferences(doc, encoding.array("Differences", doc), texts)
        val unicode = toUnicode(doc, dict)
        if (unicode != null) {
            for (c in 0 until 256) (unicode.unicode(c.toLong(), 1) ?: unicode.unicode(c.toLong(), 2))?.let { texts[c] = it }
        }
        return SimpleFont(texts, simpleWidths(doc, dict, subtype, standard, texts))
    }

    private fun applyDifferences(doc: PdfDocument, differences: PdfArray?, texts: Array<String?>) {
        if (differences == null) return
        var code = 0
        for (item in differences.items) {
            when (val value = doc.resolve(item)) {
                is PdfInt, is PdfReal -> code = value.asInt() ?: code
                is PdfName -> {
                    if (code in 0..255) texts[code] = PdfEncodings.glyphToUnicode(value.name)
                    code++
                }
                else -> Unit
            }
        }
    }

    private fun simpleWidths(doc: PdfDocument, dict: PdfDict, subtype: String?, standard: String?, texts: Array<String?>): DoubleArray {
        val scale = if (subtype == "Type3") dict.array("FontMatrix", doc)?.numbers(doc)?.getOrNull(0) ?: 0.001 else 0.001
        val widths = DoubleArray(256) { Double.NaN }
        val list = dict.array("Widths", doc)
        if (list != null) {
            val first = dict.int("FirstChar", doc) ?: 0
            for (i in 0 until list.size) {
                val code = first + i
                if (code in 0..255) list.resolved(i, doc).asDouble()?.let { widths[code] = it * scale }
            }
        }
        val missing = dict.dict("FontDescriptor", doc)?.number("MissingWidth", doc)
        for (code in 0 until 256) {
            if (!widths[code].isNaN()) continue
            widths[code] = when {
                list != null -> (missing ?: 0.0) * scale
                standard != null -> (Standard14.width(standard, texts[code]) ?: missing ?: 500.0) / 1000.0
                else -> (missing ?: 500.0) * scale
            }
        }
        return widths
    }

    private fun stripSubset(name: String): String {
        val plus = name.indexOf('+')
        return if (plus == 6 && name.substring(0, 6).all { it in 'A'..'Z' }) name.substring(7) else name
    }
}

internal object Standard14 {
    private const val HELVETICA =
        "278 278 355 556 556 889 667 191 333 333 389 584 278 333 278 278 556 556 556 556 556 556 556 556 556 556 278 278 584 584 584 556 1015 " +
            "667 667 722 722 667 611 778 722 278 500 667 556 833 722 778 667 778 722 667 611 722 667 944 667 667 611 278 278 278 469 556 333 " +
            "556 556 500 556 556 278 556 556 222 222 500 222 833 556 556 556 556 333 500 278 556 500 722 500 500 500 334 260 334 584"
    private const val HELVETICA_BOLD =
        "278 333 474 556 556 889 722 238 333 333 389 584 278 333 278 278 556 556 556 556 556 556 556 556 556 556 333 333 584 584 584 611 975 " +
            "722 722 722 722 667 611 778 722 278 556 722 611 833 722 778 667 778 722 667 611 722 667 944 667 667 611 333 278 333 584 556 333 " +
            "556 611 556 611 556 333 611 611 278 278 556 278 889 611 611 611 611 389 556 333 611 556 778 556 556 500 389 280 389 584"
    private const val TIMES =
        "250 333 408 500 500 833 778 180 333 333 500 564 250 333 250 278 500 500 500 500 500 500 500 500 500 500 278 278 564 564 564 444 921 " +
            "722 667 667 722 611 556 722 722 333 389 722 611 889 722 722 556 722 667 556 611 722 722 944 722 722 611 333 278 333 469 500 333 " +
            "444 500 444 500 444 333 500 500 278 278 500 278 778 500 500 500 500 333 389 278 500 500 722 500 500 444 480 200 480 541"
    private const val TIMES_BOLD =
        "250 333 555 500 500 1000 833 278 333 333 500 570 250 333 250 278 500 500 500 500 500 500 500 500 500 500 333 333 570 570 570 500 930 " +
            "722 667 722 722 667 611 778 778 389 500 778 667 944 722 778 611 778 722 556 667 722 722 1000 722 722 667 333 278 333 581 500 333 " +
            "500 556 444 556 444 333 500 556 278 333 556 278 833 556 500 556 556 444 389 333 556 500 722 500 500 444 394 220 394 520"
    private const val TIMES_ITALIC =
        "250 333 420 500 500 833 778 214 333 333 500 675 250 333 250 278 500 500 500 500 500 500 500 500 500 500 333 333 675 675 675 500 920 " +
            "611 611 667 722 611 611 722 722 333 444 667 556 833 667 722 611 722 611 500 556 722 611 833 611 556 556 389 278 389 422 500 333 " +
            "500 500 444 500 444 278 500 500 278 278 444 278 722 500 500 500 500 389 389 278 500 444 667 444 444 389 400 275 400 541"
    private const val TIMES_BOLD_ITALIC =
        "250 389 555 500 500 833 778 278 333 333 500 570 250 333 250 278 500 500 500 500 500 500 500 500 500 500 333 333 570 570 570 500 832 " +
            "667 667 667 722 667 667 722 778 389 500 667 611 889 722 722 611 722 667 556 611 722 667 889 667 611 611 333 278 333 570 500 333 " +
            "500 500 444 500 444 333 500 556 278 278 500 278 778 556 500 500 500 389 389 278 556 444 667 500 444 389 348 220 348 570"

    // base letters of U+00C0..U+00FF, accented Latin-1 letters take the width of the plain one
    private const val LATIN1_BASE = "AAAAAAACEEEEIIIIDNOOOOO+OUUUUYPbaaaaaaaceeeeiiiionooooo+ouuuuypy"

    private val tables: Map<String, IntArray> = mapOf(
        "Helvetica" to HELVETICA,
        "Helvetica-Bold" to HELVETICA_BOLD,
        "Times-Roman" to TIMES,
        "Times-Bold" to TIMES_BOLD,
        "Times-Italic" to TIMES_ITALIC,
        "Times-BoldItalic" to TIMES_BOLD_ITALIC,
    ).mapValues { (_, widths) -> widths.split(' ').map { it.toInt() }.toIntArray() }

    fun family(baseFont: String?): String? {
        val name = baseFont?.lowercase() ?: return null
        val bold = "bold" in name || "black" in name || "heavy" in name
        val italic = "italic" in name || "oblique" in name
        return when {
            "courier" in name -> "Courier"
            "symbol" in name -> "Symbol"
            "zapf" in name || "dingbats" in name -> "ZapfDingbats"
            "times" in name -> when {
                bold && italic -> "Times-BoldItalic"
                bold -> "Times-Bold"
                italic -> "Times-Italic"
                else -> "Times-Roman"
            }
            "helvetica" in name || "arial" in name -> if (bold) "Helvetica-Bold" else "Helvetica"
            else -> null
        }
    }

    fun width(family: String, text: String?): Double? {
        if (family == "Courier") return 600.0
        val table = tables[family] ?: return null
        val c = text?.singleOrNull() ?: return null
        val ascii = when (c.code) {
            in 32..126 -> c.code
            in 0xC0..0xFF -> LATIN1_BASE[c.code - 0xC0].code
            0xA0 -> 32
            0x2018, 0x2019, 0x201A -> 39
            0x201C, 0x201D, 0x201E -> 34
            0x2013 -> 45
            else -> return table['n'.code - 32].toDouble()
        }
        return table[ascii - 32].toDouble()
    }
}
