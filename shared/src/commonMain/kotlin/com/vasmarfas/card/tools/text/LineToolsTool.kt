package com.vasmarfas.card.tools.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.SwitchRow
import com.vasmarfas.card.ui.components.ToolInputField

val lineToolsTool = Tool(
    id = "line-tools",
    category = ToolCategory.TEXT,
    title = Res.string.line_tools,
    description = Res.string.sort_dedupe_reverse_shuffle_trim_number_line,
    icon = Icons.AutoMirrored.Filled.Sort,
    keywords = listOf("sort", "dedupe", "unique", "shuffle", "reverse", "lines", "join", "split", "сортировка", "дубликаты", "строки", "перемешать"),
) { LineToolsScreen() }

@Composable
private fun LineToolsScreen() {
    var input by rememberSaveable { mutableStateOf("") }
    var op by rememberSaveable { mutableStateOf(LineOp.SORT_ASC) }
    var ignoreCase by rememberSaveable { mutableStateOf(false) }
    var prefix by rememberSaveable { mutableStateOf("") }
    var suffix by rememberSaveable { mutableStateOf("") }
    var separator by rememberSaveable { mutableStateOf(", ") }
    var shuffleSeed by remember { mutableStateOf(0) }
    ToolInputField(
        value = input,
        onValueChange = { input = it },
        label = Res.string.lines_2.str(),
        singleLine = false,
        minLines = 5,
        monospace = true,
    )
    ChoiceChips(options = LineOp.entries, selected = op, onSelect = { op = it }, label = { it.title.str() })
    when (op) {
        LineOp.SORT_ASC, LineOp.SORT_DESC, LineOp.DEDUPE ->
            SwitchRow(Res.string.ignore_case.str(), ignoreCase, { ignoreCase = it })

        LineOp.PREFIX_SUFFIX -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolInputField(value = prefix, onValueChange = { prefix = it }, label = Res.string.prefix.str(), modifier = Modifier.weight(1f))
            ToolInputField(value = suffix, onValueChange = { suffix = it }, label = Res.string.suffix.str(), modifier = Modifier.weight(1f))
        }

        LineOp.JOIN, LineOp.SPLIT -> ToolInputField(
            value = separator,
            onValueChange = { separator = it },
            label = Res.string.separator_t_n_allowed.str(),
            monospace = true,
        )

        LineOp.SHUFFLE -> ActionButton(Res.string.shuffle_again.str(), onClick = { shuffleSeed++ })

        else -> {}
    }
    val output = remember(input, op, ignoreCase, prefix, suffix, separator, shuffleSeed) {
        LineOps.apply(input, op, LineOptions(ignoreCase, prefix, suffix, separator))
    }
    if (input.isNotEmpty()) {
        OutputCard(output)
    }
}
