package com.vasmarfas.card.tools.text

enum class MorseAlphabet { LATIN, CYRILLIC }

data class MorseStep(val tone: Boolean, val ms: Int)

object Morse {
    const val UNIT_MS = 80

    private val latin = mapOf(
        'a' to ".-", 'b' to "-...", 'c' to "-.-.", 'd' to "-..", 'e' to ".", 'f' to "..-.", 'g' to "--.", 'h' to "....",
        'i' to "..", 'j' to ".---", 'k' to "-.-", 'l' to ".-..", 'm' to "--", 'n' to "-.", 'o' to "---", 'p' to ".--.",
        'q' to "--.-", 'r' to ".-.", 's' to "...", 't' to "-", 'u' to "..-", 'v' to "...-", 'w' to ".--", 'x' to "-..-",
        'y' to "-.--", 'z' to "--..",
    )

    private val cyrillic = mapOf(
        'а' to ".-", 'б' to "-...", 'в' to ".--", 'г' to "--.", 'д' to "-..", 'е' to ".", 'ё' to ".", 'ж' to "...-",
        'з' to "--..", 'и' to "..", 'й' to ".---", 'к' to "-.-", 'л' to ".-..", 'м' to "--", 'н' to "-.", 'о' to "---",
        'п' to ".--.", 'р' to ".-.", 'с' to "...", 'т' to "-", 'у' to "..-", 'ф' to "..-.", 'х' to "....", 'ц' to "-.-.",
        'ч' to "---.", 'ш' to "----", 'щ' to "--.-", 'ъ' to "--.--", 'ы' to "-.--", 'ь' to "-..-", 'э' to "..-..",
        'ю' to "..--", 'я' to ".-.-",
    )

    private val common = mapOf(
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-", '5' to ".....", '6' to "-....",
        '7' to "--...", '8' to "---..", '9' to "----.",
        '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '\'' to ".----.", '!' to "-.-.--", '/' to "-..-.",
        '(' to "-.--.", ')' to "-.--.-", '&' to ".-...", ':' to "---...", ';' to "-.-.-.", '=' to "-...-", '+' to ".-.-.",
        '-' to "-....-", '_' to "..--.-", '"' to ".-..-.", '$' to "...-..-", '@' to ".--.-.",
    )

    private val latinDecode = (latin + common).entries.associate { it.value to it.key }
    private val cyrillicDecode = (cyrillic.filterKeys { it != 'ё' } + common).entries.associate { it.value to it.key }

    fun encode(text: String): String {
        val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return words.joinToString(" / ") { word ->
            word.mapNotNull { c ->
                val lower = c.lowercaseChar()
                latin[lower] ?: cyrillic[lower] ?: common[lower]
            }.joinToString(" ")
        }
    }

    fun decode(morse: String, alphabet: MorseAlphabet): String {
        val table = if (alphabet == MorseAlphabet.LATIN) latinDecode else cyrillicDecode
        val normalized = morse.replace('•', '.').replace('·', '.').replace('—', '-').replace('–', '-').replace('_', '-')
        return normalized.split('/', '|').joinToString(" ") { word ->
            word.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString("") { code ->
                table[code]?.toString() ?: "�"
            }
        }.trim()
    }

    fun schedule(morse: String): List<MorseStep> {
        val steps = mutableListOf<MorseStep>()
        var i = 0
        while (i < morse.length) {
            when (morse[i]) {
                '.' -> {
                    steps += MorseStep(true, UNIT_MS)
                    steps += MorseStep(false, UNIT_MS)
                }
                '-' -> {
                    steps += MorseStep(true, UNIT_MS * 3)
                    steps += MorseStep(false, UNIT_MS)
                }
                '/' -> steps += MorseStep(false, UNIT_MS * 7)
                ' ' -> {
                    var j = i
                    while (j < morse.length && morse[j] == ' ') j++
                    if (j < morse.length && morse[j] != '/') steps += MorseStep(false, UNIT_MS * 2)
                    i = j
                    continue
                }
            }
            i++
        }
        return steps
    }
}
