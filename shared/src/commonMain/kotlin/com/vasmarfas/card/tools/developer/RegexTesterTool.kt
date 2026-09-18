package com.vasmarfas.card.tools.developer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import com.vasmarfas.card.core.Tr
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.tools.text.OutputCard
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.MonoTable
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SwitchRow
import com.vasmarfas.card.ui.components.ToolInputField
import com.vasmarfas.card.ui.theme.LocalStatusColors

val regexTesterTool = Tool(
    id = "regex-tester",
    category = ToolCategory.DEVELOPER,
    title = Res.string.regex_tester,
    description = Res.string.matches_with_positions_and_groups_highlighte,
    icon = Icons.Filled.FindInPage,
    keywords = listOf("regex", "regexp", "regular expression", "pattern", "match", "groups", "регулярное выражение", "регулярка", "шаблон"),
) { RegexTesterScreen() }

@Composable
private fun RegexTesterScreen() {
    var pattern by rememberSaveable { mutableStateOf("") }
    var text by rememberSaveable { mutableStateOf("") }
    var replacement by rememberSaveable { mutableStateOf("") }
    var ignoreCase by rememberSaveable { mutableStateOf(false) }
    var multiline by rememberSaveable { mutableStateOf(false) }
    var dotAll by rememberSaveable { mutableStateOf(false) }
    var showCheatSheet by rememberSaveable { mutableStateOf(false) }
    val result = remember(pattern, text, replacement, ignoreCase, multiline, dotAll) {
        RegexTester.run(pattern, text, ignoreCase, multiline, dotAll, replacement.ifEmpty { null })
    }
    ToolInputField(
        value = pattern,
        onValueChange = { pattern = it },
        label = Res.string.pattern.str(),
        placeholder = "(\\w+)@(\\w+)\\.com",
        isError = result.error != null,
        supportingText = result.error,
        monospace = true,
    )
    SwitchRow(Res.string.ignore_case_i.str(), ignoreCase, { ignoreCase = it })
    SwitchRow(Tr("Multiline: ^ and $ per line (m)", "Многострочный: ^ и $ на каждой строке (m)").str(), multiline, { multiline = it })
    SwitchRow(Res.string.dot_matches_newline_s.str(), dotAll, { dotAll = it })
    ToolInputField(
        value = text,
        onValueChange = { text = it },
        label = Res.string.test_text.str(),
        singleLine = false,
        minLines = 4,
    )
    ToolInputField(
        value = replacement,
        onValueChange = { replacement = it },
        label = Res.string.replacement_optional_1_for_groups.str(),
        monospace = true,
    )
    SwitchRow(Res.string.show_cheat_sheet.str(), showCheatSheet, { showCheatSheet = it })
    if (showCheatSheet) {
        ResultCard(Res.string.cheat_sheet.str()) {
            RegexTester.cheatSheet.forEach { (token, description) -> KeyValueRow(token, description.str(), mono = false, copyable = false) }
        }
    }
    if (pattern.isEmpty() || result.error != null) return
    val matches = result.matches
    KeyValueRow(Res.string.matches.str(), if (matches.size >= RegexTester.MAX_MATCHES) "${matches.size}+" else matches.size.toString(), copyable = false)
    if (matches.isEmpty()) return
    val status = LocalStatusColors.current
    val colors = listOf(status.series[2].copy(alpha = 0.3f), status.warn.copy(alpha = 0.35f))
    val highlighted = remember(text, matches) {
        buildAnnotatedString {
            var pos = 0
            matches.forEachIndexed { i, m ->
                if (m.start > pos) append(text.substring(pos, m.start))
                if (m.end > m.start) {
                    withStyle(SpanStyle(background = colors[i % 2])) { append(text.substring(m.start, m.end)) }
                }
                pos = maxOf(pos, m.end)
            }
            if (pos < text.length) append(text.substring(pos))
        }
    }
    ResultCard(Res.string.highlighted.str()) {
        Text(highlighted, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
    }
    ResultCard(Res.string.match_list.str()) {
        MonoTable(
            listOf("#    Range        Match") +
                matches.take(200).map { m ->
                    val groups = if (m.groups.isEmpty()) "" else "    groups: " + m.groups.mapIndexed { gi, g -> "\$${gi + 1}=${g ?: "null"}" }.joinToString("  ")
                    "${(m.index + 1).toString().padEnd(5)}${"[${m.start}, ${m.end})".padEnd(13)}${m.value}$groups"
                },
        )
    }
    if (result.replaced != null) OutputCard(result.replaced, title = Res.string.replacement_preview.str())
}
