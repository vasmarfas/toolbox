package com.vasmarfas.card.tools.text

data class CodePointInfo(
    val cp: Int,
    val text: String,
    val utf8: String,
    val utf16: String,
    val category: String,
    val block: String,
) {
    val hex: String get() = "U+" + cp.toString(16).uppercase().padStart(4, '0')
}

object UnicodeInfo {
    private val blocks = listOf(
        Triple(0x0000, 0x007F, "Basic Latin"),
        Triple(0x0080, 0x00FF, "Latin-1 Supplement"),
        Triple(0x0100, 0x017F, "Latin Extended-A"),
        Triple(0x0180, 0x024F, "Latin Extended-B"),
        Triple(0x0250, 0x02AF, "IPA Extensions"),
        Triple(0x02B0, 0x02FF, "Spacing Modifier Letters"),
        Triple(0x0300, 0x036F, "Combining Diacritical Marks"),
        Triple(0x0370, 0x03FF, "Greek and Coptic"),
        Triple(0x0400, 0x04FF, "Cyrillic"),
        Triple(0x0500, 0x052F, "Cyrillic Supplement"),
        Triple(0x0530, 0x058F, "Armenian"),
        Triple(0x0590, 0x05FF, "Hebrew"),
        Triple(0x0600, 0x06FF, "Arabic"),
        Triple(0x0700, 0x074F, "Syriac"),
        Triple(0x0900, 0x097F, "Devanagari"),
        Triple(0x0980, 0x09FF, "Bengali"),
        Triple(0x0B80, 0x0BFF, "Tamil"),
        Triple(0x0E00, 0x0E7F, "Thai"),
        Triple(0x0F00, 0x0FFF, "Tibetan"),
        Triple(0x10A0, 0x10FF, "Georgian"),
        Triple(0x1100, 0x11FF, "Hangul Jamo"),
        Triple(0x1E00, 0x1EFF, "Latin Extended Additional"),
        Triple(0x1F00, 0x1FFF, "Greek Extended"),
        Triple(0x2000, 0x206F, "General Punctuation"),
        Triple(0x2070, 0x209F, "Superscripts and Subscripts"),
        Triple(0x20A0, 0x20CF, "Currency Symbols"),
        Triple(0x20D0, 0x20FF, "Combining Diacritical Marks for Symbols"),
        Triple(0x2100, 0x214F, "Letterlike Symbols"),
        Triple(0x2150, 0x218F, "Number Forms"),
        Triple(0x2190, 0x21FF, "Arrows"),
        Triple(0x2200, 0x22FF, "Mathematical Operators"),
        Triple(0x2300, 0x23FF, "Miscellaneous Technical"),
        Triple(0x2400, 0x243F, "Control Pictures"),
        Triple(0x2460, 0x24FF, "Enclosed Alphanumerics"),
        Triple(0x2500, 0x257F, "Box Drawing"),
        Triple(0x2580, 0x259F, "Block Elements"),
        Triple(0x25A0, 0x25FF, "Geometric Shapes"),
        Triple(0x2600, 0x26FF, "Miscellaneous Symbols"),
        Triple(0x2700, 0x27BF, "Dingbats"),
        Triple(0x2800, 0x28FF, "Braille Patterns"),
        Triple(0x2B00, 0x2BFF, "Miscellaneous Symbols and Arrows"),
        Triple(0x2C60, 0x2C7F, "Latin Extended-C"),
        Triple(0x2DE0, 0x2DFF, "Cyrillic Extended-A"),
        Triple(0x2E00, 0x2E7F, "Supplemental Punctuation"),
        Triple(0x3000, 0x303F, "CJK Symbols and Punctuation"),
        Triple(0x3040, 0x309F, "Hiragana"),
        Triple(0x30A0, 0x30FF, "Katakana"),
        Triple(0x3400, 0x4DBF, "CJK Unified Ideographs Extension A"),
        Triple(0x4E00, 0x9FFF, "CJK Unified Ideographs"),
        Triple(0xA640, 0xA69F, "Cyrillic Extended-B"),
        Triple(0xA720, 0xA7FF, "Latin Extended-D"),
        Triple(0xAC00, 0xD7AF, "Hangul Syllables"),
        Triple(0xD800, 0xDBFF, "High Surrogates"),
        Triple(0xDC00, 0xDFFF, "Low Surrogates"),
        Triple(0xE000, 0xF8FF, "Private Use Area"),
        Triple(0xF900, 0xFAFF, "CJK Compatibility Ideographs"),
        Triple(0xFB00, 0xFB4F, "Alphabetic Presentation Forms"),
        Triple(0xFB50, 0xFDFF, "Arabic Presentation Forms-A"),
        Triple(0xFE00, 0xFE0F, "Variation Selectors"),
        Triple(0xFE70, 0xFEFF, "Arabic Presentation Forms-B"),
        Triple(0xFF00, 0xFFEF, "Halfwidth and Fullwidth Forms"),
        Triple(0xFFF0, 0xFFFF, "Specials"),
        Triple(0x1D100, 0x1D1FF, "Musical Symbols"),
        Triple(0x1D400, 0x1D7FF, "Mathematical Alphanumeric Symbols"),
        Triple(0x1F000, 0x1F02F, "Mahjong Tiles"),
        Triple(0x1F0A0, 0x1F0FF, "Playing Cards"),
        Triple(0x1F100, 0x1F1FF, "Enclosed Alphanumeric Supplement"),
        Triple(0x1F300, 0x1F5FF, "Miscellaneous Symbols and Pictographs"),
        Triple(0x1F600, 0x1F64F, "Emoticons"),
        Triple(0x1F650, 0x1F67F, "Ornamental Dingbats"),
        Triple(0x1F680, 0x1F6FF, "Transport and Map Symbols"),
        Triple(0x1F780, 0x1F7FF, "Geometric Shapes Extended"),
        Triple(0x1F900, 0x1F9FF, "Supplemental Symbols and Pictographs"),
        Triple(0x1FA00, 0x1FA6F, "Chess Symbols"),
        Triple(0x1FA70, 0x1FAFF, "Symbols and Pictographs Extended-A"),
        Triple(0x20000, 0x2A6DF, "CJK Unified Ideographs Extension B"),
        Triple(0xE0000, 0xE007F, "Tags"),
        Triple(0xE0100, 0xE01EF, "Variation Selectors Supplement"),
        Triple(0xF0000, 0xFFFFF, "Supplementary Private Use Area-A"),
        Triple(0x100000, 0x10FFFF, "Supplementary Private Use Area-B"),
    )

    private const val UNRESERVED = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.~"

    fun hex(value: Int, width: Int): String = value.toString(16).uppercase().padStart(width, '0')

    fun blockName(cp: Int): String = blocks.firstOrNull { cp in it.first..it.second }?.third ?: "Other"

    fun categoryName(cp: Int): String {
        if (cp > 0xFFFF) return "Supplementary plane ${cp shr 16}"
        val category = Char(cp).category
        return category.code + " " + category.name.lowercase().replace('_', ' ')
    }

    fun utf16Units(cp: Int): String {
        if (cp < 0x10000) return hex(cp, 4)
        val v = cp - 0x10000
        return hex(0xD800 + (v shr 10), 4) + " " + hex(0xDC00 + (v and 0x3FF), 4)
    }

    fun info(cp: Int): CodePointInfo = CodePointInfo(
        cp = cp,
        text = codePointToString(cp),
        utf8 = utf8Bytes(cp).joinToString(" ") { hex(it, 2) },
        utf16 = utf16Units(cp),
        category = categoryName(cp),
        block = blockName(cp),
    )

    fun inspect(text: String, limit: Int = 300): List<CodePointInfo> = text.codePointList().take(limit).map { info(it) }

    fun escapeUtf16(text: String): String = buildString {
        for (c in text) if (c.code in 0x20..0x7E) append(c) else append("\\u").append(hex(c.code, 4))
    }

    fun escapeCodePoints(text: String): String = buildString {
        for (cp in text.codePointList()) if (cp in 0x20..0x7E) appendCodePoint(cp) else append("\\u{").append(hex(cp, 1)).append('}')
    }

    fun htmlHex(text: String): String = buildString {
        for (cp in text.codePointList()) if (cp in 0x20..0x7E) appendCodePoint(cp) else append("&#x").append(hex(cp, 1)).append(';')
    }

    fun htmlDecimal(text: String): String = buildString {
        for (cp in text.codePointList()) if (cp in 0x20..0x7E) appendCodePoint(cp) else append("&#").append(cp).append(';')
    }

    fun urlEncode(text: String): String = buildString {
        for (b in text.encodeToByteArray()) {
            val v = b.toInt() and 0xFF
            if (v < 0x80 && v.toChar() in UNRESERVED) append(v.toChar()) else append('%').append(hex(v, 2))
        }
    }

    fun parseCodePoint(input: String): Int? {
        val t = input.trim()
        if (t.isEmpty()) return null
        val hexPrefixes = listOf("U+", "u+", "\\u{", "\\u", "\\U", "0x", "0X", "&#x", "&#X")
        for (prefix in hexPrefixes) {
            if (t.startsWith(prefix)) {
                val body = t.removePrefix(prefix).trimEnd('}', ';')
                return body.toIntOrNull(16)?.takeIf { it in 0..0x10FFFF }
            }
        }
        if (t.startsWith("&#")) return t.removePrefix("&#").trimEnd(';').toIntOrNull()?.takeIf { it in 0..0x10FFFF }
        if (t.all { it.isDigit() }) return t.toIntOrNull()?.takeIf { it in 0..0x10FFFF }
        if (t.codePointList().size == 1) return t.codePointList().first()
        return t.toIntOrNull(16)?.takeIf { it in 0..0x10FFFF }
    }
}
