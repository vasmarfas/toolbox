package com.vasmarfas.card.tools.text

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.vasmarfas.card.core.fmtGrouped
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.ToolInputField

val textCounterTool = Tool(
    id = "text-counter",
    category = ToolCategory.TEXT,
    title = Res.string.text_counter,
    description = Res.string.characters_words_sentences_paragraphs_bytes,
    icon = Icons.Filled.Tag,
    keywords = listOf("count", "words", "characters", "statistics", "frequency", "reading time", "счётчик", "слова", "символы", "статистика", "частота"),
) { TextCounterScreen() }

@Composable
private fun TextCounterScreen() {
    var input by rememberSaveable { mutableStateOf("") }
    ToolInputField(
        value = input,
        onValueChange = { input = it },
        label = Res.string.text.str(),
        singleLine = false,
        minLines = 5,
    )
    val stats = remember(input) { TextStats.analyze(input) }
    ResultCard {
        KeyValueRow(Res.string.characters_2.str(), stats.chars.fmtGrouped(), copyable = false)
        KeyValueRow(Res.string.characters_without_spaces.str(), stats.charsNoSpaces.fmtGrouped(), copyable = false)
        KeyValueRow(Res.string.words.str(), stats.words.fmtGrouped(), copyable = false)
        KeyValueRow(Res.string.unique_words.str(), stats.uniqueWords.fmtGrouped(), copyable = false)
        KeyValueRow(Res.string.sentences_2.str(), stats.sentences.fmtGrouped(), copyable = false)
        KeyValueRow(Res.string.paragraphs_2.str(), stats.paragraphs.fmtGrouped(), copyable = false)
        KeyValueRow(Res.string.lines.str(), stats.lines.fmtGrouped(), copyable = false)
        KeyValueRow(Res.string.utf_8_bytes_2.str(), stats.utf8Bytes.fmtGrouped(), copyable = false)
        KeyValueRow(Res.string.reading_time_200_wpm.str(), durationText(stats.readingSeconds), copyable = false)
        KeyValueRow(Res.string.speaking_time_130_wpm.str(), durationText(stats.speakingSeconds), copyable = false)
    }
    if (stats.topWords.isNotEmpty()) {
        ResultCard(Res.string.top_words.str()) {
            stats.topWords.forEach { (word, count) ->
                KeyValueRow(word, count.fmtGrouped(), copyable = false)
            }
        }
    }
}

@Composable
private fun durationText(seconds: Int): String {
    val min = Res.string.min.str()
    val sec = Res.string.s.str()
    return if (seconds < 60) "$seconds $sec" else "${seconds / 60} $min ${seconds % 60} $sec"
}
