package com.vasmarfas.card.tools.text

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
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
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.SegmentedChoice
import com.vasmarfas.card.ui.components.ToolInputField

private enum class TranslitDirection { TO_LATIN, TO_CYRILLIC }

val transliterationTool = Tool(
    id = "transliteration",
    category = ToolCategory.TEXT,
    title = Res.string.transliteration,
    description = Res.string.russian_latin_by_the_passport_icao_gost_7_79,
    icon = Icons.Filled.Translate,
    keywords = listOf("translit", "transliteration", "latin", "cyrillic", "slug", "passport", "gost", "транслит", "латиница", "кириллица", "паспорт", "гост"),
) { TransliterationScreen() }

@Composable
private fun TransliterationScreen() {
    var input by rememberSaveable { mutableStateOf("") }
    var direction by rememberSaveable { mutableStateOf(TranslitDirection.TO_LATIN) }
    var scheme by rememberSaveable { mutableStateOf(TranslitScheme.PASSPORT) }
    SegmentedChoice(
        options = TranslitDirection.entries,
        selected = direction,
        onSelect = { direction = it },
        label = { if (it == TranslitDirection.TO_LATIN) Res.string.russian_latin.str() else Res.string.latin_russian.str() },
    )
    ChoiceChips(options = TranslitScheme.entries, selected = scheme, onSelect = { scheme = it }, label = { it.title.str() })
    ToolInputField(
        value = input,
        onValueChange = { input = it },
        label = Res.string.text.str(),
        singleLine = false,
        minLines = 3,
    )
    val output = remember(input, direction, scheme) {
        if (direction == TranslitDirection.TO_LATIN) Translit.toLatin(input, scheme) else Translit.toCyrillic(input, scheme)
    }
    if (input.isNotEmpty()) {
        OutputCard(output)
        if (direction == TranslitDirection.TO_LATIN) {
            KeyValueRow(Res.string.slug.str(), Translit.slugify(input))
        }
    }
}
