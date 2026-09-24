package com.vasmarfas.card.tools.text

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SegmentedChoice
import com.vasmarfas.card.ui.components.ToolInputField

val keyboardLayoutTool = Tool(
    id = "keyboard-layout",
    category = ToolCategory.TEXT,
    title = Res.string.keyboard_layout,
    description = Res.string.keyboard_layout_description,
    icon = Icons.Filled.SwapHoriz,
    keywords = listOf("layout", "keyboard", "qwerty", "wrong layout", "ghbdtn", "раскладка", "клавиатура", "йцукен", "не та раскладка", "руддщ"),
) { KeyboardLayoutScreen() }

@Composable
private fun KeyboardLayoutScreen() {
    var input by rememberSaveable { mutableStateOf("") }
    var direction by rememberSaveable { mutableStateOf(LayoutDirection.AUTO) }
    SegmentedChoice(
        options = LayoutDirection.entries,
        selected = direction,
        onSelect = { direction = it },
        label = { it.title.str() },
    )
    ToolInputField(
        value = input,
        onValueChange = { input = it },
        label = Res.string.text.str(),
        singleLine = false,
        minLines = 3,
        placeholder = "ghbdtn · руддщ",
    )
    Text(
        Res.string.layout_russian_written_in_latin.str(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (input.isEmpty()) return
    val resolved = remember(input, direction) { KeyboardLayout.resolve(input, direction) }
    OutputCard(remember(input, direction) { KeyboardLayout.convert(input, direction) })
    ResultCard {
        KeyValueRow(Res.string.direction.str(), resolved.title.str(), mono = false, copyable = false)
        KeyValueRow(
            Res.string.cyrillic_latin_letters.str(),
            "${KeyboardLayout.cyrillicCount(input)} / ${KeyboardLayout.latinCount(input)}",
            copyable = false,
        )
    }
}
