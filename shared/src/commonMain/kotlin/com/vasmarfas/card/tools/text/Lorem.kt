package com.vasmarfas.card.tools.text

import com.vasmarfas.card.resources.Res
import com.vasmarfas.card.resources.latin
import com.vasmarfas.card.resources.paragraphs
import com.vasmarfas.card.resources.russian
import com.vasmarfas.card.resources.sentences
import com.vasmarfas.card.resources.words_2
import kotlin.random.Random
import org.jetbrains.compose.resources.StringResource

enum class LoremUnit(val title: StringResource) {
    PARAGRAPHS(Res.string.paragraphs),
    SENTENCES(Res.string.sentences),
    WORDS(Res.string.words_2),
}

enum class LoremLang(val title: StringResource) {
    LATIN(Res.string.latin),
    RUSSIAN(Res.string.russian),
}

object Lorem {
    val latinWords = listOf(
        "lorem", "ipsum", "dolor", "sit", "amet", "consectetur", "adipiscing", "elit", "sed", "do", "eiusmod", "tempor",
        "incididunt", "ut", "labore", "et", "dolore", "magna", "aliqua", "enim", "ad", "minim", "veniam", "quis", "nostrud",
        "exercitation", "ullamco", "laboris", "nisi", "aliquip", "ex", "ea", "commodo", "consequat", "duis", "aute", "irure",
        "in", "reprehenderit", "voluptate", "velit", "esse", "cillum", "fugiat", "nulla", "pariatur", "excepteur", "sint",
        "occaecat", "cupidatat", "non", "proident", "sunt", "culpa", "qui", "officia", "deserunt", "mollit", "anim", "id",
        "est", "laborum", "at", "vero", "eos", "accusamus", "iusto", "odio", "dignissimos", "ducimus", "blanditiis",
        "praesentium", "voluptatum", "deleniti", "atque", "corrupti", "quos", "dolores", "quas", "molestias", "excepturi",
        "obcaecati", "cupiditate", "provident", "similique", "mollitia", "animi", "perferendis", "doloribus", "asperiores",
        "repellat", "omnis", "voluptas", "assumenda", "temporibus", "autem", "quibusdam", "officiis", "debitis", "rerum",
        "necessitatibus", "saepe", "eveniet", "voluptates", "repudiandae", "recusandae", "itaque", "earum", "hic", "tenetur",
    )

    val russianWords = listOf(
        "далеко", "за", "словесными", "горами", "в", "стране", "гласных", "и", "согласных", "живут", "рыбные", "тексты",
        "вдали", "от", "всех", "они", "буквенных", "домах", "на", "берегу", "семантика", "большого", "языкового", "океана",
        "маленький", "ручеек", "даль", "журчит", "по", "всей", "обеспечивает", "ее", "всеми", "необходимыми", "правилами",
        "эта", "парадигматическая", "страна", "которой", "жаренные", "предложения", "залетают", "прямо", "рот", "даже",
        "всемогущая", "пунктуация", "не", "имеет", "власти", "над", "рыбными", "текстами", "ведущими", "безорфографичный",
        "образ", "жизни", "однажды", "одна", "строчка", "рыбного", "текста", "имени", "решила", "выйти", "большой", "мир",
        "грамматики", "великий", "оксмокс", "предупреждал", "о", "злых", "запятых", "диких", "знаках", "вопроса", "коварных",
        "точках", "запятой", "но", "текст", "послушался", "совета", "собрал", "семь", "своих", "заглавных", "букв",
        "подпоясал", "инициал", "вдоль", "пояса", "пустился", "дорогу", "взобравшись", "первую", "вершину", "курсивных",
        "гор", "бросил", "последний", "взгляд", "назад", "силуэт", "своего", "родного", "города", "буквоград", "заголовок",
        "деревни", "алфавит", "подзаголовок", "переулка", "грустный", "реторический", "вопрос", "скатился", "его", "щеке",
        "продолжил", "свой", "путь",
    )

    private const val LATIN_START = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
    private const val RUSSIAN_START = "Далеко-далеко за словесными горами в стране гласных и согласных живут рыбные тексты."

    fun generate(unit: LoremUnit, count: Int, lang: LoremLang, classicStart: Boolean, random: Random = Random.Default): String {
        val words = if (lang == LoremLang.LATIN) latinWords else russianWords
        val start = if (lang == LoremLang.LATIN) LATIN_START else RUSSIAN_START
        return when (unit) {
            LoremUnit.WORDS -> List(count) { words.random(random) }.joinToString(" ")
            LoremUnit.SENTENCES -> List(count) { i -> if (classicStart && i == 0) start else sentence(words, random) }.joinToString(" ")
            LoremUnit.PARAGRAPHS -> List(count) { i -> paragraph(words, random, if (classicStart && i == 0) start else null) }.joinToString("\n\n")
        }
    }

    private fun sentence(words: List<String>, random: Random): String {
        val n = random.nextInt(6, 15)
        val parts = MutableList(n) { words.random(random) }
        if (n > 8 && random.nextInt(3) == 0) {
            val commaAt = random.nextInt(3, n - 3)
            parts[commaAt] = parts[commaAt] + ","
        }
        return parts.joinToString(" ").replaceFirstChar { it.uppercaseChar() } + "."
    }

    private fun paragraph(words: List<String>, random: Random, start: String?): String {
        val n = random.nextInt(4, 8)
        return List(n) { i -> if (start != null && i == 0) start else sentence(words, random) }.joinToString(" ")
    }
}
