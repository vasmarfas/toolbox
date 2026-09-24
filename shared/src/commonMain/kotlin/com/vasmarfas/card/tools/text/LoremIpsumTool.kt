package com.vasmarfas.card.tools.text

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Refresh
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
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.SegmentedChoice
import com.vasmarfas.card.ui.components.SwitchRow

val loremIpsumTool = Tool(
    id = "lorem-ipsum",
    category = ToolCategory.TEXT,
    title = Res.string.lorem_ipsum,
    description = Res.string.lorem_ipsum_description,
    icon = Icons.AutoMirrored.Filled.Article,
    keywords = listOf("lorem", "ipsum", "placeholder", "filler", "dummy text", "рыба", "заполнитель", "текст"),
) { LoremIpsumScreen() }

@Composable
private fun LoremIpsumScreen() {
    var unit by rememberSaveable { mutableStateOf(LoremUnit.PARAGRAPHS) }
    var lang by rememberSaveable { mutableStateOf(LoremLang.LATIN) }
    var countText by rememberSaveable { mutableStateOf("3") }
    var classicStart by rememberSaveable { mutableStateOf(true) }
    var seed by remember { mutableStateOf(0) }
    SegmentedChoice(options = LoremLang.entries, selected = lang, onSelect = { lang = it }, label = { it.title.str() })
    SegmentedChoice(options = LoremUnit.entries, selected = unit, onSelect = { unit = it }, label = { it.title.str() })
    val count = countText.trim().toIntOrNull()
    NumberField(
        value = countText,
        onValueChange = { countText = it },
        label = Res.string.count.str(),
        isError = count == null || count !in 1..200,
    )
    SwitchRow(Res.string.start_with_the_classic_opening.str(), classicStart, { classicStart = it })
    ActionButton(Res.string.regenerate.str(), onClick = { seed++ }, icon = Icons.Filled.Refresh)
    if (count != null && count in 1..200) {
        val output = remember(unit, lang, count, classicStart, seed) { Lorem.generate(unit, count, lang, classicStart) }
        OutputCard(output)
    }
}
