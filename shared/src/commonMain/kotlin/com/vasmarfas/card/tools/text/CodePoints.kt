package com.vasmarfas.card.tools.text

fun String.codePointList(): List<Int> {
    val result = ArrayList<Int>(length)
    var i = 0
    while (i < length) {
        val c = this[i]
        if (c.isHighSurrogate() && i + 1 < length && this[i + 1].isLowSurrogate()) {
            result += 0x10000 + ((c.code - 0xD800) shl 10) + (this[i + 1].code - 0xDC00)
            i += 2
        } else {
            result += c.code
            i++
        }
    }
    return result
}

fun StringBuilder.appendCodePoint(cp: Int): StringBuilder {
    if (cp < 0x10000) {
        append(cp.toChar())
    } else {
        val v = cp - 0x10000
        append((0xD800 + (v shr 10)).toChar())
        append((0xDC00 + (v and 0x3FF)).toChar())
    }
    return this
}

fun codePointToString(cp: Int): String = StringBuilder().appendCodePoint(cp).toString()

fun utf8Bytes(cp: Int): List<Int> = when {
    cp < 0x80 -> listOf(cp)
    cp < 0x800 -> listOf(0xC0 or (cp shr 6), 0x80 or (cp and 0x3F))
    cp < 0x10000 -> listOf(0xE0 or (cp shr 12), 0x80 or ((cp shr 6) and 0x3F), 0x80 or (cp and 0x3F))
    else -> listOf(0xF0 or (cp shr 18), 0x80 or ((cp shr 12) and 0x3F), 0x80 or ((cp shr 6) and 0x3F), 0x80 or (cp and 0x3F))
}
