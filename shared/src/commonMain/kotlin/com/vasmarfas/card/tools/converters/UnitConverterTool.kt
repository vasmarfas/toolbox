package com.vasmarfas.card.tools.converters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.str
import com.vasmarfas.card.core.toDoubleLenient
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.AnswerCard
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.DropdownChoice
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ResultCard

val unitConverterTool = Tool(
    id = "unit-converter",
    category = ToolCategory.CONVERTERS,
    title = Res.string.unit_converter,
    description = Res.string.unit_converter_description,
    icon = Icons.Filled.Straighten,
    keywords = listOf(
        "units", "metric", "imperial", "inch", "mile", "pound", "celsius", "fahrenheit", "psi", "bar",
        "единицы", "дюйм", "миля", "фунт", "цельсий", "фаренгейт", "давление", "объём", "скорость",
    ),
) { UnitConverterScreen() }

@Composable
private fun UnitConverterScreen() {
    var category by rememberSaveable { mutableStateOf(UnitCategory.LENGTH) }
    var fromId by rememberSaveable { mutableStateOf(UnitCategory.LENGTH.units[0].id) }
    var toId by rememberSaveable { mutableStateOf(UnitCategory.LENGTH.units[1].id) }
    var input by rememberSaveable { mutableStateOf("1") }
    val from = category.units.firstOrNull { it.id == fromId } ?: category.units[0]
    val to = category.units.firstOrNull { it.id == toId } ?: category.units[1]
    val value = input.toDoubleLenient()
    ChoiceChips(
        options = UnitCategory.entries,
        selected = category,
        onSelect = {
            category = it
            fromId = it.units[0].id
            toId = it.units[1].id
        },
        label = { it.title.str() },
    )
    NumberField(
        value = input,
        onValueChange = { input = it },
        label = Res.string.value_.str(),
        suffix = from.symbol.str(),
        isError = input.isNotBlank() && value == null,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DropdownChoice(
            options = category.units,
            selected = from,
            onSelect = { fromId = it.id },
            label = Res.string.from_.str(),
            text = { it.name.str() },
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = {
                val previous = from.id
                fromId = to.id
                toId = previous
            },
        ) {
            Icon(Icons.Filled.SwapHoriz, contentDescription = Res.string.swap.str())
        }
        DropdownChoice(
            options = category.units,
            selected = to,
            onSelect = { toId = it.id },
            label = Res.string.to.str(),
            text = { it.name.str() },
            modifier = Modifier.weight(1f),
        )
    }
    if (value != null) {
        val converted = Units.convert(value, from, to)
        AnswerCard(converted.fmtReadable().withUnit(to.symbol.str()), "${from.name.str()} → ${to.name.str()}", copyValue = converted.fmtSig())
        ResultCard(Res.string.all_units.str()) {
            category.units.forEach { unit ->
                val result = Units.convert(value, from, unit)
                KeyValueRow(unit.name.str(), result.fmtReadable().withUnit(unit.symbol.str()), copyValue = result.fmtSig())
            }
        }
    }
}

private fun AnnotatedString.withUnit(symbol: String) = this + AnnotatedString(" $symbol")
