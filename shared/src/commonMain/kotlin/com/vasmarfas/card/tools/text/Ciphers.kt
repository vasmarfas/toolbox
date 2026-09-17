package com.vasmarfas.card.tools.text

import com.vasmarfas.card.resources.*
import org.jetbrains.compose.resources.StringResource

enum class Cipher(val title: StringResource) {
    CAESAR(Res.string.caesar),
    ROT13(Res.string.rot13),
    ROT47(Res.string.rot47),
    ATBASH(Res.string.atbash),
    VIGENERE(Res.string.vigen_re),
    REVERSE(Res.string.reverse),
}

object Ciphers {
    const val LATIN = "abcdefghijklmnopqrstuvwxyz"
    const val CYRILLIC = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя"

    private fun alphabetOf(c: Char): String? {
        val lower = c.lowercaseChar()
        return when {
            lower in LATIN -> LATIN
            lower in CYRILLIC -> CYRILLIC
            else -> null
        }
    }

    private fun shiftChar(c: Char, shift: Int, alphabet: String): Char {
        val idx = alphabet.indexOf(c.lowercaseChar())
        val n = alphabet.length
        val shifted = alphabet[((idx + shift) % n + n) % n]
        return if (c.isUpperCase()) shifted.uppercaseChar() else shifted
    }

    fun caesar(text: String, shift: Int): String = text.map { c ->
        val alphabet = alphabetOf(c)
        if (alphabet == null) c else shiftChar(c, shift, alphabet)
    }.joinToString("")

    fun rot13(text: String): String = text.map { c ->
        if (c.lowercaseChar() in LATIN) shiftChar(c, 13, LATIN) else c
    }.joinToString("")

    fun rot47(text: String): String = text.map { c ->
        if (c.code in 33..126) (33 + (c.code - 33 + 47) % 94).toChar() else c
    }.joinToString("")

    fun atbash(text: String): String = text.map { c ->
        val alphabet = alphabetOf(c)
        if (alphabet == null) {
            c
        } else {
            val mirrored = alphabet[alphabet.length - 1 - alphabet.indexOf(c.lowercaseChar())]
            if (c.isUpperCase()) mirrored.uppercaseChar() else mirrored
        }
    }.joinToString("")

    fun vigenere(text: String, key: String, decrypt: Boolean): String {
        val keyLetters = key.filter { alphabetOf(it) != null }
        if (keyLetters.isEmpty()) return text
        val sb = StringBuilder(text.length)
        var k = 0
        for (c in text) {
            val alphabet = alphabetOf(c)
            if (alphabet == null) {
                sb.append(c)
                continue
            }
            val keyChar = keyLetters[k % keyLetters.length]
            val shift = alphabetOf(keyChar)!!.indexOf(keyChar.lowercaseChar())
            sb.append(shiftChar(c, if (decrypt) -shift else shift, alphabet))
            k++
        }
        return sb.toString()
    }

    fun bruteForce(text: String): List<Pair<Int, String>> {
        val max = if (text.any { it.lowercaseChar() in CYRILLIC }) CYRILLIC.length else LATIN.length
        return (1 until max).map { it to caesar(text, -it) }
    }
}
