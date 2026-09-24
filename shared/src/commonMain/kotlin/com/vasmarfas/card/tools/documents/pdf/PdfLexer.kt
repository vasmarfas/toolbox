package com.vasmarfas.card.tools.documents.pdf

import kotlin.math.pow

private const val WHITESPACE: Byte = 1
private const val DELIMITER: Byte = 2

private val charClass = ByteArray(256).apply {
    for (c in intArrayOf(0, 9, 10, 12, 13, 32)) this[c] = WHITESPACE
    for (c in "()<>[]{}/%") this[c.code] = DELIMITER
}

internal fun isWhitespace(b: Int) = charClass[b and 0xFF] == WHITESPACE

internal fun isDelimiter(b: Int) = charClass[b and 0xFF] == DELIMITER

internal fun isRegular(b: Int) = charClass[b and 0xFF].toInt() == 0

internal fun hexValue(b: Int): Int = when (b) {
    in 48..57 -> b - 48
    in 65..70 -> b - 55
    in 97..102 -> b - 87
    else -> -1
}

internal fun latin1(bytes: ByteArray, start: Int = 0, end: Int = bytes.size): String {
    val chars = CharArray(end - start) { (bytes[start + it].toInt() and 0xFF).toChar() }
    return chars.concatToString()
}

private val powersOfTen = DoubleArray(23) { 10.0.pow(it) }

internal fun powerOfTen(n: Int): Double = if (n < powersOfTen.size) powersOfTen[n] else 10.0.pow(n)

internal class ByteSink(capacity: Int = 256) {
    private var buf = ByteArray(maxOf(16, capacity))
    var size = 0
        private set

    fun write(b: Int) {
        if (size == buf.size) grow(1)
        buf[size++] = b.toByte()
    }

    fun write(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size - offset) {
        if (length <= 0) return
        if (size + length > buf.size) grow(length)
        bytes.copyInto(buf, size, offset, offset + length)
        size += length
    }

    fun writeAscii(text: String) {
        if (size + text.length > buf.size) grow(text.length)
        for (c in text) buf[size++] = c.code.toByte()
    }

    fun last(): Int = if (size == 0) -1 else buf[size - 1].toInt() and 0xFF

    fun clear() {
        size = 0
    }

    fun toByteArray(): ByteArray = buf.copyOf(size)

    fun copyInto(target: ByteSink, from: Int, to: Int) = target.write(buf, from, to - from)

    private fun grow(extra: Int) {
        var next = buf.size.toLong() * 2
        while (next < size.toLong() + extra) next *= 2
        buf = buf.copyOf(minOf(next, Int.MAX_VALUE.toLong() - 8).toInt())
    }
}

internal class NameCache {
    private val slots = arrayOfNulls<String>(4096)

    fun get(bytes: ByteArray, start: Int, end: Int): String {
        val length = end - start
        if (length > 40) return latin1(bytes, start, end)
        var hash = length
        for (i in start until end) hash = hash * 31 + bytes[i]
        val slot = (hash xor (hash ushr 12)) and 4095
        val cached = slots[slot]
        if (cached != null && cached.length == length) {
            var same = true
            for (i in 0 until length) {
                if (cached[i].code != (bytes[start + i].toInt() and 0xFF)) {
                    same = false
                    break
                }
            }
            if (same) return cached
        }
        val created = latin1(bytes, start, end)
        slots[slot] = created
        return created
    }
}

// numbers come back through intValue, realValue and isInteger, so scanning allocates nothing per
// token except strings and names
internal class PdfLexer(val data: ByteArray, val names: NameCache, var pos: Int = 0, val end: Int = data.size) {
    var isInteger = false
        private set
    var intValue = 0L
        private set
    var realValue = 0.0
        private set

    private val scratch = ByteSink(64)

    fun peek(): Int = if (pos < end) data[pos].toInt() and 0xFF else -1

    fun peekAt(offset: Int): Int {
        val i = pos + offset
        return if (i in 0 until end) data[i].toInt() and 0xFF else -1
    }

    fun skipWhitespace() {
        while (pos < end) {
            val c = data[pos].toInt() and 0xFF
            if (charClass[c] == WHITESPACE) {
                pos++
            } else if (c == '%'.code) {
                while (pos < end && data[pos].toInt() != 10 && data[pos].toInt() != 13) pos++
            } else {
                return
            }
        }
    }

    fun atKeyword(word: String): Boolean = matchesAt(pos, word)

    fun matchesAt(at: Int, word: String): Boolean {
        if (at < 0 || at + word.length > end) return false
        for (i in word.indices) if (data[at + i].toInt() != word[i].code) return false
        val after = at + word.length
        return after >= end || !isRegular(data[after].toInt())
    }

    fun readKeyword(word: String): Boolean {
        if (!atKeyword(word)) return false
        pos += word.length
        return true
    }

    fun startsNumber(c: Int): Boolean = c in 48..57 || c == '-'.code || c == '+'.code || c == '.'.code

    fun readNumber(): Boolean {
        var i = pos
        var negative = false
        while (i < end && (data[i].toInt() == '-'.code || data[i].toInt() == '+'.code)) {
            if (data[i].toInt() == '-'.code) negative = true
            i++
        }
        var mantissa = 0L
        var significant = 0
        var digits = 0
        var fraction = 0
        var dropped = 0
        var dot = false
        while (i < end) {
            val c = data[i].toInt()
            if (c in 48..57) {
                digits++
                if (significant < 18) {
                    mantissa = mantissa * 10 + (c - 48)
                    if (mantissa != 0L) significant++
                    if (dot) fraction++
                } else if (!dot) {
                    dropped++
                }
            } else if (c == '.'.code && !dot) {
                dot = true
            } else {
                break
            }
            i++
        }
        if (digits == 0) {
            if (i == pos || (i < end && isRegular(data[i].toInt()))) return false
        }
        while (i < end && (data[i].toInt() in 48..57 || data[i].toInt() == '.'.code || data[i].toInt() == '-'.code)) i++
        pos = i
        if (!dot && dropped == 0) {
            isInteger = true
            intValue = if (negative) -mantissa else mantissa
            realValue = intValue.toDouble()
        } else {
            isInteger = false
            var value = mantissa.toDouble()
            if (fraction > 0) value /= powerOfTen(fraction)
            if (dropped > 0) value *= powerOfTen(dropped)
            realValue = if (negative) -value else value
            intValue = realValue.toLong()
        }
        return true
    }

    fun readDigits(): Long {
        var i = pos
        var value = 0L
        while (i < end && data[i].toInt() in 48..57 && i - pos < 18) {
            value = value * 10 + (data[i] - 48)
            i++
        }
        if (i == pos) return -1
        while (i < end && data[i].toInt() in 48..57) i++
        pos = i
        return value
    }

    fun readName(): String {
        pos++
        val start = pos
        var escaped = false
        while (pos < end) {
            val c = data[pos].toInt()
            if (!isRegular(c)) break
            if (c == '#'.code) escaped = true
            pos++
        }
        if (!escaped) return names.get(data, start, pos)
        scratch.clear()
        var i = start
        while (i < pos) {
            val c = data[i].toInt() and 0xFF
            if (c == '#'.code && i + 2 < pos) {
                val hi = hexValue(data[i + 1].toInt())
                val lo = hexValue(data[i + 2].toInt())
                if (hi >= 0 && lo >= 0) {
                    scratch.write((hi shl 4) or lo)
                    i += 3
                    continue
                }
            }
            scratch.write(c)
            i++
        }
        val bytes = scratch.toByteArray()
        return names.get(bytes, 0, bytes.size)
    }

    fun readLiteralString(): ByteArray {
        pos++
        scratch.clear()
        var depth = 1
        while (pos < end) {
            val c = data[pos++].toInt() and 0xFF
            when (c) {
                '('.code -> {
                    depth++
                    scratch.write(c)
                }
                ')'.code -> {
                    depth--
                    if (depth == 0) break
                    scratch.write(c)
                }
                '\\'.code -> readEscape()
                13 -> {
                    scratch.write(10)
                    if (pos < end && data[pos].toInt() == 10) pos++
                }
                else -> scratch.write(c)
            }
        }
        return scratch.toByteArray()
    }

    private fun readEscape() {
        if (pos >= end) return
        val e = data[pos++].toInt() and 0xFF
        when (e) {
            'n'.code -> scratch.write(10)
            'r'.code -> scratch.write(13)
            't'.code -> scratch.write(9)
            'b'.code -> scratch.write(8)
            'f'.code -> scratch.write(12)
            in 48..55 -> {
                var value = e - 48
                var count = 1
                while (count < 3 && pos < end && data[pos].toInt() in 48..55) {
                    value = value * 8 + (data[pos] - 48)
                    pos++
                    count++
                }
                scratch.write(value and 0xFF)
            }
            13 -> if (pos < end && data[pos].toInt() == 10) pos++
            10 -> Unit
            else -> scratch.write(e)
        }
    }

    fun readHexString(): ByteArray {
        pos++
        scratch.clear()
        var high = -1
        while (pos < end) {
            val c = data[pos++].toInt() and 0xFF
            if (c == '>'.code) break
            val v = hexValue(c)
            if (v < 0) continue
            if (high < 0) {
                high = v
            } else {
                scratch.write((high shl 4) or v)
                high = -1
            }
        }
        if (high >= 0) scratch.write(high shl 4)
        return scratch.toByteArray()
    }

    fun readRegular(): String {
        val start = pos
        while (pos < end && isRegular(data[pos].toInt())) pos++
        if (pos == start) pos++
        return names.get(data, start, pos)
    }

    fun indexOf(word: String, from: Int, limit: Int = end): Int {
        if (word.isEmpty()) return from
        val first = word[0].code.toByte()
        var i = from
        val last = limit - word.length
        outer@ while (i <= last) {
            if (data[i] != first) {
                i++
                continue
            }
            for (k in 1 until word.length) {
                if (data[i + k].toInt() != word[k].code) {
                    i++
                    continue@outer
                }
            }
            return i
        }
        return -1
    }
}
