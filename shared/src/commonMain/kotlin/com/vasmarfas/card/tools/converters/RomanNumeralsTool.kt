package com.vasmarfas.card.tools.converters

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.TagChips
import com.vasmarfas.card.ui.components.ToolInputField

val romanNumeralsTool = Tool(
    id = "roman-numerals",
    category = ToolCategory.CONVERTERS,
    title = Res.string.roman_numerals,
    description = Res.string.arabic_to_roman_and_back_1_to_3999_with_stri,
    icon = Icons.Filled.FormatListNumbered,
    keywords = listOf("roman", "numerals", "latin", "римские", "цифры", "mcmxc"),
) { RomanNumeralsScreen() }

@Composable
private fun RomanNumeralsScreen() {
    var input by rememberSaveable { mutableStateOf("2026") }
    val trimmed = input.trim()
    val arabic = remember(trimmed) { trimmed.toIntOrNull() }
    val roman = remember(trimmed) { if (trimmed.toIntOrNull() == null) Roman.toArabic(trimmed) else null }
    val converted = when {
        arabic != null -> Roman.toRoman(arabic)?.let { arabic to it }
        roman != null -> roman to trimmed.uppercase()
        else -> null
    }
    ToolInputField(
        value = input,
        onValueChange = { input = it },
        label = Res.string.number_or_roman_numeral.str(),
        placeholder = "1994 · MCMXCIV",
        keyboardType = KeyboardType.Ascii,
        isError = trimmed.isNotEmpty() && converted == null,
        monospace = true,
    )
    if (trimmed.isNotEmpty() && converted == null) {
        ErrorText(
            if (arabic != null) Res.string.only_1_to_3999_can_be_written_in_roman_numer.str()
            else Res.string.not_a_valid_roman_numeral.str(),
        )
    }
    if (converted != null) {
        ResultCard {
            KeyValueRow(Res.string.roman.str(), converted.second)
            KeyValueRow(Res.string.arabic.str(), converted.first.toString())
        }
    }
    TagChips(Roman.symbols.filter { it.second.length == 1 }.map { "${it.second} = ${it.first}" })
}
