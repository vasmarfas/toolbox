package com.vasmarfas.card.tools.security

import com.vasmarfas.card.core.Tr
import com.vasmarfas.card.core.secureRandomBytes
import kotlin.math.ln
import kotlin.math.pow

data class CharSets(
    val lower: Boolean = true,
    val upper: Boolean = true,
    val digits: Boolean = true,
    val symbols: Boolean = true,
    val excludeAmbiguous: Boolean = false,
) {
    val groups: List<String>
        get() = buildList {
            if (lower) add(filter(PasswordGen.LOWER))
            if (upper) add(filter(PasswordGen.UPPER))
            if (digits) add(filter(PasswordGen.DIGITS))
            if (symbols) add(filter(PasswordGen.SYMBOLS))
        }.filter { it.isNotEmpty() }

    private fun filter(source: String) = if (excludeAmbiguous) source.filterNot { it in PasswordGen.AMBIGUOUS } else source

    val alphabet: String get() = groups.joinToString("")
}

object PasswordGen {
    const val LOWER = "abcdefghijklmnopqrstuvwxyz"
    const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    const val DIGITS = "0123456789"
    const val SYMBOLS = "!@#$%^&*()-_=+[]{};:,.?/"
    const val AMBIGUOUS = "Il1O0o|`'\";:.,"

    fun randomInt(bound: Int): Int {
        if (bound <= 1) return 0
        val limit = Int.MAX_VALUE - (Int.MAX_VALUE % bound)
        while (true) {
            val bytes = secureRandomBytes(4)
            var v = 0
            for (b in bytes) v = (v shl 8) or (b.toInt() and 0xFF)
            v = v and Int.MAX_VALUE
            if (v < limit) return v % bound
        }
    }

    fun <T> shuffle(items: MutableList<T>) {
        for (i in items.lastIndex downTo 1) {
            val j = randomInt(i + 1)
            val tmp = items[i]
            items[i] = items[j]
            items[j] = tmp
        }
    }

    fun password(length: Int, sets: CharSets, requireEach: Boolean): String? {
        val groups = sets.groups
        if (groups.isEmpty() || length < 1) return null
        val alphabet = sets.alphabet
        if (requireEach && length < groups.size) return null
        val chars = mutableListOf<Char>()
        if (requireEach) groups.forEach { group -> chars += group[randomInt(group.length)] }
        while (chars.size < length) chars += alphabet[randomInt(alphabet.length)]
        shuffle(chars)
        return chars.joinToString("")
    }

    fun passphrase(words: Int, list: List<String>, separator: String, capitalize: Boolean, addNumber: Boolean): String {
        val picked = MutableList(words) {
            val word = list[randomInt(list.size)]
            if (capitalize) word.replaceFirstChar { c -> c.uppercaseChar() } else word
        }
        if (addNumber && picked.isNotEmpty()) {
            val index = randomInt(picked.size)
            picked[index] = picked[index] + randomInt(100).toString().padStart(2, '0')
        }
        return picked.joinToString(separator)
    }

    fun entropyBits(alphabetSize: Int, length: Int): Double =
        if (alphabetSize <= 1 || length <= 0) 0.0 else length * ln(alphabetSize.toDouble()) / ln(2.0)

    fun passphraseEntropyBits(listSize: Int, words: Int, addNumber: Boolean): Double =
        words * ln(listSize.toDouble()) / ln(2.0) + if (addNumber) ln(100.0) / ln(2.0) else 0.0

    fun crackTimeSeconds(bits: Double, guessesPerSecond: Double): Double = 2.0.pow(bits - 1) / guessesPerSecond

    fun crackTimeLabel(seconds: Double): Tr = when {
        seconds < 1 -> Tr("instantly", "мгновенно")
        seconds < 60 -> Tr("${seconds.toLong()} seconds", "${seconds.toLong()} с")
        seconds < 3600 -> Tr("${(seconds / 60).toLong()} minutes", "${(seconds / 60).toLong()} мин")
        seconds < 86_400 -> Tr("${(seconds / 3600).toLong()} hours", "${(seconds / 3600).toLong()} ч")
        seconds < 2_592_000 -> Tr("${(seconds / 86_400).toLong()} days", "${(seconds / 86_400).toLong()} дн")
        seconds < 31_536_000 -> Tr("${(seconds / 2_592_000).toLong()} months", "${(seconds / 2_592_000).toLong()} мес")
        seconds < 31_536_000_000.0 -> Tr("${(seconds / 31_536_000).toLong()} years", "${(seconds / 31_536_000).toLong()} лет")
        seconds < 31_536_000_000_000.0 -> Tr("${(seconds / 31_536_000_000.0).toLong()} thousand years", "${(seconds / 31_536_000_000.0).toLong()} тыс. лет")
        seconds < 3.1536e16 -> Tr("${(seconds / 31_536_000_000_000.0).toLong()} million years", "${(seconds / 31_536_000_000_000.0).toLong()} млн лет")
        else -> Tr("longer than the age of the universe", "дольше возраста Вселенной")
    }

    fun strengthLabel(bits: Double): Tr = when {
        bits < 28 -> Tr("very weak", "очень слабый")
        bits < 36 -> Tr("weak", "слабый")
        bits < 60 -> Tr("reasonable", "приемлемый")
        bits < 80 -> Tr("strong", "надёжный")
        bits < 128 -> Tr("very strong", "очень надёжный")
        else -> Tr("excessive", "избыточный")
    }
}
