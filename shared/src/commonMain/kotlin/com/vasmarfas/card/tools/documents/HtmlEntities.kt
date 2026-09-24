package com.vasmarfas.card.tools.documents

private const val LATIN1 =
    "nbsp iexcl cent pound curren yen brvbar sect uml copy ordf laquo not shy reg macr deg plusmn sup2 sup3 acute micro para middot cedil sup1 " +
        "ordm raquo frac14 frac12 frac34 iquest Agrave Aacute Acirc Atilde Auml Aring AElig Ccedil Egrave Eacute Ecirc Euml Igrave Iacute Icirc " +
        "Iuml ETH Ntilde Ograve Oacute Ocirc Otilde Ouml times Oslash Ugrave Uacute Ucirc Uuml Yacute THORN szlig agrave aacute acirc atilde auml " +
        "aring aelig ccedil egrave eacute ecirc euml igrave iacute icirc iuml eth ntilde ograve oacute ocirc otilde ouml divide oslash ugrave " +
        "uacute ucirc uuml yacute thorn yuml"

private const val GREEK_UPPER = "Alpha Beta Gamma Delta Epsilon Zeta Eta Theta Iota Kappa Lambda Mu Nu Xi Omicron Pi Rho - Sigma Tau Upsilon Phi Chi Psi Omega"

private val OTHER_ENTITIES = listOf(
    "amp" to 38, "lt" to 60, "gt" to 62, "quot" to 34, "apos" to 39, "AMP" to 38, "LT" to 60, "GT" to 62, "QUOT" to 34,
    "COPY" to 169, "REG" to 174, "OElig" to 338, "oelig" to 339, "Scaron" to 352, "scaron" to 353, "Yuml" to 376, "fnof" to 402,
    "circ" to 710, "tilde" to 732, "thetasym" to 977, "upsih" to 978, "piv" to 982, "sigmaf" to 962,
    "ensp" to 8194, "emsp" to 8195, "emsp13" to 8196, "emsp14" to 8197, "numsp" to 8199, "puncsp" to 8200, "thinsp" to 8201,
    "hairsp" to 8202, "ZeroWidthSpace" to 8203, "zwnj" to 8204, "zwj" to 8205, "lrm" to 8206, "rlm" to 8207, "hyphen" to 8208,
    "dash" to 8208, "ndash" to 8211, "mdash" to 8212, "horbar" to 8213, "lsquo" to 8216, "rsquo" to 8217, "sbquo" to 8218,
    "ldquo" to 8220, "rdquo" to 8221, "bdquo" to 8222, "dagger" to 8224, "Dagger" to 8225, "bull" to 8226, "bullet" to 8226,
    "nldr" to 8229, "hellip" to 8230, "mldr" to 8230, "permil" to 8240, "prime" to 8242, "Prime" to 8243, "lsaquo" to 8249,
    "rsaquo" to 8250, "oline" to 8254, "frasl" to 8260, "NoBreak" to 8288, "euro" to 8364, "image" to 8465, "numero" to 8470,
    "copysr" to 8471, "weierp" to 8472, "real" to 8476, "trade" to 8482, "TRADE" to 8482, "ohm" to 8486, "alefsym" to 8501,
    "frac13" to 8531, "frac23" to 8532, "frac15" to 8533, "frac25" to 8534, "frac35" to 8535, "frac45" to 8536, "frac16" to 8537,
    "frac56" to 8538, "frac18" to 8539, "frac38" to 8540, "frac58" to 8541, "frac78" to 8542, "larr" to 8592, "uarr" to 8593,
    "rarr" to 8594, "darr" to 8595, "harr" to 8596, "crarr" to 8629, "lArr" to 8656, "uArr" to 8657, "rArr" to 8658, "dArr" to 8659,
    "hArr" to 8660, "forall" to 8704, "part" to 8706, "exist" to 8707, "empty" to 8709, "nabla" to 8711, "isin" to 8712,
    "notin" to 8713, "ni" to 8715, "prod" to 8719, "sum" to 8721, "minus" to 8722, "lowast" to 8727, "radic" to 8730, "prop" to 8733,
    "infin" to 8734, "ang" to 8736, "and" to 8743, "or" to 8744, "cap" to 8745, "cup" to 8746, "int" to 8747, "there4" to 8756,
    "sim" to 8764, "cong" to 8773, "asymp" to 8776, "approx" to 8776, "ne" to 8800, "neq" to 8800, "equiv" to 8801, "le" to 8804,
    "leq" to 8804, "ge" to 8805, "geq" to 8805, "sub" to 8834, "sup" to 8835, "nsub" to 8836, "sube" to 8838, "supe" to 8839,
    "oplus" to 8853, "otimes" to 8855, "perp" to 8869, "sdot" to 8901, "lceil" to 8968, "rceil" to 8969, "lfloor" to 8970,
    "rfloor" to 8971, "lang" to 10216, "rang" to 10217, "loz" to 9674, "starf" to 9733, "bigstar" to 9733, "star" to 9734,
    "phone" to 9742, "female" to 9792, "male" to 9794, "spades" to 9824, "clubs" to 9827, "hearts" to 9829, "diams" to 9830,
    "flat" to 9837, "natural" to 9838, "sharp" to 9839, "check" to 10003, "checkmark" to 10003, "cross" to 10007,
    "longleftarrow" to 10229, "longrightarrow" to 10230, "Longrightarrow" to 10233, "Tab" to 9, "NewLine" to 10, "excl" to 33,
    "num" to 35, "dollar" to 36, "percnt" to 37, "lpar" to 40, "rpar" to 41, "ast" to 42, "plus" to 43, "comma" to 44,
    "period" to 46, "sol" to 47, "colon" to 58, "semi" to 59, "equals" to 61, "quest" to 63, "commat" to 64, "lsqb" to 91,
    "lbrack" to 91, "bsol" to 92, "rsqb" to 93, "rbrack" to 93, "Hat" to 94, "lowbar" to 95, "grave" to 96, "lcub" to 123,
    "lbrace" to 123, "verbar" to 124, "vert" to 124, "rcub" to 125, "rbrace" to 125, "half" to 189, "centerdot" to 183,
    "pm" to 177, "div" to 247, "circledR" to 174, "angst" to 197,
)

internal object HtmlEntities {
    private val named: Map<String, Int> = buildMap {
        LATIN1.split(' ').forEachIndexed { i, name -> put(name, 0xA0 + i) }
        GREEK_UPPER.split(' ').forEachIndexed { i, name -> if (name != "-") put(name, 0x391 + i) }
        GREEK_UPPER.split(' ').forEachIndexed { i, name -> if (name != "-") put(name.lowercase(), 0x3B1 + i) }
        for ((name, code) in OTHER_ENTITIES) put(name, code)
    }

    // HTML5 still decodes these without the semicolon
    private val legacy: Set<String> = LATIN1.split(' ').toSet() + setOf("amp", "lt", "gt", "quot", "AMP", "LT", "GT", "QUOT", "COPY", "REG")

    // Windows-1252 meanings HTML5 gives to numeric references in 0x80..0x9F
    private const val C1 = "€�‚ƒ„…†‡ˆ‰Š‹Œ�Ž��‘’“”•–—˜™š›œ�žŸ"

    fun decode(text: String, attribute: Boolean = false): String {
        if ('&' !in text) return text
        val sb = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c != '&') {
                sb.append(c)
                i++
                continue
            }
            i = reference(text, i, attribute, sb)
        }
        return sb.toString()
    }

    private fun reference(text: String, amp: Int, attribute: Boolean, sb: StringBuilder): Int {
        var j = amp + 1
        if (j < text.length && text[j] == '#') {
            j++
            val radix = if (j < text.length && (text[j] == 'x' || text[j] == 'X')) 16 else 10
            if (radix == 16) j++
            val start = j
            var code = 0
            while (j < text.length) {
                val digit = text[j].digitToIntOrNull(radix) ?: break
                if (code <= 0x10FFFF) code = code * radix + digit
                j++
            }
            if (j == start) {
                sb.append('&')
                return amp + 1
            }
            appendCode(sb, code)
            return if (j < text.length && text[j] == ';') j + 1 else j
        }
        while (j < text.length && j - amp <= 32 && text[j].isLetterOrDigit()) j++
        val name = text.substring(amp + 1, j)
        if (j < text.length && text[j] == ';') {
            val code = named[name]
            if (code != null) {
                appendCode(sb, code)
                return j + 1
            }
        } else {
            for (len in name.length downTo 2) {
                val prefix = name.substring(0, len)
                if (prefix !in legacy) continue
                val next = if (amp + 1 + len < text.length) text[amp + 1 + len] else ' '
                if (attribute && (next == '=' || next.isLetterOrDigit())) break
                appendCode(sb, named.getValue(prefix))
                return amp + 1 + len
            }
        }
        sb.append('&')
        return amp + 1
    }

    private fun appendCode(sb: StringBuilder, code: Int) {
        when {
            code == 0 || code in 0xD800..0xDFFF || code > 0x10FFFF -> sb.append('�')
            code in 0x80..0x9F -> sb.append(C1[code - 0x80])
            code < 0x10000 -> sb.append(code.toChar())
            else -> {
                val v = code - 0x10000
                sb.append((0xD800 + (v ushr 10)).toChar()).append((0xDC00 + (v and 0x3FF)).toChar())
            }
        }
    }
}
