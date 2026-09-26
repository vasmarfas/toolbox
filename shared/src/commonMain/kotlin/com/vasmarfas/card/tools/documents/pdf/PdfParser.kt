package com.vasmarfas.card.tools.documents.pdf

private const val MAX_DEPTH = 100

internal class IndirectObject(val number: Int, val generation: Int, val value: PdfObject)

// with a missing or wrong /Length the data is cut at the next endstream, or at endobj if that comes first
internal class PdfParser(
    private val data: ByteArray,
    names: NameCache,
    start: Int = 0,
    end: Int = data.size,
    private val allowRefs: Boolean = true,
    private val lengthOf: (PdfRef) -> PdfObject? = { null },
) {
    val lexer = PdfLexer(data, names, start, end)

    fun parseObject(depth: Int = 0): PdfObject {
        if (depth > MAX_DEPTH) throw PdfException("Objects are nested too deeply")
        val lx = lexer
        lx.skipWhitespace()
        val c = lx.peek()
        return when {
            c < 0 -> PdfNull
            c == '/'.code -> PdfName(lx.readName())
            c == '('.code -> PdfString(lx.readLiteralString())
            c == '<'.code -> if (lx.peekAt(1) == '<'.code) parseDict(depth) else PdfString(lx.readHexString(), hex = true)
            c == '['.code -> parseArray(depth)
            lx.startsNumber(c) -> parseNumber()
            else -> parseKeyword()
        }
    }

    private fun parseNumber(): PdfObject {
        val lx = lexer
        if (!lx.readNumber()) {
            lx.readRegular()
            return PdfNull
        }
        if (!lx.isInteger) return PdfReal(lx.realValue)
        val first = lx.intValue
        if (allowRefs && first in 0..Int.MAX_VALUE.toLong()) {
            val save = lx.pos
            lx.skipWhitespace()
            if (lx.peek() in 48..57) {
                val generation = lx.readDigits()
                if (generation in 0..Int.MAX_VALUE.toLong()) {
                    lx.skipWhitespace()
                    if (lx.readKeyword("R")) return PdfRef(first.toInt(), generation.toInt())
                }
            }
            lx.pos = save
        }
        return PdfInt.of(first)
    }

    private fun parseKeyword(): PdfObject {
        val lx = lexer
        return when {
            lx.readKeyword("true") -> PdfBoolean.of(true)
            lx.readKeyword("false") -> PdfBoolean.of(false)
            lx.readKeyword("null") -> PdfNull
            else -> {
                lx.readRegular()
                PdfNull
            }
        }
    }

    private fun atObjectEnd(): Boolean {
        val lx = lexer
        return lx.atKeyword("endobj") || lx.atKeyword("stream") || lx.atKeyword("endstream") || lx.atKeyword("obj")
    }

    private fun parseDict(depth: Int): PdfDict {
        val lx = lexer
        lx.pos += 2
        val dict = PdfDict()
        while (true) {
            lx.skipWhitespace()
            val c = lx.peek()
            if (c < 0) break
            if (c == '>'.code) {
                lx.pos++
                if (lx.peek() == '>'.code) {
                    lx.pos++
                    break
                }
                continue
            }
            if (c == '/'.code) {
                val key = lx.readName()
                lx.skipWhitespace()
                val v = lx.peek()
                if (v == '>'.code && lx.peekAt(1) == '>'.code) continue
                if (isRegular(v) && !lx.startsNumber(v) && atObjectEnd()) break
                val value = parseObject(depth + 1)
                if (value !is PdfNull) dict[key] = value
                continue
            }
            if (isRegular(c) && !lx.startsNumber(c) && atObjectEnd()) break
            parseObject(depth + 1)
        }
        return dict
    }

    private fun parseArray(depth: Int): PdfArray {
        val lx = lexer
        lx.pos++
        val items = ArrayList<PdfObject>()
        while (true) {
            lx.skipWhitespace()
            val c = lx.peek()
            if (c < 0) break
            if (c == ']'.code) {
                lx.pos++
                break
            }
            if (c == '>'.code && lx.peekAt(1) == '>'.code) break
            if (isRegular(c) && !lx.startsNumber(c) && atObjectEnd()) break
            items.add(parseObject(depth + 1))
        }
        return PdfArray(items)
    }

    fun readIndirect(offset: Int): IndirectObject? {
        val lx = lexer
        if (offset < 0 || offset >= lx.end) return null
        lx.pos = offset
        lx.skipWhitespace()
        val number = lx.readDigits()
        if (number < 0 || number > Int.MAX_VALUE) return null
        lx.skipWhitespace()
        val generation = lx.readDigits()
        if (generation < 0 || generation > Int.MAX_VALUE) return null
        lx.skipWhitespace()
        if (!lx.readKeyword("obj")) return null
        val value = parseObject()
        if (value is PdfDict) {
            val save = lx.pos
            lx.skipWhitespace()
            if (lx.atKeyword("stream")) return IndirectObject(number.toInt(), generation.toInt(), readStream(value))
            lx.pos = save
        }
        return IndirectObject(number.toInt(), generation.toInt(), value)
    }

    private fun readStream(dict: PdfDict): PdfStream {
        val lx = lexer
        lx.pos += 6
        while (lx.pos < lx.end && data[lx.pos].toInt() == ' '.code) lx.pos++
        if (lx.peek() == 13) {
            lx.pos++
            if (lx.peek() == 10) lx.pos++
        } else if (lx.peek() == 10) {
            lx.pos++
        }
        val start = lx.pos
        val declared = when (val length = dict["Length"]) {
            is PdfRef -> lengthOf(length).asDouble()
            else -> length.asDouble()
        }?.toLong() ?: -1L
        val fits = declared >= 0 && start + declared <= lx.end
        var stop = -1
        if (fits && endsStreamAt((start + declared).toInt())) stop = (start + declared).toInt()
        if (stop < 0) stop = findStreamEnd(start)
        if (stop < 0) stop = if (fits) (start + declared).toInt() else lx.end
        lx.pos = stop
        lx.skipWhitespace()
        lx.readKeyword("endstream")
        return PdfStream(dict, data.copyOfRange(start, stop))
    }

    private fun endsStreamAt(at: Int): Boolean {
        var i = at
        while (i < lexer.end && isWhitespace(data[i].toInt())) i++
        return bytesAt(i, "endstream") || bytesAt(i, "endobj")
    }

    private fun bytesAt(at: Int, word: String): Boolean {
        if (at + word.length > lexer.end) return false
        for (k in word.indices) if (data[at + k].toInt() != word[k].code) return false
        return true
    }

    private fun findStreamEnd(start: Int): Int {
        val endstream = lexer.indexOf("endstream", start)
        val endobj = lexer.indexOf("endobj", start, if (endstream < 0) lexer.end else endstream)
        val at = if (endobj >= 0) endobj else endstream
        if (at < 0) return -1
        var stop = at
        if (stop > start && data[stop - 1].toInt() == 10) stop--
        if (stop > start && data[stop - 1].toInt() == 13) stop--
        return stop
    }
}
