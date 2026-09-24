package com.vasmarfas.card.tools.documents

// Symbol and Wingdings put their glyphs at Latin code points, or at U+F020..U+F0FF in newer files.
// Symbol maps completely, Wingdings only for the bullets, checks and circled digits documents use
internal object SymbolFonts {
    private const val SYMBOL =
        " !∀#∃%&∋()∗+,−./" +
            "0123456789:;<=>?" +
            "≅ΑΒΧΔΕΦΓΗΙϑΚΛΜΝΟ" +
            "ΠΘΡΣΤΥςΩΞΨΖ[∴]⊥_" +
            "‾αβχδεφγηιϕκλμνο" +
            "πθρστυϖωξψζ{|}∼�" +
            "����������������" +
            "����������������" +
            "€ϒ′≤⁄∞ƒ♣♦♥♠↔←↑→↓" +
            "°±″≥×∝∂•÷≠≡≈…⏐⎯↵" +
            "ℵℑℜ℘⊗⊕∅∩∪⊃⊇⊄⊂⊆∈∉" +
            "∠∇®©™∏√⋅¬∧∨⇔⇐⇑⇒⇓" +
            "◊〈®©™∑⎛⎜⎝⎡⎢⎣⎧⎨⎩⎪" +
            "�〉∫⌠⎮⌡⎞⎟⎠⎤⎥⎦⎫⎬⎭�"

    private val WINGDINGS: Map<Int, String> = buildMap {
        put(0x4A, "☺")
        put(0x4C, "☹")
        put(0x6C, "●")
        put(0x6E, "■")
        put(0x6F, "□")
        put(0x71, "❑")
        put(0x76, "❖")
        put(0xA7, "▪")
        put(0xA8, "◻")
        put(0xD8, "➢")
        put(0xFB, "✗")
        put(0xFC, "✓")
        put(0xFD, "☒")
        put(0xFE, "☑")
        put(0x80, "⓪")
        for (i in 0..9) put(0x81 + i, (0x2460 + i).toChar().toString())
        put(0x8B, "⓿")
        for (i in 0..9) put(0x8C + i, (0x2776 + i).toChar().toString())
    }

    fun isSymbolFont(font: String?): Boolean = font != null && (font.equals("Symbol", ignoreCase = true) || isWingdings(font))

    fun map(font: String?, code: Int): String? {
        val c = if (code in 0xF000..0xF0FF) code - 0xF000 else code
        return when {
            font.equals("Symbol", ignoreCase = true) && c in 0x20..0xFF -> SYMBOL[c - 0x20].takeIf { it != '�' }?.toString()
            font != null && isWingdings(font) -> WINGDINGS[c]
            code in 0x20..0xD7FF || code in 0xE000..0xFFFD -> code.toChar().toString()
            else -> null
        }
    }

    fun text(font: String, text: String): String = buildString {
        for (ch in text) append(if (ch < ' ') ch.toString() else map(font, ch.code) ?: "")
    }

    private fun isWingdings(font: String) = font.equals("Wingdings", ignoreCase = true)
}
