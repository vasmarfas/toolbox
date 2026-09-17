package com.vasmarfas.card.tools.developer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.PlatformKind
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.MonoTable
import com.vasmarfas.card.ui.components.ResultCard

private const val MAX_EVENTS = 20

val keyboardTesterTool = Tool(
    id = "keyboard-tester",
    category = ToolCategory.DEVELOPER,
    title = Res.string.keyboard_tester,
    description = Res.string.click_the_field_and_press_keys_the_last_even,
    icon = Icons.Filled.Keyboard,
    keywords = listOf("keyboard", "key", "keycode", "shortcut", "modifiers", "input", "клавиатура", "клавиша", "код клавиши", "сочетание"),
    platforms = setOf(PlatformKind.DESKTOP, PlatformKind.WEB, PlatformKind.ANDROID),
) { KeyboardTesterScreen() }

@Composable
private fun KeyboardTesterScreen() {
    val events = remember { mutableStateListOf<String>() }
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(16.dp))
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp),
            )
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable { focusRequester.requestFocus() }
            .onKeyEvent { e ->
                val modifiers = buildList {
                    if (e.isCtrlPressed) add("Ctrl")
                    if (e.isShiftPressed) add("Shift")
                    if (e.isAltPressed) add("Alt")
                    if (e.isMetaPressed) add("Meta")
                }.joinToString("+").ifEmpty { "—" }
                val type = if (e.type == KeyEventType.KeyDown) "down" else if (e.type == KeyEventType.KeyUp) "up" else "unknown"
                val name = e.key.toString().removePrefix("Key: ")
                val cp = e.utf16CodePoint
                val char = if (cp in 0x20..0x10FFFF && cp != 0x7F) cp.toChar().toString() else "—"
                events.add(0, "${type.padEnd(8)}${name.take(24).padEnd(26)}${char.padEnd(6)}${cp.toString().padEnd(8)}$modifiers")
                while (events.size > MAX_EVENTS) events.removeAt(events.lastIndex)
                true
            }
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (focused) Res.string.press_any_key.str() else Res.string.click_here_to_capture_keys.str(),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }
    ActionButton(Res.string.clear.str(), onClick = { events.clear() }, enabled = events.isNotEmpty())
    if (events.isNotEmpty()) {
        ResultCard(Res.string.last_events.str()) {
            MonoTable(listOf("Type    Key                       Char  Code    Modifiers") + events.toList())
        }
    }
}
