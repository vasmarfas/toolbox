package com.vasmarfas.card.tools.documents.pdf

import kotlin.math.min

internal class CMap {
    private class Codespace(val length: Int, val low: IntArray, val high: IntArray) {
        fun matches(bytes: ByteArray, pos: Int): Boolean {
            for (k in 0 until length) {
                val b = bytes[pos + k].toInt() and 0xFF
                if (b < low[k] || b > high[k]) return false
            }
            return true
        }
    }

    private class UnicodeRange(val first: Long, val last: Long, val length: Int, val base: ByteArray)

    private class CidRange(val first: Long, val last: Long, val length: Int, val cid: Int)

    var name: String? = null
    var vertical = false
    var identity = false

    // predefined Uni*-UCS2 and Uni*-UTF16 CMaps, the code itself is UTF-16
    var unicodeCodes = false

    var code = 0L
        private set

    private val spaces = ArrayList<Codespace>()
    private var shortest = 1
    private val one = arrayOfNulls<String>(256)
    private val two = arrayOfNulls<Array<String?>>(256)
    private val wide = HashMap<Long, String>()
    private val ranges = ArrayList<UnicodeRange>()
    private val cids = HashMap<Long, Int>()
    private val cidRanges = ArrayList<CidRange>()

    val hasCodespaces: Boolean get() = spaces.isNotEmpty()

    fun addCodespace(low: ByteArray, high: ByteArray) {
        val length = min(low.size, high.size)
        if (length !in 1..4) return
        spaces.add(Codespace(length, IntArray(length) { low[it].toInt() and 0xFF }, IntArray(length) { high[it].toInt() and 0xFF }))
        shortest = spaces.minOf { it.length }
    }

    fun read(bytes: ByteArray, pos: Int): Int {
        val available = min(4, bytes.size - pos)
        var value = 0L
        for (length in 1..available) {
            value = (value shl 8) or (bytes[pos + length - 1].toLong() and 0xFF)
            for (space in spaces) {
                if (space.length == length && space.matches(bytes, pos)) {
                    code = value
                    return length
                }
            }
        }
        val length = min(shortest, available).coerceAtLeast(1)
        value = 0L
        for (k in 0 until length) value = (value shl 8) or (bytes[pos + k].toLong() and 0xFF)
        code = value
        return length
    }

    fun put(code: Long, length: Int, text: String) {
        when (length) {
            1 -> one[code.toInt() and 0xFF] = text
            2 -> {
                val high = (code shr 8).toInt() and 0xFF
                val page = two[high] ?: arrayOfNulls<String>(256).also { two[high] = it }
                page[code.toInt() and 0xFF] = text
            }
            else -> wide[code or (length.toLong() shl 32)] = text
        }
    }

    fun unicode(code: Long, length: Int): String? {
        val direct = when (length) {
            1 -> one[code.toInt() and 0xFF]
            2 -> two[(code shr 8).toInt() and 0xFF]?.get(code.toInt() and 0xFF)
            else -> wide[code or (length.toLong() shl 32)]
        }
        if (direct != null) return direct
        for (range in ranges) {
            if (range.length == length && code >= range.first && code <= range.last) return destination(offset(range.base, code - range.first))
        }
        if (unicodeCodes) {
            return if (length == 4) {
                charArrayOf((code ushr 16).toInt().toChar(), (code and 0xFFFF).toInt().toChar()).concatToString()
            } else {
                code.toInt().toChar().toString()
            }
        }
        return null
    }

    fun codeOf(text: String): Pair<Long, Int>? {
        for (c in 0 until 256) if (one[c] == text) return c.toLong() to 1
        for (high in 0 until 256) {
            val page = two[high] ?: continue
            for (low in 0 until 256) if (page[low] == text) return ((high shl 8) or low).toLong() to 2
        }
        return null
    }

    fun cid(code: Long, length: Int): Int {
        cids[code or (length.toLong() shl 32)]?.let { return it }
        for (range in cidRanges) {
            if (range.length == length && code >= range.first && code <= range.last) return range.cid + (code - range.first).toInt()
        }
        return if (identity) code.toInt() else -1
    }

    fun addRange(first: ByteArray, last: ByteArray, base: ByteArray) {
        val length = first.size
        val low = number(first)
        val high = number(last)
        if (high < low || length !in 1..4) return
        if (high - low < 256) {
            for (c in low..high) put(c, length, destination(offset(base, c - low)))
        } else {
            ranges.add(UnicodeRange(low, high, length, base))
        }
    }

    fun addCidRange(first: ByteArray, last: ByteArray, cid: Int) {
        val low = number(first)
        val high = number(last)
        if (high >= low && first.size in 1..4) cidRanges.add(CidRange(low, high, first.size, cid))
    }

    fun addCid(code: ByteArray, cid: Int) {
        if (code.size in 1..4) cids[number(code) or (code.size.toLong() shl 32)] = cid
    }

    fun inherit(parent: CMap) {
        if (spaces.isEmpty()) {
            spaces.addAll(parent.spaces)
            shortest = parent.shortest
        }
        identity = identity || parent.identity
        unicodeCodes = unicodeCodes || parent.unicodeCodes
        vertical = vertical || parent.vertical
        for ((key, value) in parent.cids) cids.getOrPut(key) { value }
        cidRanges.addAll(parent.cidRanges)
    }

    companion object {
        fun number(bytes: ByteArray): Long {
            var value = 0L
            for (b in bytes) value = (value shl 8) or (b.toLong() and 0xFF)
            return value
        }

        fun destination(bytes: ByteArray): String = when (bytes.size) {
            0 -> ""
            1 -> (bytes[0].toInt() and 0xFF).toChar().toString()
            else -> PdfEncodings.utf16(bytes, 0, bytes.size)
        }

        fun offset(base: ByteArray, delta: Long): ByteArray {
            if (delta == 0L) return base
            val out = base.copyOf()
            var carry = delta
            var i = out.size - 1
            while (carry != 0L && i >= 0) {
                val sum = (out[i].toLong() and 0xFF) + (carry and 0xFF)
                out[i] = sum.toByte()
                carry = (carry ushr 8) + (sum ushr 8)
                i--
            }
            return out
        }

        fun predefined(name: String): CMap? {
            val cmap = CMap()
            cmap.name = name
            cmap.vertical = name.endsWith("-V")
            when {
                name == "Identity-H" || name == "Identity-V" -> {
                    cmap.addCodespace(byteArrayOf(0, 0), byteArrayOf(-1, -1))
                    cmap.identity = true
                }
                name.startsWith("Uni") && "UCS2" in name -> {
                    cmap.addCodespace(byteArrayOf(0, 0), byteArrayOf(-1, -1))
                    cmap.unicodeCodes = true
                }
                name.startsWith("Uni") && "UTF16" in name -> {
                    cmap.addCodespace(byteArrayOf(0, 0), byteArrayOf(0xD7.toByte(), -1))
                    cmap.addCodespace(byteArrayOf(0xE0.toByte(), 0), byteArrayOf(-1, -1))
                    cmap.addCodespace(byteArrayOf(0xD8.toByte(), 0, 0xDC.toByte(), 0), byteArrayOf(0xDB.toByte(), -1, 0xDF.toByte(), -1))
                    cmap.unicodeCodes = true
                }
                else -> return null
            }
            return cmap
        }

        fun parse(data: ByteArray, names: NameCache, resolveParent: (String) -> CMap? = { predefined(it) }): CMap {
            val cmap = CMap()
            val parser = PdfParser(data, names, allowRefs = false)
            val lexer = parser.lexer
            val operands = ArrayList<PdfObject>()
            while (true) {
                lexer.skipWhitespace()
                val c = lexer.peek()
                if (c < 0) break
                if (!isRegular(c) || lexer.startsNumber(c)) {
                    operands.add(parser.parseObject())
                    if (operands.size > 300_000) operands.clear()
                    continue
                }
                when (lexer.readRegular()) {
                    "endcodespacerange" -> for (i in 0 until operands.size - 1 step 2) {
                        val low = operands[i] as? PdfString ?: continue
                        val high = operands[i + 1] as? PdfString ?: continue
                        cmap.addCodespace(low.bytes, high.bytes)
                    }
                    "endbfchar" -> for (i in 0 until operands.size - 1 step 2) {
                        val source = operands[i] as? PdfString ?: continue
                        val text = when (val target = operands[i + 1]) {
                            is PdfString -> destination(target.bytes)
                            is PdfName -> PdfEncodings.glyphToUnicode(target.name)
                            else -> null
                        } ?: continue
                        if (source.bytes.size in 1..4) cmap.put(number(source.bytes), source.bytes.size, text)
                    }
                    "endbfrange" -> for (i in 0 until operands.size - 2 step 3) {
                        val first = operands[i] as? PdfString ?: continue
                        val last = operands[i + 1] as? PdfString ?: continue
                        when (val target = operands[i + 2]) {
                            is PdfString -> cmap.addRange(first.bytes, last.bytes, target.bytes)
                            is PdfArray -> {
                                val low = number(first.bytes)
                                for (k in target.items.indices) {
                                    val item = target.items[k] as? PdfString ?: continue
                                    if (first.bytes.size in 1..4) cmap.put(low + k, first.bytes.size, destination(item.bytes))
                                }
                            }
                            else -> Unit
                        }
                    }
                    "endcidchar" -> for (i in 0 until operands.size - 1 step 2) {
                        val source = operands[i] as? PdfString ?: continue
                        cmap.addCid(source.bytes, operands[i + 1].asInt() ?: continue)
                    }
                    "endcidrange" -> for (i in 0 until operands.size - 2 step 3) {
                        val first = operands[i] as? PdfString ?: continue
                        val last = operands[i + 1] as? PdfString ?: continue
                        cmap.addCidRange(first.bytes, last.bytes, operands[i + 2].asInt() ?: continue)
                    }
                    "usecmap" -> (operands.lastOrNull() as? PdfName)?.let { parent -> resolveParent(parent.name)?.let { cmap.inherit(it) } }
                    "def" -> if (operands.size >= 2) {
                        when ((operands[operands.size - 2] as? PdfName)?.name) {
                            "WMode" -> cmap.vertical = operands.last().asInt() == 1
                            "CMapName" -> cmap.name = (operands.last() as? PdfName)?.name
                        }
                    }
                }
                operands.clear()
            }
            return cmap
        }
    }
}
