package com.vasmarfas.card.tools.text

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.Tr
import com.vasmarfas.card.core.fmt
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.ToolInputField
import com.vasmarfas.card.ui.theme.LocalStatusColors

private const val MAX_DIFF_LINES = 500

val textDiffTool = Tool(
    id = "text-diff",
    category = ToolCategory.TEXT,
    title = Res.string.text_diff,
    description = Res.string.line_by_line_comparison_of_two_texts_with_ad,
    icon = Icons.Filled.Difference,
    keywords = listOf("diff", "compare", "difference", "lcs", "similarity", "сравнить", "разница", "отличия", "схожесть"),
    expandable = true,
) { TextDiffScreen() }

@Composable
private fun TextDiffScreen() {
    var left by rememberSaveable { mutableStateOf("") }
    var right by rememberSaveable { mutableStateOf("") }
    ToolInputField(
        value = left,
        onValueChange = { left = it },
        label = Res.string.original.str(),
        singleLine = false,
        minLines = 4,
        monospace = true,
    )
    ToolInputField(
        value = right,
        onValueChange = { right = it },
        label = Res.string.modified.str(),
        singleLine = false,
        minLines = 4,
        monospace = true,
    )
    if (left.isEmpty() && right.isEmpty()) return
    val result = remember(left, right) { TextDiff.diffLines(left, right) }
    ResultCard {
        KeyValueRow(Res.string.similarity.str(), result.similarity.fmt(1) + " %", copyable = false)
        KeyValueRow(Res.string.added_lines.str(), result.added.toString(), copyable = false)
        KeyValueRow(Res.string.removed_lines.str(), result.removed.toString(), copyable = false)
    }
    val status = LocalStatusColors.current
    val addedColor = status.good.copy(alpha = 0.25f)
    val removedColor = status.bad.copy(alpha = 0.25f)
    val style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
    ResultCard(Res.string.differences.str()) {
        Column(Modifier.fillMaxWidth()) {
            result.lines.take(MAX_DIFF_LINES).forEach { line ->
                val (prefix, color) = when (line.kind) {
                    DiffKind.ADDED -> "+ " to addedColor
                    DiffKind.REMOVED -> "- " to removedColor
                    DiffKind.EQUAL -> "  " to Color.Transparent
                }
                Text(
                    prefix + line.text,
                    style = style,
                    modifier = Modifier.fillMaxWidth().background(color).padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }
        }
        if (result.lines.size > MAX_DIFF_LINES) {
            Text(Tr("Only the first $MAX_DIFF_LINES lines are shown.", "Показаны только первые $MAX_DIFF_LINES строк.").str(), style = MaterialTheme.typography.bodySmall)
        }
    }
}
