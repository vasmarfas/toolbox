package com.vasmarfas.card.tools.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.CopyIconButton
import com.vasmarfas.card.ui.components.DropdownChoice
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SelectableText
import com.vasmarfas.card.ui.components.SwitchRow
import com.vasmarfas.card.ui.components.ToolInputField
import com.vasmarfas.card.ui.components.ToolSection
import org.jetbrains.compose.resources.stringResource

val mojibakeFixerTool = Tool(
    id = "mojibake-fixer",
    category = ToolCategory.TEXT,
    title = Res.string.mojibake_fixer,
    description = Res.string.mojibake_fixer_description,
    icon = Icons.Filled.AutoFixHigh,
    keywords = listOf(
        "mojibake", "encoding", "charset", "code page", "garbled", "utf-8", "windows-1251", "cp1251", "koi8-r", "cp866", "latin-1",
        "кракозябры", "крякозябры", "кодировка", "абракадабра", "перекодировать", "иероглифы", "непонятные символы",
    ),
) { MojibakeFixerScreen() }

private val samples = listOf(
    "РџСЂРёРІРµС‚! РљР°Рє РґРµР»Р°?",
    "Ïðèâåò! Êàê äåëà?",
    "рТЙЧЕФ! лБЛ ДЕМБ?",
    "╨Я╤А╨╕╨▓╨╡╤В! ╨Ъ╨░╨║ ╨┤╨╡╨╗╨░?",
)

private val shownPages = listOf(Codepage.CP1251, Codepage.CP1252, Codepage.KOI8R, Codepage.CP866)

@Composable
private fun MojibakeFixerScreen() {
    var text by rememberSaveable { mutableStateOf("") }
    var picked by rememberSaveable(text) { mutableStateOf(0) }
    var manual by rememberSaveable { mutableStateOf(false) }
    var shownAs by rememberSaveable { mutableStateOf(Codepage.CP1251) }
    var actual by rememberSaveable { mutableStateOf(Codepage.UTF8) }
    ToolInputField(
        value = text,
        onValueChange = { text = it },
        label = Res.string.garbled_text.str(),
        singleLine = false,
        minLines = 4,
        maxLines = 12,
        placeholder = samples.first(),
    )
    if (text.isBlank()) {
        ToolSection(Res.string.examples.str()) {
            ChoiceChips(options = samples, selected = null, onSelect = { text = it }, label = { it.substringBefore('!') })
        }
        return
    }
    val repairs = remember(text) { Mojibake.repairs(text) }
    if (Mojibake.hasLostCharacters(text)) ErrorText(Res.string.mojibake_lost_characters.str())
    repairs.getOrNull(picked)?.let { fix ->
        ResultCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(Res.string.fixed_text.str(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                CopyIconButton(fix.text)
            }
            SelectableText(fix.text, style = MaterialTheme.typography.bodyLarge)
            Text(explanation(fix.steps), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (repairs.size > 1) {
        ToolSection(Res.string.other_readings.str()) {
            repairs.forEachIndexed { index, fix ->
                if (index == picked) return@forEachIndexed
                OutlinedCard(onClick = { picked = index }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(fix.text.take(200).replace('\n', ' '), style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(explanation(fix.steps), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
    SwitchRow(Res.string.choose_code_pages_manually.str(), manual, { manual = it })
    if (!manual) return
    DropdownChoice(
        options = shownPages,
        selected = shownAs,
        onSelect = {
            shownAs = it
            if (actual == it) actual = Codepage.UTF8
        },
        label = Res.string.text_is_shown_as.str(),
        text = { it.label },
    )
    DropdownChoice(
        options = Codepage.entries.filter { it != shownAs },
        selected = actual,
        onSelect = { actual = it },
        label = Res.string.text_is_actually_in.str(),
        text = { it.label },
    )
    val result = remember(text, shownAs, actual) { Mojibake.apply(text, listOf(MojibakeStep(shownAs, actual))) }
    if (result == null) {
        ErrorText(stringResource(Res.string.text_does_not_fit_code_page, shownAs.label))
    } else {
        OutputCard(result)
    }
}

@Composable
private fun explanation(steps: List<MojibakeStep>): String {
    if (steps.isEmpty()) return Res.string.mojibake_already_readable.str()
    val then = Res.string.then.str()
    return steps.reversed()
        .map { stringResource(Res.string.mojibake_step, it.actual.label, it.shownAs.label) }
        .joinToString(", $then ")
        .replaceFirstChar { it.uppercaseChar() }
}
