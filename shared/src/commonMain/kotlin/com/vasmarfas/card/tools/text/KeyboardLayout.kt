package com.vasmarfas.card.tools.text

import com.vasmarfas.card.resources.*
import org.jetbrains.compose.resources.StringResource

enum class LayoutDirection(val title: StringResource) {
    AUTO(Res.string.auto),
    EN_TO_RU(Res.string.to_ycuken),
    RU_TO_EN(Res.string.to_qwerty),
}

object KeyboardLayout {
    private const val LATIN_LOWER = "`qwertyuiop[]asdfghjkl;'zxcvbnm,./"
    private const val CYRILLIC_LOWER = "ёйцукенгшщзхъфывапролджэячсмитьбю."
    private const val LATIN_UPPER = "~QWERTYUIOP{}ASDFGHJKL:\"ZXCVBNM<>?"
    private const val CYRILLIC_UPPER = "ЁЙЦУКЕНГШЩЗХЪФЫВАПРОЛДЖЭЯЧСМИТЬБЮ,"
    private const val LATIN_SHIFTED = "@#\$^&|"
    private const val CYRILLIC_SHIFTED = "\"№;:?/"

    val enToRu: Map<Char, Char> = buildMap {
        LATIN_LOWER.forEachIndexed { index, char -> put(char, CYRILLIC_LOWER[index]) }
        LATIN_UPPER.forEachIndexed { index, char -> put(char, CYRILLIC_UPPER[index]) }
        LATIN_SHIFTED.forEachIndexed { index, char -> put(char, CYRILLIC_SHIFTED[index]) }
    }

    val ruToEn: Map<Char, Char> = enToRu.entries.associate { (latin, cyrillic) -> cyrillic to latin }

    fun cyrillicCount(text: String): Int = text.count { it in 'а'..'я' || it in 'А'..'Я' || it == 'ё' || it == 'Ё' }

    fun latinCount(text: String): Int = text.count { it in 'a'..'z' || it in 'A'..'Z' }

    fun resolve(text: String, direction: LayoutDirection): LayoutDirection = when {
        direction != LayoutDirection.AUTO -> direction
        cyrillicCount(text) > latinCount(text) -> LayoutDirection.RU_TO_EN
        else -> LayoutDirection.EN_TO_RU
    }

    fun convert(text: String, direction: LayoutDirection): String {
        val map = if (resolve(text, direction) == LayoutDirection.RU_TO_EN) ruToEn else enToRu
        return buildString(text.length) { text.forEach { append(map[it] ?: it) } }
    }
}
