package com.vasmarfas.card.tools.security

import com.vasmarfas.card.core.Tr
import kotlin.math.ln
import kotlin.math.max

data class StrengthIssue(val text: Tr, val penaltyBits: Double)

data class StrengthResult(
    val length: Int,
    val alphabetSize: Int,
    val rawBits: Double,
    val bits: Double,
    val issues: List<StrengthIssue>,
    val suggestions: List<Tr>,
) {
    val score: Int
        get() = when {
            bits < 28 -> 0
            bits < 36 -> 1
            bits < 60 -> 2
            bits < 80 -> 3
            else -> 4
        }
}

object PasswordStrength {
    private val keyboardRows = listOf(
        "qwertyuiop", "asdfghjkl", "zxcvbnm", "1234567890",
        "йцукенгшщзхъ", "фывапролджэ", "ячсмитьбю",
    )

    private val common = setOf(
        "password", "passw0rd", "qwerty", "123456", "12345678", "123456789", "1234567890", "letmein", "admin",
        "welcome", "monkey", "dragon", "football", "baseball", "iloveyou", "sunshine", "princess", "master",
        "shadow", "superman", "batman", "trustno1", "abc123", "111111", "000000", "zaq12wsx", "qazwsx",
        "пароль", "привет", "любовь", "россия", "спартак", "зенит", "динамо", "москва", "йцукен", "какдела",
        "solnce", "natasha", "sergey", "andrey", "marina", "alexander", "dmitry", "hello", "test", "secret",
    )

    private val leetMap = mapOf('0' to 'o', '1' to 'i', '3' to 'e', '4' to 'a', '5' to 's', '7' to 't', '@' to 'a', '$' to 's', '!' to 'i')

    fun normalize(password: String): String = password.lowercase().map { leetMap[it] ?: it }.joinToString("")

    fun alphabetSize(password: String): Int {
        var size = 0
        if (password.any { it in 'a'..'z' }) size += 26
        if (password.any { it in 'A'..'Z' }) size += 26
        if (password.any { it.isDigit() }) size += 10
        if (password.any { it in PasswordGen.SYMBOLS }) size += PasswordGen.SYMBOLS.length
        if (password.any { it.code in 0x400..0x4FF }) size += 33
        if (password.any { it.code > 0x4FF || (it.code > 0x7E && it.code < 0x400) }) size += 20
        if (password.any { it == ' ' }) size += 1
        return max(size, 1)
    }

    fun longestRepeat(password: String): Int {
        var best = 1
        var current = 1
        for (i in 1 until password.length) {
            current = if (password[i] == password[i - 1]) current + 1 else 1
            if (current > best) best = current
        }
        return if (password.isEmpty()) 0 else best
    }

    fun longestSequence(password: String): Int {
        if (password.length < 2) return password.length
        var best = 1
        var current = 1
        var direction = 0
        for (i in 1 until password.length) {
            val delta = password[i].code - password[i - 1].code
            if ((delta == 1 || delta == -1) && (direction == 0 || direction == delta)) {
                current++
                direction = delta
            } else {
                current = 1
                direction = 0
            }
            if (current > best) best = current
        }
        return best
    }

    fun keyboardWalk(password: String): Int {
        val lower = password.lowercase()
        var best = 1
        for (row in keyboardRows) {
            for (start in lower.indices) {
                for (end in start + 2..lower.length) {
                    val part = lower.substring(start, end)
                    if (row.contains(part) || row.contains(part.reversed())) {
                        if (part.length > best) best = part.length
                    }
                }
            }
        }
        return best
    }

    fun hasDate(password: String): Boolean {
        val years = Regex("(19\\d{2}|20[0-2]\\d)")
        if (years.containsMatchIn(password)) return true
        return Regex("\\b\\d{2}[.\\-/]\\d{2}[.\\-/]\\d{2,4}\\b").containsMatchIn(password)
    }

    fun dictionaryHit(password: String): String? {
        val normalized = normalize(password)
        common.firstOrNull { normalized == it }?.let { return it }
        common.firstOrNull { it.length >= 5 && normalized.contains(it) }?.let { return it }
        val stripped = normalized.trimEnd('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '!', '?', '.')
        if (stripped.length >= 4 && stripped in common) return stripped
        val word = (WordLists.english + WordLists.russian).firstOrNull { it.length >= 5 && it == stripped }
        return word
    }

    fun analyze(password: String): StrengthResult {
        if (password.isEmpty()) {
            return StrengthResult(0, 0, 0.0, 0.0, emptyList(), listOf(Tr("Enter a password.", "Введите пароль.")))
        }
        val alphabet = alphabetSize(password)
        val rawBits = password.length * ln(alphabet.toDouble()) / ln(2.0)
        val issues = mutableListOf<StrengthIssue>()
        val suggestions = mutableListOf<Tr>()
        if (password.length < 12) {
            suggestions += Tr("Use at least 12 characters, 16 or more is better.", "Используйте не менее 12 символов, лучше 16 и больше.")
        }
        val repeat = longestRepeat(password)
        if (repeat >= 3) {
            issues += StrengthIssue(Tr("$repeat identical characters in a row", "$repeat одинаковых символа подряд"), repeat * 2.0)
            suggestions += Tr("Avoid repeating the same character.", "Не повторяйте один символ несколько раз подряд.")
        }
        val sequence = longestSequence(password)
        if (sequence >= 4) {
            issues += StrengthIssue(Tr("sequence of $sequence characters (abcd, 1234)", "последовательность из $sequence символов (abcd, 1234)"), sequence * 2.5)
            suggestions += Tr("Avoid alphabet or digit sequences.", "Не используйте последовательности букв или цифр.")
        }
        val walk = keyboardWalk(password)
        if (walk >= 4) {
            issues += StrengthIssue(Tr("keyboard walk of $walk characters (qwerty)", "проход по клавиатуре из $walk символов (qwerty)"), walk * 2.5)
            suggestions += Tr("Do not type neighbouring keys in a row.", "Не набирайте соседние клавиши подряд.")
        }
        if (hasDate(password)) {
            issues += StrengthIssue(Tr("contains a year or a date", "содержит год или дату"), 8.0)
            suggestions += Tr("Dates and birth years are guessed first.", "Даты и годы рождения подбирают в первую очередь.")
        }
        val hit = dictionaryHit(password)
        if (hit != null) {
            issues += StrengthIssue(Tr("dictionary word: \"$hit\"", "словарное слово: «$hit»"), 14.0)
            suggestions += Tr("Do not build the password around a single word.", "Не стройте пароль вокруг одного слова.")
        }
        if (password.length >= 4 && password.toSet().size <= 2) {
            issues += StrengthIssue(Tr("only ${password.toSet().size} distinct characters", "всего ${password.toSet().size} различных символа"), 10.0)
        }
        if (alphabet <= 26) {
            suggestions += Tr("Mix letter cases, digits and symbols.", "Смешивайте регистры, цифры и спецсимволы.")
        }
        if (Regex("^[A-ZА-ЯЁ][a-zа-яё]+\\d{1,4}[!?.]?$").matches(password)) {
            issues += StrengthIssue(Tr("predictable pattern: word + digits", "предсказуемый шаблон: слово + цифры"), 10.0)
        }
        val penalty = issues.sumOf { it.penaltyBits }
        val bits = max(rawBits - penalty, if (password.isEmpty()) 0.0 else 1.0)
        if (issues.isEmpty() && password.length >= 16) {
            suggestions += Tr("Good password. Store it in a password manager.", "Хороший пароль. Храните его в менеджере паролей.")
        }
        if (suggestions.isEmpty()) {
            suggestions += Tr("Add length: every extra character multiplies the search space.", "Добавьте длину: каждый лишний символ умножает пространство перебора.")
        }
        return StrengthResult(password.length, alphabet, rawBits, bits, issues, suggestions.distinctBy { it.en })
    }

    fun scoreLabel(score: Int): Tr = when (score) {
        0 -> Tr("very weak", "очень слабый")
        1 -> Tr("weak", "слабый")
        2 -> Tr("medium", "средний")
        3 -> Tr("strong", "надёжный")
        else -> Tr("very strong", "очень надёжный")
    }
}
