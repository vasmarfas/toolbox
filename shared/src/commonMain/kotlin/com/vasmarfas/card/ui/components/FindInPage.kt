package com.vasmarfas.card.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import org.jetbrains.compose.resources.stringResource

class FindState {
    var open by mutableStateOf(false)
        private set
    var query by mutableStateOf("")
    var current by mutableStateOf(0)
    internal var focusRequests by mutableStateOf(0)
    internal val entries = mutableStateListOf<FindEntry>()
    private var order = 0

    val total: Int get() = entries.sumOf { it.count }

    fun show(): Boolean {
        if (entries.isEmpty()) return false
        open = true
        focusRequests++
        return true
    }

    fun shortcut(event: KeyEvent): Boolean =
        event.type == KeyEventType.KeyDown && event.key == Key.F && (event.isCtrlPressed || event.isMetaPressed) && show()

    fun close() {
        open = false
        query = ""
        current = 0
    }

    fun step(forward: Boolean) {
        val total = total
        if (total > 0) current = (current + if (forward) 1 else total - 1) % total
    }

    internal fun register(): FindEntry = FindEntry(order++)

    internal fun firstIndex(entry: FindEntry): Int = entries.filter { it.order < entry.order }.sumOf { it.count }
}

internal class FindEntry(val order: Int) {
    var count by mutableStateOf(0)
}

val LocalFind = staticCompositionLocalOf { FindState() }

private fun Char.folded() = if (this == 'ё' || this == 'Ё') 'е' else this

internal fun occurrences(text: String, query: String): List<Int> {
    if (query.isEmpty()) return emptyList()
    val haystack = text.map { it.folded() }.joinToString("")
    val needle = query.map { it.folded() }.joinToString("")
    val found = mutableListOf<Int>()
    var at = haystack.indexOf(needle, ignoreCase = true)
    while (at >= 0) {
        found += at
        at = haystack.indexOf(needle, at + needle.length, ignoreCase = true)
    }
    return found
}

@Composable
fun FindableText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign? = null,
) {
    val find = LocalFind.current
    val entry = remember(find) { find.register() }
    DisposableEffect(find, entry) {
        find.entries += entry
        onDispose { find.entries -= entry }
    }
    val query = if (find.open) find.query.trim() else ""
    val hits = remember(text, query) { occurrences(text, query) }
    SideEffect { entry.count = hits.size }
    if (hits.isEmpty()) {
        Text(text, modifier, color, style = style, maxLines = maxLines, overflow = overflow, textAlign = textAlign)
        return
    }
    val first = find.firstIndex(entry)
    val focused = (find.current - first).takeIf { it in hits.indices }
    val highlight = MaterialTheme.colorScheme.secondaryContainer
    val onHighlight = MaterialTheme.colorScheme.onSecondaryContainer
    val accent = MaterialTheme.colorScheme.tertiary
    val onAccent = MaterialTheme.colorScheme.onTertiary
    val marked = remember(text, hits, query, focused, highlight, accent) {
        buildAnnotatedString {
            append(text)
            hits.forEachIndexed { i, start ->
                val span = if (i == focused) SpanStyle(color = onAccent, background = accent) else SpanStyle(color = onHighlight, background = highlight)
                addStyle(span, start, start + query.length)
            }
        }
    }
    val requester = remember { BringIntoViewRequester() }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    LaunchedEffect(focused, hits, layout) {
        val index = focused ?: return@LaunchedEffect
        val start = hits[index]
        val box = layout?.getPathForRange(start, start + query.length)?.getBounds() ?: return@LaunchedEffect
        requester.bringIntoView(box)
    }
    Text(
        marked,
        modifier.bringIntoViewRequester(requester),
        color,
        style = style,
        maxLines = maxLines,
        overflow = overflow,
        textAlign = textAlign,
        onTextLayout = { layout = it },
    )
}

@Composable
fun FindBar(find: FindState, modifier: Modifier = Modifier) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(find.focusRequests) { focus.requestFocus() }
    val total = find.total
    LaunchedEffect(total) { if (find.current >= total) find.current = 0 }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 6.dp,
    ) {
        Row(Modifier.padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            BasicTextField(
                value = find.query,
                onValueChange = {
                    find.query = it
                    find.current = 0
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .width(180.dp)
                    .focusRequester(focus)
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.Enter, Key.NumPadEnter -> find.step(forward = !event.isShiftPressed)
                            Key.Escape -> find.close()
                            else -> return@onPreviewKeyEvent false
                        }
                        true
                    },
                decorationBox = { field ->
                    if (find.query.isEmpty()) {
                        Text(Res.string.find_on_page.str(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    field()
                },
            )
            if (find.query.isNotBlank()) {
                Text(
                    if (total == 0) Res.string.find_nothing.str() else stringResource(Res.string.find_count, find.current + 1, total),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { find.step(forward = false) }, enabled = total > 0) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = Res.string.find_previous.str())
            }
            IconButton(onClick = { find.step(forward = true) }, enabled = total > 0) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = Res.string.find_next.str())
            }
            IconButton(onClick = { find.close() }) {
                Icon(Icons.Filled.Close, contentDescription = Res.string.close_find.str())
            }
        }
    }
}
