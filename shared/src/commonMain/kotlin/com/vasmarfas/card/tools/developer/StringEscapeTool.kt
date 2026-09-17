package com.vasmarfas.card.tools.developer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
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
import com.vasmarfas.card.tools.text.OutputCard
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.SegmentedChoice
import com.vasmarfas.card.ui.components.ToolInputField

private enum class EscapeDirection { ESCAPE, UNESCAPE }

val stringEscapeTool = Tool(
    id = "string-escape",
    category = ToolCategory.DEVELOPER,
    title = Res.string.string_escape,
    description = Res.string.escape_and_unescape_strings_for_json_java_ko,
    icon = Icons.Filled.Code,
    keywords = listOf("escape", "unescape", "quote", "json", "html", "sql", "shell", "экранирование", "кавычки", "спецсимволы"),
) { StringEscapeScreen() }

@Composable
private fun StringEscapeScreen() {
    var target by rememberSaveable { mutableStateOf(EscapeTarget.JSON) }
    var direction by rememberSaveable { mutableStateOf(EscapeDirection.ESCAPE) }
    var input by rememberSaveable { mutableStateOf("") }
    ChoiceChips(options = EscapeTarget.entries, selected = target, onSelect = { target = it }, label = { it.title.str() })
    SegmentedChoice(
        options = EscapeDirection.entries,
        selected = direction,
        onSelect = { direction = it },
        label = { if (it == EscapeDirection.ESCAPE) Res.string.escape.str() else Res.string.unescape.str() },
    )
    ToolInputField(
        value = input,
        onValueChange = { input = it },
        label = if (direction == EscapeDirection.ESCAPE) Res.string.text.str() else Res.string.escaped_string.str(),
        singleLine = false,
        minLines = 4,
        monospace = direction == EscapeDirection.UNESCAPE,
    )
    if (input.isEmpty()) return
    val output = remember(input, target, direction) {
        if (direction == EscapeDirection.ESCAPE) StringEscapes.escape(input, target) else StringEscapes.unescape(input, target)
    }
    if (output == null) {
        ErrorText(Res.string.cannot_unescape_malformed_escape_sequence.str())
        return
    }
    OutputCard(output)
}
