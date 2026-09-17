package com.vasmarfas.card.tools.printing

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.tools.network.SimpleTable
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.ToolInputField

val gcodeCheatsheetTool = Tool(
    id = "gcode-cheatsheet",
    category = ToolCategory.PRINTING,
    title = Res.string.g_code_cheat_sheet,
    description = Res.string.marlin_and_klipper_commands_with_parameters,
    icon = Icons.Filled.Print,
    keywords = listOf("gcode", "g-code", "marlin", "klipper", "m104", "m600", "pressure advance", "гкод", "клиппер", "прошивка", "команды"),
) { GcodeCheatsheetScreen() }

@Composable
private fun GcodeCheatsheetScreen() {
    var query by rememberSaveable { mutableStateOf("") }
    ToolInputField(
        value = query,
        onValueChange = { query = it },
        label = Res.string.command_or_description.str(),
        placeholder = "M104 · retract · сетка",
    )
    val rows = GcodeReference.search(query)
    ResultCard(title = "${rows.size}") {
        SimpleTable(
            header = listOf(
                Res.string.command.str(),
                Res.string.parameters.str(),
                Res.string.firmware.str(),
                Res.string.description.str(),
            ),
            rows = rows.map { listOf(it.code, it.params, it.flavor, it.description.str()) },
            weights = listOf(1.6f, 1.2f, 1f, 3.4f),
            mono = false,
        )
    }
}
