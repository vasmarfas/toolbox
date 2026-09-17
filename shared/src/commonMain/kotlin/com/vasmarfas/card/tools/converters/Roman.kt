package com.vasmarfas.card.tools.converters

object Roman {
    val symbols = listOf(
        1000 to "M", 900 to "CM", 500 to "D", 400 to "CD",
        100 to "C", 90 to "XC", 50 to "L", 40 to "XL",
        10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I",
    )
    private val pattern = Regex("^M{0,3}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})$")
    private val values = mapOf('I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100, 'D' to 500, 'M' to 1000)

    fun toRoman(number: Int): String? {
        if (number !in 1..3999) return null
        var rest = number
        return buildString {
            for ((value, symbol) in symbols) {
                while (rest >= value) {
                    append(symbol)
                    rest -= value
                }
            }
        }
    }

    fun toArabic(roman: String): Int? {
        val s = roman.trim().uppercase()
        if (s.isEmpty() || !pattern.matches(s)) return null
        var total = 0
        for (i in s.indices) {
            val current = values.getValue(s[i])
            val next = if (i + 1 < s.length) values.getValue(s[i + 1]) else 0
            total += if (current < next) -current else current
        }
        return total
    }
}
