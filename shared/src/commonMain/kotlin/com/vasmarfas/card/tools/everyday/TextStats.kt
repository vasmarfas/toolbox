package com.vasmarfas.card.tools.everyday

data class TextStats(val chars: Int, val charsNoSpaces: Int, val words: Int, val lines: Int)

private val whitespace = Regex("\\s+")

fun textStats(text: String): TextStats = TextStats(
    chars = text.length,
    charsNoSpaces = text.count { !it.isWhitespace() },
    words = text.split(whitespace).count { it.isNotEmpty() },
    lines = if (text.isEmpty()) 0 else text.lines().size,
)
