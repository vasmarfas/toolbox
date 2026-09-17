package com.vasmarfas.card.tools.text

import com.vasmarfas.card.resources.*
import org.jetbrains.compose.resources.StringResource

enum class TextCase(val title: StringResource) {
    UPPER(Res.string.upper_case),
    LOWER(Res.string.lower_case),
    TITLE(Res.string.title_case),
    SENTENCE(Res.string.sentence_case),
    CAMEL(Res.string.camelcase),
    // not PASCAL: the Objective-C export would be `pascal`, a clang calling-convention keyword
    PASCAL_CASE(Res.string.pascalcase),
    SNAKE(Res.string.snake_case),
    SCREAMING_SNAKE(Res.string.screaming_snake_case),
    KEBAB(Res.string.kebab_case),
    INVERT(Res.string.invert_case),
    ALTERNATING(Res.string.alternating),
}

object CaseConvert {
    fun words(text: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        for (i in text.indices) {
            val c = text[i]
            if (!c.isLetterOrDigit()) {
                if (current.isNotEmpty()) {
                    result += current.toString()
                    current.clear()
                }
                continue
            }
            if (current.isNotEmpty()) {
                val prev = text[i - 1]
                val next = text.getOrNull(i + 1)
                val boundary = c.isUpperCase() &&
                    (prev.isLowerCase() || prev.isDigit() || (prev.isUpperCase() && next != null && next.isLowerCase()))
                if (boundary) {
                    result += current.toString()
                    current.clear()
                }
            }
            current.append(c)
        }
        if (current.isNotEmpty()) result += current.toString()
        return result
    }

    fun convert(text: String, case: TextCase): String = when (case) {
        TextCase.UPPER -> text.uppercase()
        TextCase.LOWER -> text.lowercase()
        TextCase.TITLE -> title(text)
        TextCase.SENTENCE -> sentence(text)
        TextCase.CAMEL -> words(text).mapIndexed { i, w -> if (i == 0) w.lowercase() else capitalize(w) }.joinToString("")
        TextCase.PASCAL_CASE -> words(text).joinToString("") { capitalize(it) }
        TextCase.SNAKE -> words(text).joinToString("_") { it.lowercase() }
        TextCase.SCREAMING_SNAKE -> words(text).joinToString("_") { it.uppercase() }
        TextCase.KEBAB -> words(text).joinToString("-") { it.lowercase() }
        TextCase.INVERT -> text.map { if (it.isUpperCase()) it.lowercaseChar() else it.uppercaseChar() }.joinToString("")
        TextCase.ALTERNATING -> alternating(text)
    }

    private fun capitalize(word: String): String = word.lowercase().replaceFirstChar { it.uppercaseChar() }

    private fun title(text: String): String {
        val sb = StringBuilder(text.length)
        var startOfWord = true
        for (c in text) {
            if (c.isLetterOrDigit()) {
                sb.append(if (startOfWord) c.uppercaseChar() else c.lowercaseChar())
                startOfWord = false
            } else {
                sb.append(c)
                startOfWord = c != '\'' && c != '’'
            }
        }
        return sb.toString()
    }

    private fun sentence(text: String): String {
        val sb = StringBuilder(text.length)
        var capitalizeNext = true
        for (c in text) {
            if (c.isLetter()) {
                sb.append(if (capitalizeNext) c.uppercaseChar() else c.lowercaseChar())
                capitalizeNext = false
            } else {
                sb.append(c)
                if (c == '.' || c == '!' || c == '?' || c == '…' || c == '\n') capitalizeNext = true
            }
        }
        return sb.toString()
    }

    private fun alternating(text: String): String {
        val sb = StringBuilder(text.length)
        var upper = false
        for (c in text) {
            if (c.isLetter()) {
                sb.append(if (upper) c.uppercaseChar() else c.lowercaseChar())
                upper = !upper
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }
}
