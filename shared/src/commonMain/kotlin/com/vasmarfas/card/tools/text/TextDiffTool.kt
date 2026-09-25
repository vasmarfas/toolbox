package com.vasmarfas.card.tools.text

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.TextDecoding
import com.vasmarfas.card.core.fmt
import com.vasmarfas.card.core.pickFiles
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.CopyIconButton
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SegmentedChoice
import com.vasmarfas.card.ui.components.SwitchRow
import com.vasmarfas.card.ui.components.ToolInputField
import com.vasmarfas.card.ui.components.monoFamily
import com.vasmarfas.card.ui.theme.LocalStatusColors
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource

private const val CONTEXT_LINES = 3
private const val MAX_SHOWN_ROWS = 1500

// both texts live in saved state, an Android bundle does not hold much more
private const val MAX_FILE_BYTES = 512_000L

private val textExtensions = setOf(
    "txt", "md", "csv", "tsv", "json", "xml", "html", "htm", "css", "js", "ts", "kt", "kts", "java", "py", "c", "h", "cpp", "hpp", "cs",
    "go", "rs", "swift", "rb", "php", "sh", "bat", "ps1", "yml", "yaml", "toml", "ini", "cfg", "conf", "log", "sql", "gradle", "properties", "srt", "tex",
)

val textDiffTool = Tool(
    id = "text-diff",
    category = ToolCategory.TEXT,
    title = Res.string.text_diff,
    description = Res.string.text_diff_description,
    icon = Icons.Filled.Difference,
    keywords = listOf(
        "diff", "compare", "difference", "patch", "git", "lcs", "similarity",
        "сравнить", "сравнение", "разница", "отличия", "изменения", "схожесть",
    ),
    expandable = true,
) { TextDiffScreen() }

private enum class DiffLayout { SIDE_BY_SIDE, UNIFIED }

private sealed interface DiffItem {
    data class Line(val index: Int) : DiffItem
    data class Fold(val from: Int, val to: Int) : DiffItem
}

private class DiffPalette(good: Color, bad: Color, info: Color) {
    val added = good.copy(alpha = 0.16f)
    val removed = bad.copy(alpha = 0.16f)
    val changed = info.copy(alpha = 0.16f)
    val addedWord = good.copy(alpha = 0.4f)
    val removedWord = bad.copy(alpha = 0.4f)
    val changedWord = info.copy(alpha = 0.38f)
    val addedGap = good.copy(alpha = 0.06f)
    val removedGap = bad.copy(alpha = 0.06f)
}

@Composable
private fun TextDiffScreen() {
    var left by rememberSaveable { mutableStateOf("") }
    var right by rememberSaveable { mutableStateOf("") }
    var layout by rememberSaveable { mutableStateOf(DiffLayout.SIDE_BY_SIDE) }
    var ignoreCase by rememberSaveable { mutableStateOf(false) }
    var ignoreWhitespace by rememberSaveable { mutableStateOf(false) }
    var collapse by rememberSaveable { mutableStateOf(true) }
    var fileError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun open(into: (String) -> Unit) {
        scope.launch {
            val file = runCatching { pickFiles(textExtensions) }.getOrNull()?.firstOrNull() ?: return@launch
            fileError = null
            if (file.size() > MAX_FILE_BYTES) {
                fileError = getString(Res.string.diff_file_too_large)
                return@launch
            }
            runCatching { TextDecoding.decode(file.readBytes()) }
                .onSuccess(into)
                .onFailure { fileError = it.message ?: it.toString() }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = { open { left = it } }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(Res.string.open_old_file.str(), maxLines = 1)
        }
        TextButton(onClick = { open { right = it } }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(Res.string.open_new_file.str(), maxLines = 1)
        }
    }
    fileError?.let { ErrorText(it) }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = left,
            onValueChange = { left = it },
            label = Res.string.original.str(),
            modifier = Modifier.weight(1f),
            singleLine = false,
            minLines = 6,
            maxLines = 14,
            monospace = true,
            textStyle = MaterialTheme.typography.bodySmall,
        )
        ToolInputField(
            value = right,
            onValueChange = { right = it },
            label = Res.string.modified.str(),
            modifier = Modifier.weight(1f),
            singleLine = false,
            minLines = 6,
            maxLines = 14,
            monospace = true,
            textStyle = MaterialTheme.typography.bodySmall,
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = {
            val old = left
            left = right
            right = old
        }) {
            Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(Res.string.swap.str())
        }
        TextButton(onClick = {
            left = ""
            right = ""
        }) { Text(Res.string.clear.str()) }
    }
    SwitchRow(Res.string.ignore_case.str(), ignoreCase, { ignoreCase = it })
    SwitchRow(Res.string.ignore_whitespace.str(), ignoreWhitespace, { ignoreWhitespace = it })
    SwitchRow(Res.string.diff_collapse_unchanged.str(), collapse, { collapse = it })
    if (left.isEmpty() && right.isEmpty()) return

    val result = remember(left, right, ignoreCase, ignoreWhitespace) {
        TextDiff.diff(left, right, DiffOptions(ignoreCase, ignoreWhitespace))
    }
    val status = LocalStatusColors.current
    val palette = remember(status) { DiffPalette(status.good, status.bad, status.info) }
    DiffSummary(result, palette)
    if (result.identical) return

    var expanded by remember(result) { mutableStateOf(emptySet<Int>()) }
    val items = remember(result, collapse, expanded) { diffItems(result.rows, collapse, expanded) }
    val patch = remember(result) { TextDiff.patch(result) }
    ResultCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(Res.string.differences.str(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                Res.string.copy_as_patch.str(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CopyIconButton(patch)
        }
        SegmentedChoice(
            options = DiffLayout.entries,
            selected = layout,
            onSelect = { layout = it },
            label = {
                when (it) {
                    DiffLayout.SIDE_BY_SIDE -> Res.string.diff_side_by_side.str()
                    DiffLayout.UNIFIED -> Res.string.diff_unified.str()
                }
            },
        )
        val shown = items.take(MAX_SHOWN_ROWS)
        val onExpand = { fold: DiffItem.Fold -> expanded = expanded + fold.from }
        when (layout) {
            DiffLayout.SIDE_BY_SIDE -> SideBySideDiff(result, shown, palette, onExpand)
            DiffLayout.UNIFIED -> UnifiedDiff(result, shown, palette, onExpand)
        }
        if (items.size > MAX_SHOWN_ROWS) {
            Text(
                pluralStringResource(Res.plurals.diff_rows_shown, MAX_SHOWN_ROWS, MAX_SHOWN_ROWS),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiffSummary(result: DiffResult, palette: DiffPalette) {
    ResultCard {
        KeyValueRow(Res.string.similarity.str(), result.similarity.fmt(1) + " %", copyable = false)
        if (result.identical) {
            Text(Res.string.diff_texts_identical.str(), style = MaterialTheme.typography.bodyMedium)
            return@ResultCard
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LegendEntry(palette.addedWord, Res.string.added_lines.str(), result.added)
            LegendEntry(palette.removedWord, Res.string.removed_lines.str(), result.removed)
            LegendEntry(palette.changedWord, Res.string.changed_lines.str(), result.changed)
        }
    }
}

@Composable
private fun LegendEntry(color: Color, label: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(8.dp))
        Text("$label: $count", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SideBySideDiff(result: DiffResult, items: List<DiffItem>, palette: DiffPalette, onExpand: (DiffItem.Fold) -> Unit) {
    val digits = numberWidth(result)
    DiffFrame {
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(Res.string.original.str(), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Text(Res.string.modified.str(), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
        }
        items.forEach { item ->
            when (item) {
                is DiffItem.Fold -> FoldRow(item, onExpand)
                is DiffItem.Line -> {
                    val row = result.rows[item.index]
                    val (oldBackground, newBackground) = when (row.kind) {
                        DiffKind.EQUAL -> Color.Transparent to Color.Transparent
                        DiffKind.ADDED -> palette.addedGap to palette.added
                        DiffKind.REMOVED -> palette.removed to palette.removedGap
                        DiffKind.CHANGED -> palette.changed to palette.changed
                    }
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        DiffCell(row.oldNumber, row.oldText, row.oldSpans, oldBackground, palette.wordColor(row.kind, old = true), digits, Modifier.weight(1f))
                        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        DiffCell(row.newNumber, row.newText, row.newSpans, newBackground, palette.wordColor(row.kind, old = false), digits, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun UnifiedDiff(result: DiffResult, items: List<DiffItem>, palette: DiffPalette, onExpand: (DiffItem.Fold) -> Unit) {
    val digits = numberWidth(result)
    DiffFrame {
        items.forEach { item ->
            if (item is DiffItem.Fold) {
                FoldRow(item, onExpand)
                return@forEach
            }
            val row = result.rows[(item as DiffItem.Line).index]
            when (row.kind) {
                DiffKind.EQUAL -> UnifiedLine(row.oldNumber, row.newNumber, ' ', row.newText.orEmpty(), emptyList(), Color.Transparent, Color.Transparent, digits)
                DiffKind.REMOVED -> UnifiedLine(row.oldNumber, null, '-', row.oldText.orEmpty(), emptyList(), palette.removed, palette.removedWord, digits)
                DiffKind.ADDED -> UnifiedLine(null, row.newNumber, '+', row.newText.orEmpty(), emptyList(), palette.added, palette.addedWord, digits)
                DiffKind.CHANGED -> {
                    UnifiedLine(row.oldNumber, null, '-', row.oldText.orEmpty(), row.oldSpans, palette.changed, palette.changedWord, digits)
                    UnifiedLine(null, row.newNumber, '+', row.newText.orEmpty(), row.newSpans, palette.changed, palette.changedWord, digits)
                }
            }
        }
    }
}

@Composable
private fun DiffFrame(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .background(MaterialTheme.colorScheme.surface),
    ) { content() }
}

@Composable
private fun DiffCell(
    number: Int?,
    text: String?,
    spans: List<IntRange>,
    background: Color,
    wordColor: Color,
    digits: Int,
    modifier: Modifier,
) {
    Row(modifier.fillMaxHeight().background(background).padding(horizontal = 4.dp, vertical = 1.dp)) {
        LineNumber(number, digits)
        if (text != null) {
            Text(highlighted(text, spans, wordColor), style = codeStyle(), modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun UnifiedLine(
    oldNumber: Int?,
    newNumber: Int?,
    marker: Char,
    text: String,
    spans: List<IntRange>,
    background: Color,
    wordColor: Color,
    digits: Int,
) {
    Row(Modifier.fillMaxWidth().background(background).padding(horizontal = 4.dp, vertical = 1.dp)) {
        LineNumber(oldNumber, digits)
        LineNumber(newNumber, digits)
        Text(marker + " ", style = codeStyle(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(highlighted(text, spans, wordColor), style = codeStyle(), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun LineNumber(number: Int?, digits: Int) {
    Text(
        (number?.toString() ?: "").padStart(digits) + " ",
        style = MaterialTheme.typography.labelSmall.copy(fontFamily = monoFamily()),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 1.dp),
    )
}

@Composable
private fun FoldRow(fold: DiffItem.Fold, onExpand: (DiffItem.Fold) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable { onExpand(fold) }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.UnfoldMore, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        val count = fold.to - fold.from
        Text(
            pluralStringResource(Res.plurals.diff_unchanged_lines, count, count),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun codeStyle() = MaterialTheme.typography.bodySmall.copy(fontFamily = monoFamily())

private fun DiffPalette.wordColor(kind: DiffKind, old: Boolean): Color = when (kind) {
    DiffKind.CHANGED -> changedWord
    DiffKind.ADDED -> if (old) Color.Transparent else addedWord
    DiffKind.REMOVED -> if (old) removedWord else Color.Transparent
    DiffKind.EQUAL -> Color.Transparent
}

private fun highlighted(text: String, spans: List<IntRange>, color: Color): AnnotatedString {
    if (spans.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text)
        spans.forEach { addStyle(SpanStyle(background = color), it.first, it.last + 1) }
    }
}

private fun numberWidth(result: DiffResult): Int {
    val last = result.rows.maxOfOrNull { maxOf(it.oldNumber ?: 0, it.newNumber ?: 0) } ?: 0
    return last.toString().length
}

// a fold is keyed by its first hidden line, so an expanded fold survives recomputing the list
private fun diffItems(rows: List<DiffRow>, collapse: Boolean, expanded: Set<Int>): List<DiffItem> {
    if (!collapse) return rows.indices.map { DiffItem.Line(it) }
    val out = mutableListOf<DiffItem>()
    var k = 0
    while (k < rows.size) {
        if (rows[k].kind != DiffKind.EQUAL) {
            out += DiffItem.Line(k)
            k++
            continue
        }
        val start = k
        while (k < rows.size && rows[k].kind == DiffKind.EQUAL) k++
        val hideFrom = if (start == 0) 0 else start + CONTEXT_LINES
        val hideTo = if (k == rows.size) rows.size else k - CONTEXT_LINES
        if (hideTo - hideFrom >= 2 && hideFrom !in expanded) {
            for (r in start until hideFrom) out += DiffItem.Line(r)
            out += DiffItem.Fold(hideFrom, hideTo)
            for (r in hideTo until k) out += DiffItem.Line(r)
        } else {
            for (r in start until k) out += DiffItem.Line(r)
        }
    }
    return out
}
