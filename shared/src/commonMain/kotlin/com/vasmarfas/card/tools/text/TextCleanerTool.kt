package com.vasmarfas.card.tools.text

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.SwitchRow
import com.vasmarfas.card.ui.components.ToolInputField

data class CleanOptions(
    val collapseWhitespace: Boolean = true,
    val trimLines: Boolean = true,
    val removeLineBreaks: Boolean = false,
    val stripHtml: Boolean = false,
    val removeNonPrintable: Boolean = true,
    val removeDiacritics: Boolean = false,
    val normalizePunctuation: Boolean = false,
    val removeEmoji: Boolean = false,
)

fun cleanText(text: String, options: CleanOptions): String {
    var t = text
    if (options.stripHtml) t = TextCleaner.stripHtml(t)
    if (options.removeEmoji) t = TextCleaner.removeEmoji(t)
    if (options.removeNonPrintable) t = TextCleaner.removeNonPrintable(t)
    if (options.removeDiacritics) t = TextCleaner.removeDiacritics(t)
    if (options.normalizePunctuation) t = TextCleaner.normalizePunctuation(t)
    if (options.removeLineBreaks) t = t.replace("\r\n", "\n").replace('\r', '\n').replace('\n', ' ')
    if (options.trimLines) t = t.lines().joinToString("\n") { it.trim() }
    if (options.collapseWhitespace) t = TextCleaner.collapseWhitespace(t)
    return t
}

val textCleanerTool = Tool(
    id = "text-cleaner",
    category = ToolCategory.TEXT,
    title = Res.string.text_cleaner,
    description = Res.string.text_cleaner_description,
    icon = Icons.Filled.CleaningServices,
    keywords = listOf("clean", "whitespace", "trim", "html", "strip", "emoji", "diacritics", "quotes", "очистка", "пробелы", "теги", "эмодзи", "кавычки"),
) { TextCleanerScreen() }

@Composable
private fun TextCleanerScreen() {
    var input by rememberSaveable { mutableStateOf("") }
    var options by remember { mutableStateOf(CleanOptions()) }
    ToolInputField(
        value = input,
        onValueChange = { input = it },
        label = Res.string.text.str(),
        singleLine = false,
        minLines = 5,
    )
    SwitchRow(Res.string.collapse_whitespace.str(), options.collapseWhitespace, { options = options.copy(collapseWhitespace = it) })
    SwitchRow(Res.string.cleaner_trim_lines.str(), options.trimLines, { options = options.copy(trimLines = it) })
    SwitchRow(Res.string.remove_line_breaks.str(), options.removeLineBreaks, { options = options.copy(removeLineBreaks = it) })
    SwitchRow(Res.string.strip_html_tags.str(), options.stripHtml, { options = options.copy(stripHtml = it) })
    SwitchRow(Res.string.remove_non_printable_characters.str(), options.removeNonPrintable, { options = options.copy(removeNonPrintable = it) })
    SwitchRow(Res.string.remove_diacritics_e.str(), options.removeDiacritics, { options = options.copy(removeDiacritics = it) })
    SwitchRow(Res.string.normalize_quotes_and_dashes.str(), options.normalizePunctuation, { options = options.copy(normalizePunctuation = it) })
    SwitchRow(Res.string.remove_emoji.str(), options.removeEmoji, { options = options.copy(removeEmoji = it) })
    if (input.isNotEmpty()) {
        val output = remember(input, options) { cleanText(input, options) }
        OutputCard(output)
        KeyValueRow(Res.string.length_before_after.str(), "${input.length} → ${output.length}", copyable = false)
    }
}
