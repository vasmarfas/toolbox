package com.vasmarfas.card.tools.developer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SwitchRow
import com.vasmarfas.card.ui.components.ToolInputField

val chmodCalculatorTool = Tool(
    id = "chmod-calculator",
    category = ToolCategory.DEVELOPER,
    title = Res.string.chmod_calculator,
    description = Res.string.chmod_calculator_description,
    icon = Icons.Filled.Terminal,
    keywords = listOf("chmod", "permissions", "unix", "linux", "octal", "rwx", "setuid", "sticky", "права доступа", "файл"),
) { ChmodCalculatorScreen() }

@Composable
private fun ChmodCalculatorScreen() {
    var mode by rememberSaveable { mutableStateOf(0x1ED) }
    var octalText by rememberSaveable { mutableStateOf(Chmod.octal(0x1ED)) }
    var symbolicText by rememberSaveable { mutableStateOf(Chmod.symbolic(0x1ED)) }
    var fileName by rememberSaveable { mutableStateOf("file") }
    val update: (Int) -> Unit = { m ->
        mode = m
        octalText = Chmod.octal(m)
        symbolicText = Chmod.symbolic(m)
    }
    val octalValid = Chmod.parseOctal(octalText) != null
    val symbolicValid = Chmod.parseSymbolic(symbolicText) != null
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ToolInputField(
            value = octalText,
            onValueChange = { text ->
                octalText = text
                Chmod.parseOctal(text)?.let { m ->
                    mode = m
                    symbolicText = Chmod.symbolic(m)
                }
            },
            label = Res.string.octal.str(),
            modifier = Modifier.weight(1f),
            isError = !octalValid,
            monospace = true,
        )
        ToolInputField(
            value = symbolicText,
            onValueChange = { text ->
                symbolicText = text
                Chmod.parseSymbolic(text)?.let { m ->
                    mode = m
                    octalText = Chmod.octal(m)
                }
            },
            label = Res.string.symbolic.str(),
            modifier = Modifier.weight(1f),
            isError = !symbolicValid,
            monospace = true,
        )
    }
    val labels = listOf(Res.string.owner, Res.string.group, Res.string.others)
    val permissions = listOf(Res.string.read, Res.string.write, Res.string.execute)
    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.weight(1.3f))
        permissions.forEach { permission ->
            Text(permission.str(), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        }
    }
    labels.forEachIndexed { row, label ->
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label.str(), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1.3f))
            permissions.indices.forEach { col ->
                val bit = Chmod.bits[row * 3 + col]
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Checkbox(checked = mode and bit != 0, onCheckedChange = { update(mode xor bit) })
                }
            }
        }
    }
    SwitchRow("setuid (4000)", mode and Chmod.SUID != 0, { update(mode xor Chmod.SUID) })
    SwitchRow("setgid (2000)", mode and Chmod.SGID != 0, { update(mode xor Chmod.SGID) })
    SwitchRow("sticky (1000)", mode and Chmod.STICKY != 0, { update(mode xor Chmod.STICKY) })
    ToolInputField(value = fileName, onValueChange = { fileName = it }, label = Res.string.file_or_directory.str(), monospace = true)
    ResultCard {
        KeyValueRow(Res.string.octal.str(), Chmod.octal(mode))
        KeyValueRow(Res.string.symbolic.str(), Chmod.symbolic(mode))
        KeyValueRow(Res.string.command.str(), "chmod ${Chmod.octal(mode)} $fileName")
        KeyValueRow(Res.string.command_symbolic.str(), "chmod ${Chmod.symbolicCommand(mode)} $fileName")
    }
}
