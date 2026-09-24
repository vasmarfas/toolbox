package com.vasmarfas.card.tools.text

// bytes 0x80..0xFF, the ones a page leaves undefined map to C1 controls
enum class Codepage(val label: String, private val upper: String) {
    UTF8("UTF-8", ""),
    CP1251(
        "Windows-1251",
        "ЂЃ‚ѓ„…†‡€‰Љ‹ЊЌЋЏђ‘’“”•–—\u0098™љ›њќћџ" +
            " ЎўЈ¤Ґ¦§Ё©Є«¬­®Ї°±Ііґµ¶·ё№є»јЅѕї" +
            "АБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ" +
            "абвгдежзийклмнопрстуфхцчшщъыьэюя",
    ),
    CP1252(
        "Windows-1252",
        "€\u0081‚ƒ„…†‡ˆ‰Š‹Œ\u008DŽ\u008F\u0090‘’“”•–—˜™š›œ\u009DžŸ" +
            " ¡¢£¤¥¦§¨©ª«¬­®¯°±²³´µ¶·¸¹º»¼½¾¿" +
            "ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞß" +
            "àáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ",
    ),
    KOI8R(
        "KOI8-R",
        "─│┌┐└┘├┤┬┴┼▀▄█▌▐░▒▓⌠■∙√≈≤≥ ⌡°²·÷" +
            "═║╒ё╓╔╕╖╗╘╙╚╛╜╝╞╟╠╡Ё╢╣╤╥╦╧╨╩╪╫╬©" +
            "юабцдефгхийклмнопярстужвьызшэщчъ" +
            "ЮАБЦДЕФГХИЙКЛМНОПЯРСТУЖВЬЫЗШЭЩЧЪ",
    ),
    CP866(
        "CP866",
        "АБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ" +
            "абвгдежзийклмноп░▒▓│┤╡╢╖╕╣║╗╝╜╛┐" +
            "└┴┬├─┼╞╟╚╔╩╦╠═╬╧╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀" +
            "рстуфхцчшщъыьэюяЁёЄєЇїЎў°∙·√№¤■ ",
    ),
    ;

    private val byteOf: Map<Char, Int> by lazy { upper.withIndex().associate { (i, c) -> c to 0x80 + i } }

    fun decode(data: ByteArray): String {
        if (this == UTF8) return data.decodeToString()
        return CharArray(data.size) {
            val b = data[it].toInt() and 0xFF
            if (b < 0x80) b.toChar() else upper[b - 0x80]
        }.concatToString()
    }

    fun encode(text: String): ByteArray? {
        if (this == UTF8) return text.encodeToByteArray()
        val out = ByteArray(text.length)
        for (i in text.indices) {
            val c = text[i]
            out[i] = (if (c.code < 0x80) c.code else byteOf[c] ?: return null).toByte()
        }
        return out
    }
}

data class MojibakeStep(val shownAs: Codepage, val actual: Codepage)

class MojibakeFix(val text: String, val steps: List<MojibakeStep>)

// every single and double wrong reading between the pages, ranked by Russian letter pair frequency,
// with a heavy price for garbling leftovers and capitals inside a lowercase word
object Mojibake {
    private const val SAMPLE = 1500
    private const val STEP_COST = 0.3
    private const val BIGRAM_WEIGHT = 0.5
    private const val RUSSIAN = "абвгдежзийклмнопрстуфхцчшщъыьэюя"
    private const val TYPOGRAPHIC = "«»—–…“”‘’„№°• €"
    private const val UKRAINIAN = "іїєґўІЇЄҐЎ"
    private const val LATIN_EXTRAS = "ŒœŠšŽžŸƒ"
    private const val SYMBOLS = "†‡‰‹›™ˆ˜"
    private val boxExtras = setOf(0x2219, 0x221A, 0x2248, 0x2264, 0x2265, 0x2320, 0x2321, 0xF7)
    private val lostRun = Regex("\\?{3,}")

    // -log2 of how often one letter follows another, row and column 0 are anything but a Russian letter
    private val bigramBits = listOf(
        "e654647854b464443535667667eede6a8",
        "2c6565564a55453b55437876689ddda74",
        "5399b93984b64763937b5b99bb6639bb6",
        "23b6a93c94c5585375566c9cbbcc37cca",
        "449b665bb4b74661839b6bb7bbbbbbbbb",
        "4386a73893c64843846c5c8acaca57c87",
        "29766577596544377343b899778dddddb",
        "53aaa328a2a4a637aa7a8aaa8aaaaaaaa",
        "3264a558b4b57443b4ab6bb9bbbb6898a",
        "26657647565545477544c67658adddd74",
        "1b98b6bbbbb54558bb44bbb876bbbbbbb",
        "32967c5cb3ca5762b4454cc8ccccccbcc",
        "43987a37a2c76b63cc8b6accaccc53c54",
        "23869c2cc3c775645c785accaccc6cca5",
        "43b7763aa2d6bd53bd6459b89bbd36db6",
        "3b535466775444485444a877678eee8b7",
        "64ccca3cc4c84a7272a85acccacc79cc7",
        "42a6793794d69662b7654978abdd57db6",
        "3586a948d4d3466456526b7d67d966da4",
        "3396d83bb4d68853935859bd7ddd53bd8",
        "385745554985345b5444b76b576bbbb5b",
        "73aaaa4aa2aa58a2a37654aaaaaa8aaaa",
        "14a5aa4a86a67a52a38a6aaaaaaaaaaaa",
        "43a4aa2aa2a8aaa7aa7a5aa8aaaa4aaaa",
        "52bbbb2bb2b5b73bbb748bbb8bbbb7bbb",
        "63a7aa2aa2a45a5765845aaaaaaaa6aaa",
        "838888288288883888885888888888888",
        "777777177777777777777777777777777",
        "2b558639bb26659b6655bb4b669bbbbbb",
        "1b7abb7b68b4b73bbb54b8bba5bbbbb69",
        "788888888881464885828488878888888",
        "284997976955767996429999394999989",
        "1b8895556b88a66ba674bb767b7bbbb67",
    )

    private val readings: List<MojibakeStep> = listOf(Codepage.CP1251, Codepage.CP1252, Codepage.KOI8R, Codepage.CP866)
        .flatMap { shown -> Codepage.entries.filter { it != shown }.map { MojibakeStep(shown, it) } }

    fun apply(text: String, steps: List<MojibakeStep>): String? {
        var current = text
        for (step in steps) current = step.shownAs.encode(current)?.let(step.actual::decode) ?: return null
        return current
    }

    fun repairs(text: String, limit: Int = 5): List<MojibakeFix> {
        val sample = text.take(SAMPLE)
        val best = mutableMapOf<String, Pair<List<MojibakeStep>, Double>>()
        fun offer(candidate: String, steps: List<MojibakeStep>) {
            val score = score(candidate) - STEP_COST * steps.size
            val known = best[candidate]
            if (known == null || known.second < score) best[candidate] = steps to score
        }
        offer(sample, emptyList())
        for (first in readings) {
            val once = apply(sample, listOf(first)) ?: continue
            if (once == sample) continue
            offer(once, listOf(first))
            for (second in readings) {
                val twice = apply(once, listOf(second)) ?: continue
                if (twice != once && twice != sample) offer(twice, listOf(first, second))
            }
        }
        return best.values.sortedByDescending { it.second }.take(limit).mapNotNull { (steps, _) ->
            apply(text, steps)?.let { MojibakeFix(it, steps) }
        }
    }

    fun hasLostCharacters(text: String): Boolean = '�' in text || lostRun.containsMatchIn(text)

    fun score(text: String): Double {
        var total = 0.0
        var count = 0
        var previousClass = 0
        var previous = ' '
        var i = 0
        while (i < text.length) {
            val c = text[i]
            val code = if (c.isHighSurrogate() && i + 1 < text.length && text[i + 1].isLowSurrogate()) {
                i++
                0x10000 + ((c.code - 0xD800) shl 10) + (text[i].code - 0xDC00)
            } else {
                c.code
            }
            total += penalty(code)
            val letter = letterClass(c)
            if (letter != 0 || previousClass != 0) total -= BIGRAM_WEIGHT * bits(previousClass, letter)
            if (c.isLetter() && previous.isLetter() && c.isUpperCase() && previous.isLowerCase()) total -= 5
            count++
            previousClass = letter
            previous = c
            i++
        }
        if (previousClass != 0) total -= BIGRAM_WEIGHT * bits(previousClass, 0)
        return total / maxOf(count, 1)
    }

    private fun letterClass(c: Char): Int {
        val lower = c.lowercaseChar()
        return RUSSIAN.indexOf(if (lower == 'ё') 'е' else lower) + 1
    }

    private fun bits(from: Int, to: Int): Int = bigramBits[from][to].digitToInt(16)

    private fun penalty(code: Int): Int = when {
        code < 0x80 -> if (code >= 0x20 || code == 0x09 || code == 0x0A || code == 0x0D) 0 else -10
        code in 0x410..0x44F || code == 0x401 || code == 0x451 -> 0
        code < 0x10000 && code.toChar() in TYPOGRAPHIC -> -2
        code < 0x10000 && code.toChar() in UKRAINIAN -> -3
        code in 0x400..0x4FF -> -8
        code == 0xFFFD -> -12
        code in 0x80..0x9F -> -10
        code in 0x2500..0x25FF || code in boxExtras -> -8
        code in 0xC0..0xFF || code < 0x10000 && code.toChar() in LATIN_EXTRAS -> -4
        code in 0xA0..0xBF || code < 0x10000 && code.toChar() in SYMBOLS -> -6
        code in 0x1F300..0x1FAFF || code in 0x2600..0x27BF -> -2
        else -> -6
    }
}
