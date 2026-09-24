package com.vasmarfas.card.tools.text

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Abc
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.ToolInputField

val caseConverterTool = Tool(
    id = "case-converter",
    category = ToolCategory.TEXT,
    title = Res.string.case_converter,
    description = Res.string.case_converter_description,
    icon = Icons.Filled.Abc,
    keywords = listOf("case", "uppercase", "lowercase", "camel", "pascal", "snake", "kebab", "регистр", "заглавные", "строчные", "верхний", "нижний"),
) { CaseConverterScreen() }

@Composable
private fun CaseConverterScreen() {
    var input by rememberSaveable { mutableStateOf("") }
    ToolInputField(
        value = input,
        onValueChange = { input = it },
        label = Res.string.text.str(),
        singleLine = false,
        minLines = 3,
    )
    if (input.isNotEmpty()) {
        ResultCard {
            TextCase.entries.forEach { case ->
                KeyValueRow(case.title.str(), CaseConvert.convert(input, case))
            }
        }
    }
}
