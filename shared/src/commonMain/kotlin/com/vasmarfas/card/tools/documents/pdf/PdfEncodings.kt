package com.vasmarfas.card.tools.documents.pdf

// PDF spec Annex D encodings and the glyph-name to Unicode table, '\u0000' marks an unused code
internal object PdfEncodings {
    val standard: CharArray = ascii().also { t ->
        t[0x27] = '\u2019'
        t[0x60] = '\u2018'
        fill(
            t, 0xA1,
            0x00A1, 0x00A2, 0x00A3, 0x2044, 0x00A5, 0x0192, 0x00A7, 0x00A4, 0x0027, 0x201C, 0x00AB, 0x2039, 0x203A, 0xFB01, 0xFB02, 0,
            0x2013, 0x2020, 0x2021, 0x00B7, 0, 0x00B6, 0x2022, 0x201A, 0x201E, 0x201D, 0x00BB, 0x2026, 0x2030, 0, 0x00BF, 0,
            0x0060, 0x00B4, 0x02C6, 0x02DC, 0x00AF, 0x02D8, 0x02D9, 0x00A8, 0, 0x02DA, 0x00B8, 0, 0x02DD, 0x02DB, 0x02C7, 0x2014,
        )
        val sparse = intArrayOf(
            0xE1, 0x00C6, 0xE3, 0x00AA, 0xE8, 0x0141, 0xE9, 0x00D8, 0xEA, 0x0152, 0xEB, 0x00BA,
            0xF1, 0x00E6, 0xF5, 0x0131, 0xF8, 0x0142, 0xF9, 0x00F8, 0xFA, 0x0153, 0xFB, 0x00DF,
        )
        for (i in sparse.indices step 2) t[sparse[i]] = sparse[i + 1].toChar()
    }

    val winAnsi: CharArray = ascii().also { t ->
        t[0x7F] = '\u2022'
        fill(
            t, 0x80,
            0x20AC, 0x2022, 0x201A, 0x0192, 0x201E, 0x2026, 0x2020, 0x2021, 0x02C6, 0x2030, 0x0160, 0x2039, 0x0152, 0x2022, 0x017D, 0x2022,
            0x2022, 0x2018, 0x2019, 0x201C, 0x201D, 0x2022, 0x2013, 0x2014, 0x02DC, 0x2122, 0x0161, 0x203A, 0x0153, 0x2022, 0x017E, 0x0178,
        )
        for (c in 0xA0..0xFF) t[c] = c.toChar()
        t[0xA0] = ' '
        t[0xAD] = '-'
    }

    val macRoman: CharArray = ascii().also { t ->
        fill(
            t, 0x80,
            0x00C4, 0x00C5, 0x00C7, 0x00C9, 0x00D1, 0x00D6, 0x00DC, 0x00E1, 0x00E0, 0x00E2, 0x00E4, 0x00E3, 0x00E5, 0x00E7, 0x00E9, 0x00E8,
            0x00EA, 0x00EB, 0x00ED, 0x00EC, 0x00EE, 0x00EF, 0x00F1, 0x00F3, 0x00F2, 0x00F4, 0x00F6, 0x00F5, 0x00FA, 0x00F9, 0x00FB, 0x00FC,
            0x2020, 0x00B0, 0x00A2, 0x00A3, 0x00A7, 0x2022, 0x00B6, 0x00DF, 0x00AE, 0x00A9, 0x2122, 0x00B4, 0x00A8, 0x2260, 0x00C6, 0x00D8,
            0x221E, 0x00B1, 0x2264, 0x2265, 0x00A5, 0x00B5, 0x2202, 0x2211, 0x220F, 0x03C0, 0x222B, 0x00AA, 0x00BA, 0x03A9, 0x00E6, 0x00F8,
            0x00BF, 0x00A1, 0x00AC, 0x221A, 0x0192, 0x2248, 0x2206, 0x00AB, 0x00BB, 0x2026, 0x0020, 0x00C0, 0x00C3, 0x00D5, 0x0152, 0x0153,
            0x2013, 0x2014, 0x201C, 0x201D, 0x2018, 0x2019, 0x00F7, 0x25CA, 0x00FF, 0x0178, 0x2044, 0x00A4, 0x2039, 0x203A, 0xFB01, 0xFB02,
            0x2021, 0x00B7, 0x201A, 0x201E, 0x2030, 0x00C2, 0x00CA, 0x00C1, 0x00CB, 0x00C8, 0x00CD, 0x00CE, 0x00CF, 0x00CC, 0x00D3, 0x00D4,
            0xF8FF, 0x00D2, 0x00DA, 0x00DB, 0x00D9, 0x0131, 0x02C6, 0x02DC, 0x00AF, 0x02D8, 0x02D9, 0x02DA, 0x00B8, 0x02DD, 0x02DB, 0x02C7,
        )
    }

    val pdfDoc: CharArray = ascii().also { t ->
        t[0x09] = '\t'
        t[0x0A] = '\n'
        t[0x0D] = '\r'
        fill(t, 0x18, 0x02D8, 0x02C7, 0x02C6, 0x02D9, 0x02DD, 0x02DB, 0x02DA, 0x02DC)
        fill(
            t, 0x80,
            0x2022, 0x2020, 0x2021, 0x2026, 0x2014, 0x2013, 0x0192, 0x2044, 0x2039, 0x203A, 0x2212, 0x2030, 0x201E, 0x201C, 0x201D, 0x2018,
            0x2019, 0x201A, 0x2122, 0xFB01, 0xFB02, 0x0141, 0x0152, 0x0160, 0x0178, 0x017D, 0x0131, 0x0142, 0x0153, 0x0161, 0x017E, 0,
            0x20AC,
        )
        for (c in 0xA1..0xFF) t[c] = c.toChar()
    }

    val symbol: CharArray = CharArray(256).also { t ->
        fill(
            t, 0x20,
            0x0020, 0x0021, 0x2200, 0x0023, 0x2203, 0x0025, 0x0026, 0x220B, 0x0028, 0x0029, 0x2217, 0x002B, 0x002C, 0x2212, 0x002E, 0x002F,
            0x0030, 0x0031, 0x0032, 0x0033, 0x0034, 0x0035, 0x0036, 0x0037, 0x0038, 0x0039, 0x003A, 0x003B, 0x003C, 0x003D, 0x003E, 0x003F,
            0x2245, 0x0391, 0x0392, 0x03A7, 0x0394, 0x0395, 0x03A6, 0x0393, 0x0397, 0x0399, 0x03D1, 0x039A, 0x039B, 0x039C, 0x039D, 0x039F,
            0x03A0, 0x0398, 0x03A1, 0x03A3, 0x03A4, 0x03A5, 0x03C2, 0x03A9, 0x039E, 0x03A8, 0x0396, 0x005B, 0x2234, 0x005D, 0x22A5, 0x005F,
            0xF8E5, 0x03B1, 0x03B2, 0x03C7, 0x03B4, 0x03B5, 0x03C6, 0x03B3, 0x03B7, 0x03B9, 0x03D5, 0x03BA, 0x03BB, 0x03BC, 0x03BD, 0x03BF,
            0x03C0, 0x03B8, 0x03C1, 0x03C3, 0x03C4, 0x03C5, 0x03D6, 0x03C9, 0x03BE, 0x03C8, 0x03B6, 0x007B, 0x007C, 0x007D, 0x223C, 0,
        )
        fill(
            t, 0xA0,
            0x20AC, 0x03D2, 0x2032, 0x2264, 0x2044, 0x221E, 0x0192, 0x2663, 0x2666, 0x2665, 0x2660, 0x2194, 0x2190, 0x2191, 0x2192, 0x2193,
            0x00B0, 0x00B1, 0x2033, 0x2265, 0x00D7, 0x221D, 0x2202, 0x2022, 0x00F7, 0x2260, 0x2261, 0x2248, 0x2026, 0xF8E6, 0xF8E7, 0x21B5,
            0x2135, 0x2111, 0x211C, 0x2118, 0x2297, 0x2295, 0x2205, 0x2229, 0x222A, 0x2283, 0x2287, 0x2284, 0x2282, 0x2286, 0x2208, 0x2209,
            0x2220, 0x2207, 0x00AE, 0x00A9, 0x2122, 0x220F, 0x221A, 0x22C5, 0x00AC, 0x2227, 0x2228, 0x21D4, 0x21D0, 0x21D1, 0x21D2, 0x21D3,
            0x25CA, 0x2329, 0x00AE, 0x00A9, 0x2122, 0x2211, 0x239B, 0x239C, 0x239D, 0x23A1, 0x23A2, 0x23A3, 0x23A7, 0x23A8, 0x23A9, 0x23AA,
            0, 0x232A, 0x222B, 0x2320, 0x23AE, 0x2321, 0x239E, 0x239F, 0x23A0, 0x23A4, 0x23A5, 0x23A6, 0x23AB, 0x23AC, 0x23AD, 0,
        )
    }

    // the Unicode Dingbats block follows this font, apart from glyphs already encoded elsewhere
    val zapfDingbats: CharArray = CharArray(256).also { t ->
        t[0x20] = ' '
        for (c in 0x21..0x7E) t[c] = (0x2700 + c - 0x20).toChar()
        val moved = intArrayOf(
            0x25, 0x260E, 0x2A, 0x261B, 0x2B, 0x261E, 0x48, 0x2605, 0x6C, 0x25CF, 0x6E, 0x25A0,
            0x73, 0x25B2, 0x74, 0x25BC, 0x75, 0x25C6, 0x77, 0x25D7,
        )
        for (i in moved.indices step 2) t[moved[i]] = moved[i + 1].toChar()
        for (c in 0x80..0x8D) t[c] = (0x2768 + c - 0x80).toChar()
        for (c in 0xA1..0xA7) t[c] = (0x2761 + c - 0xA1).toChar()
        fill(t, 0xA8, 0x2663, 0x2666, 0x2665, 0x2660)
        for (c in 0xAC..0xB5) t[c] = (0x2460 + c - 0xAC).toChar()
        for (c in 0xB6..0xD3) t[c] = (0x2776 + c - 0xB6).toChar()
        fill(t, 0xD4, 0x2794, 0x2192, 0x2194, 0x2195)
        for (c in 0xD8..0xEF) t[c] = (0x2798 + c - 0xD8).toChar()
        for (c in 0xF1..0xFE) t[c] = (0x27B1 + c - 0xF1).toChar()
    }

    private val pdfDocReverse: Map<Char, Byte> by lazy {
        val map = HashMap<Char, Byte>(256)
        for (code in 255 downTo 0) if (pdfDoc[code] != '\u0000') map[pdfDoc[code]] = code.toByte()
        map
    }

    fun byName(name: String?): CharArray? = when (name) {
        "WinAnsiEncoding" -> winAnsi
        "MacRomanEncoding", "MacExpertEncoding" -> macRoman
        "StandardEncoding" -> standard
        "PDFDocEncoding" -> pdfDoc
        else -> null
    }

    fun decodeText(bytes: ByteArray): String {
        if (bytes.size >= 2) {
            val b0 = bytes[0].toInt() and 0xFF
            val b1 = bytes[1].toInt() and 0xFF
            if (b0 == 0xFE && b1 == 0xFF) return utf16(bytes, 2, bytes.size, bigEndian = true)
            if (b0 == 0xFF && b1 == 0xFE) return utf16(bytes, 2, bytes.size, bigEndian = false)
            if (bytes.size >= 3 && b0 == 0xEF && b1 == 0xBB && (bytes[2].toInt() and 0xFF) == 0xBF) return bytes.decodeToString(3, bytes.size)
        }
        val chars = CharArray(bytes.size) {
            val code = bytes[it].toInt() and 0xFF
            val mapped = pdfDoc[code]
            if (mapped != '\u0000') mapped else code.toChar()
        }
        return chars.concatToString()
    }

    fun encodeText(text: String): ByteArray {
        val reverse = pdfDocReverse
        val out = ByteArray(text.length)
        for (i in text.indices) {
            out[i] = reverse[text[i]] ?: return utf16Bytes(text)
        }
        return out
    }

    private fun utf16Bytes(text: String): ByteArray {
        val out = ByteArray(2 + text.length * 2)
        out[0] = 0xFE.toByte()
        out[1] = 0xFF.toByte()
        for (i in text.indices) {
            out[2 + i * 2] = (text[i].code ushr 8).toByte()
            out[3 + i * 2] = text[i].code.toByte()
        }
        return out
    }

    // drops the ESC-delimited language tags PDF text strings may carry
    fun utf16(bytes: ByteArray, start: Int, end: Int, bigEndian: Boolean = true): String {
        val sb = StringBuilder((end - start) / 2)
        var i = start
        while (i + 1 < end) {
            val hi = bytes[if (bigEndian) i else i + 1].toInt() and 0xFF
            val lo = bytes[if (bigEndian) i + 1 else i].toInt() and 0xFF
            val unit = (hi shl 8) or lo
            i += 2
            if (unit == 0x1B) {
                while (i + 1 < end) {
                    val next = ((bytes[if (bigEndian) i else i + 1].toInt() and 0xFF) shl 8) or (bytes[if (bigEndian) i + 1 else i].toInt() and 0xFF)
                    i += 2
                    if (next == 0x1B) break
                }
                continue
            }
            sb.append(unit.toChar())
        }
        return sb.toString()
    }

    // Adobe Glyph List rules: suffixes after '.' are dropped, '_' joins ligature parts
    fun glyphToUnicode(name: String): String? {
        glyphs[name]?.let { return it }
        val dot = name.indexOf('.')
        if (dot == 0) return null
        val base = if (dot > 0) name.substring(0, dot) else name
        if (base.indexOf('_') > 0) {
            val sb = StringBuilder()
            for (part in base.split('_')) sb.append(glyphToUnicode(part) ?: return null)
            return sb.toString()
        }
        if (dot > 0) glyphs[base]?.let { return it }
        return codePointName(base)
    }

    private fun codePointName(name: String): String? {
        if (name.length >= 7 && name.startsWith("uni") && (name.length - 3) % 4 == 0) {
            val sb = StringBuilder()
            for (i in 3 until name.length step 4) {
                val unit = hexNumber(name, i, i + 4, lowercase = true)
                if (unit < 0 || unit in 0xD800..0xDFFF) return null
                sb.append(unit.toChar())
            }
            return sb.toString()
        }
        if (name.length in 5..7 && name[0] == 'u') {
            val code = hexNumber(name, 1, name.length, lowercase = false)
            if (code < 0 || code in 0xD800..0xDFFF || code > 0x10FFFF) return null
            return codePointToString(code)
        }
        return null
    }

    private fun hexNumber(text: String, start: Int, end: Int, lowercase: Boolean): Int {
        var value = 0
        for (i in start until end) {
            val c = text[i]
            if (!lowercase && c in 'a'..'f') return -1
            val digit = hexValue(c.code)
            if (digit < 0) return -1
            value = value * 16 + digit
        }
        return value
    }

    fun codePointToString(code: Int): String {
        if (code < 0x10000) return code.toChar().toString()
        val v = code - 0x10000
        return charArrayOf((0xD800 + (v ushr 10)).toChar(), (0xDC00 + (v and 0x3FF)).toChar()).concatToString()
    }

    private fun ascii(): CharArray = CharArray(256).also { for (c in 0x20..0x7E) it[c] = c.toChar() }

    private fun fill(table: CharArray, start: Int, vararg codes: Int) {
        for (i in codes.indices) table[start + i] = codes[i].toChar()
    }

    private val glyphs: Map<String, String> by lazy { buildGlyphList() }

    private fun buildGlyphList(): Map<String, String> {
        val map = HashMap<String, String>(1400)
        fun sequence(start: Int, names: String) {
            var code = start
            for (n in names.split(' ')) {
                if (n != "-") map[n] = code.toChar().toString()
                code++
            }
        }
        for (c in 'A'..'Z') map[c.toString()] = c.toString()
        for (c in 'a'..'z') map[c.toString()] = c.toString()
        sequence(
            0x20,
            "space exclam quotedbl numbersign dollar percent ampersand quotesingle parenleft parenright asterisk plus comma hyphen period slash " +
                "zero one two three four five six seven eight nine colon semicolon less equal greater question at",
        )
        sequence(0x5B, "bracketleft backslash bracketright asciicircum underscore grave")
        sequence(0x7B, "braceleft bar braceright asciitilde")
        sequence(
            0xA0,
            "nbspace exclamdown cent sterling currency yen brokenbar section dieresis copyright ordfeminine guillemotleft logicalnot sfthyphen " +
                "registered macron degree plusminus twosuperior threesuperior acute mu paragraph periodcentered cedilla onesuperior ordmasculine " +
                "guillemotright onequarter onehalf threequarters questiondown Agrave Aacute Acircumflex Atilde Adieresis Aring AE Ccedilla Egrave " +
                "Eacute Ecircumflex Edieresis Igrave Iacute Icircumflex Idieresis Eth Ntilde Ograve Oacute Ocircumflex Otilde Odieresis multiply " +
                "Oslash Ugrave Uacute Ucircumflex Udieresis Yacute Thorn germandbls agrave aacute acircumflex atilde adieresis aring ae ccedilla " +
                "egrave eacute ecircumflex edieresis igrave iacute icircumflex idieresis eth ntilde ograve oacute ocircumflex otilde odieresis " +
                "divide oslash ugrave uacute ucircumflex udieresis yacute thorn ydieresis",
        )
        sequence(
            0x100,
            "Amacron amacron Abreve abreve Aogonek aogonek Cacute cacute Ccircumflex ccircumflex Cdotaccent cdotaccent Ccaron ccaron Dcaron " +
                "dcaron Dcroat dcroat Emacron emacron Ebreve ebreve Edotaccent edotaccent Eogonek eogonek Ecaron ecaron Gcircumflex gcircumflex " +
                "Gbreve gbreve Gdotaccent gdotaccent Gcommaaccent gcommaaccent Hcircumflex hcircumflex Hbar hbar Itilde itilde Imacron imacron " +
                "Ibreve ibreve Iogonek iogonek Idotaccent dotlessi IJ ij Jcircumflex jcircumflex Kcommaaccent kcommaaccent kgreenlandic Lacute " +
                "lacute Lcommaaccent lcommaaccent Lcaron lcaron Ldot ldot Lslash lslash Nacute nacute Ncommaaccent ncommaaccent Ncaron ncaron " +
                "napostrophe Eng eng Omacron omacron Obreve obreve Ohungarumlaut ohungarumlaut OE oe Racute racute Rcommaaccent rcommaaccent " +
                "Rcaron rcaron Sacute sacute Scircumflex scircumflex Scedilla scedilla Scaron scaron Tcommaaccent tcommaaccent Tcaron tcaron " +
                "Tbar tbar Utilde utilde Umacron umacron Ubreve ubreve Uring uring Uhungarumlaut uhungarumlaut Uogonek uogonek Wcircumflex " +
                "wcircumflex Ycircumflex ycircumflex Ydieresis Zacute zacute Zdotaccent zdotaccent Zcaron zcaron longs",
        )
        sequence(0x391, "Alpha Beta Gamma Delta Epsilon Zeta Eta Theta Iota Kappa Lambda Mu Nu Xi Omicron Pi Rho - Sigma Tau Upsilon Phi Chi Psi Omega")
        sequence(
            0x3B1,
            "alpha beta gamma delta epsilon zeta eta theta iota kappa lambda mugreek nu xi omicron pi rho sigma1 sigma tau upsilon phi chi psi omega",
        )
        val cyrillic = (
            "A Be Ve Ge De Ie Zhe Ze Ii Iishort Ka El Em En O Pe Er Es Te U Ef Kha Tse Che Sha Shcha Hardsign Yeri Softsign Ereversed IU IA"
            ).split(' ')
        for (k in 0 until 32) {
            val afiiUpper = if (k < 6) 10017 + k else 10018 + k
            val afiiLower = if (k < 6) 10065 + k else 10066 + k
            map["afii$afiiUpper"] = (0x410 + k).toChar().toString()
            map["afii$afiiLower"] = (0x430 + k).toChar().toString()
            map[cyrillic[k] + "cyrillic"] = (0x410 + k).toChar().toString()
            map[cyrillic[k].lowercase() + "cyrillic"] = (0x430 + k).toChar().toString()
        }
        val cyrillicExtra = intArrayOf(
            10023, 0x401, 10051, 0x402, 10052, 0x403, 10053, 0x404, 10054, 0x405, 10055, 0x406, 10056, 0x407, 10057, 0x408, 10058, 0x409,
            10059, 0x40A, 10060, 0x40B, 10061, 0x40C, 10062, 0x40E, 10145, 0x40F, 10071, 0x451, 10099, 0x452, 10100, 0x453, 10101, 0x454,
            10102, 0x455, 10103, 0x456, 10104, 0x457, 10105, 0x458, 10106, 0x459, 10107, 0x45A, 10108, 0x45B, 10109, 0x45C, 10110, 0x45E,
            10193, 0x45F, 10050, 0x490, 10098, 0x491, 10146, 0x462, 10194, 0x463, 10147, 0x472, 10195, 0x473, 10148, 0x474, 10196, 0x475,
            10846, 0x4D9, 61352, 0x2116, 61289, 0x2113, 208, 0x2015,
        )
        for (i in cyrillicExtra.indices step 2) {
            val number = cyrillicExtra[i]
            val name = if (number < 10000) "afii00${number.toString().padStart(3, '0')}" else "afii$number"
            map[name] = cyrillicExtra[i + 1].toChar().toString()
        }
        val cyrillicNamed = "Io Dje Gje E Dze I Yi Je Lje Nje Tshe Kje - Ushort Dzhe".split(' ')
        for (k in cyrillicNamed.indices) {
            if (cyrillicNamed[k] == "-") continue
            map[cyrillicNamed[k] + "cyrillic"] = (0x401 + k).toChar().toString()
            map[cyrillicNamed[k].lowercase() + "cyrillic"] = (0x451 + k).toChar().toString()
        }
        map["Gheupturncyrillic"] = "\u0490"
        map["gheupturncyrillic"] = "\u0491"
        val named = (
            "nonbreakingspace 00A0 softhyphen 00AD middot 00B7 Dslash 0110 dmacron 0111 Gcedilla 0122 gcedilla 0123 Kcedilla 0136 kcedilla 0137 " +
                "Lcedilla 013B lcedilla 013C Ncedilla 0145 ncedilla 0146 Rcedilla 0156 rcedilla 0157 Tcedilla 0162 tcedilla 0163 Cdot 010A " +
                "cdot 010B Edot 0116 edot 0117 Gdot 0120 gdot 0121 Idot 0130 Zdot 017B zdot 017C Ldotaccent 013F ldotaccent 0140 Odblacute 0150 " +
                "odblacute 0151 Udblacute 0170 udblacute 0171 quoterightn 0149 kra 0138 Ohorn 01A0 ohorn 01A1 Uhorn 01AF uhorn 01B0 Gcaron 01E6 " +
                "gcaron 01E7 Aringacute 01FA aringacute 01FB AEacute 01FC aeacute 01FD Oslashacute 01FE oslashacute 01FF Scommaaccent 0218 " +
                "scommaaccent 0219 dotlessj 0237 Schwa 018F schwa 0259 florin 0192 circumflex 02C6 caron 02C7 breve 02D8 dotaccent 02D9 ring 02DA " +
                "ogonek 02DB tilde 02DC hungarumlaut 02DD tonos 0384 dieresistonos 0385 Alphatonos 0386 anoteleia 0387 Epsilontonos 0388 " +
                "Etatonos 0389 Iotatonos 038A Omicrontonos 038C Upsilontonos 038E Omegatonos 038F iotadieresistonos 0390 Iotadieresis 03AA " +
                "Upsilondieresis 03AB alphatonos 03AC epsilontonos 03AD etatonos 03AE iotatonos 03AF upsilondieresistonos 03B0 iotadieresis 03CA " +
                "upsilondieresis 03CB omicrontonos 03CC upsilontonos 03CD omegatonos 03CE theta1 03D1 Upsilon1 03D2 phi1 03D5 omega1 03D6 " +
                "Wgrave 1E80 wgrave 1E81 Wacute 1E82 wacute 1E83 Wdieresis 1E84 wdieresis 1E85 Ygrave 1EF2 ygrave 1EF3 enspace 2002 " +
                "emspace 2003 thinspace 2009 hairspace 200A zerowidthspace 200B figuredash 2012 endash 2013 emdash 2014 horizontalbar 2015 " +
                "underscoredbl 2017 quoteleft 2018 quoteright 2019 quotesinglbase 201A quotereversed 201B quotedblleft 201C quotedblright 201D " +
                "quotedblbase 201E dagger 2020 daggerdbl 2021 bullet 2022 onedotenleader 2024 twodotenleader 2025 ellipsis 2026 perthousand 2030 " +
                "minute 2032 second 2033 guilsinglleft 2039 guilsinglright 203A exclamdbl 203C fraction 2044 Euro 20AC euro 20AC numero 2116 " +
                "trademark 2122 Ohm 2126 estimated 212E aleph 2135 Ifraktur 2111 Rfraktur 211C weierstrass 2118 onethird 2153 twothirds 2154 " +
                "oneeighth 215B threeeighths 215C fiveeighths 215D seveneighths 215E arrowleft 2190 arrowup 2191 arrowright 2192 arrowdown 2193 " +
                "arrowboth 2194 arrowupdn 2195 carriagereturn 21B5 arrowdblleft 21D0 arrowdblup 21D1 arrowdblright 21D2 arrowdbldown 21D3 " +
                "arrowdblboth 21D4 universal 2200 partialdiff 2202 existential 2203 emptyset 2205 increment 2206 gradient 2207 element 2208 " +
                "notelement 2209 suchthat 220B product 220F summation 2211 minus 2212 asteriskmath 2217 radical 221A proportional 221D " +
                "infinity 221E angle 2220 logicaland 2227 logicalor 2228 intersection 2229 union 222A integral 222B therefore 2234 similar 223C " +
                "congruent 2245 approxequal 2248 notequal 2260 equivalence 2261 lessequal 2264 greaterequal 2265 propersubset 2282 " +
                "propersuperset 2283 notsubset 2284 reflexsubset 2286 reflexsuperset 2287 circleplus 2295 circlemultiply 2297 perpendicular 22A5 " +
                "dotmath 22C5 house 2302 revlogicalnot 2310 integraltp 2320 integralbt 2321 angleleft 2329 angleright 232A filledbox 25A0 " +
                "H22073 25A1 filledrect 25AC triagup 25B2 triagrt 25BA triagdn 25BC triaglf 25C4 lozenge 25CA circle 25CB H18533 25CF " +
                "invbullet 25D8 invcircle 25D9 openbullet 25E6 smileface 263A invsmileface 263B sun 263C female 2640 male 2642 spade 2660 " +
                "club 2663 heart 2665 diamond 2666 musicalnote 266A musicalnotedbl 266B checkmark 2713 ff FB00 fi FB01 fl FB02 ffi FB03 " +
                "ffl FB04 registersans 00AE registerserif 00AE copyrightsans 00A9 copyrightserif 00A9 trademarksans 2122 trademarkserif 2122 " +
                "apple F8FF radicalex F8E5 arrowvertex F8E6 arrowhorizex F8E7"
            ).split(' ')
        for (i in named.indices step 2) map[named[i]] = named[i + 1].toInt(16).toChar().toString()
        return map
    }
}
