package com.vasmarfas.card.tools.converters

object BaseConvert {
    const val DIGITS = "0123456789abcdefghijklmnopqrstuvwxyz"

    fun convert(input: String, from: Int, to: Int): String? {
        require(from in 2..36 && to in 2..36)
        var s = input.trim().lowercase().replace(" ", "").replace("_", "")
        if (s.isEmpty()) return null
        val negative = s.startsWith("-")
        if (negative) s = s.substring(1)
        val prefix = when (from) {
            2 -> "0b"
            8 -> "0o"
            16 -> "0x"
            else -> null
        }
        if (prefix != null && s.startsWith(prefix)) s = s.substring(2)
        if (s.isEmpty()) return null
        val digits = ArrayList<Int>(s.length)
        for (c in s) {
            val d = DIGITS.indexOf(c)
            if (d < 0 || d >= from) return null
            digits.add(d)
        }
        var current: List<Int> = digits.dropWhile { it == 0 }
        if (current.isEmpty()) return "0"
        val out = StringBuilder()
        while (current.isNotEmpty()) {
            val quotient = ArrayList<Int>(current.size)
            var remainder = 0
            for (d in current) {
                val acc = remainder * from + d
                val q = acc / to
                remainder = acc % to
                if (quotient.isNotEmpty() || q != 0) quotient.add(q)
            }
            out.append(DIGITS[remainder])
            current = quotient
        }
        return (if (negative) "-" else "") + out.reverse().toString().uppercase()
    }
}
