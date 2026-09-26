package com.vasmarfas.card.tools.converters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.fmt
import com.vasmarfas.card.core.str
import com.vasmarfas.card.core.toDoubleLenient
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.tools.network.SimpleTable
import com.vasmarfas.card.ui.components.AnswerCard
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.DropdownChoice
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SegmentedChoice
import com.vasmarfas.card.ui.components.SwitchRow
import com.vasmarfas.card.ui.components.ToolSection

private enum class CookingMode { INGREDIENTS, OVEN }

private enum class TempScale { C, F }

val cookingConverterTool = Tool(
    id = "cooking-converter",
    category = ToolCategory.CONVERTERS,
    title = Res.string.cooking_converter,
    description = Res.string.cooking_converter_description,
    icon = Icons.Filled.Restaurant,
    keywords = listOf("cup", "tablespoon", "teaspoon", "flour", "sugar", "oven", "gas mark", "чашка", "ложка", "мука", "сахар", "духовка", "граммы", "рецепт"),
) { CookingConverterScreen() }

@Composable
private fun CookingConverterScreen() {
    var mode by rememberSaveable { mutableStateOf(CookingMode.INGREDIENTS) }
    SegmentedChoice(
        options = CookingMode.entries,
        selected = mode,
        onSelect = { mode = it },
        label = { if (it == CookingMode.INGREDIENTS) Res.string.ingredients.str() else Res.string.oven.str() },
    )
    val saved = rememberSaveableStateHolder()
    saved.SaveableStateProvider(mode) {
        when (mode) {
            CookingMode.INGREDIENTS -> IngredientsSection()
            CookingMode.OVEN -> OvenSection()
        }
    }
}

@Composable
private fun IngredientsSection() {
    var input by rememberSaveable { mutableStateOf("1") }
    var ingredient by rememberSaveable { mutableStateOf(Ingredient.FLOUR) }
    var from by rememberSaveable { mutableStateOf(CookingUnit.CUP_US) }
    var to by rememberSaveable { mutableStateOf(CookingUnit.GRAM) }
    val value = input.toDoubleLenient()
    ChoiceChips(
        options = Ingredient.entries,
        selected = ingredient,
        onSelect = { ingredient = it },
        label = { it.title.str() },
    )
    NumberField(
        value = input,
        onValueChange = { input = it },
        label = Res.string.amount.str(),
        suffix = from.symbol.str(),
        isError = input.isNotBlank() && value == null,
    )
    DropdownChoice(
        options = CookingUnit.entries,
        selected = from,
        onSelect = { from = it },
        label = Res.string.from_.str(),
        text = { it.title.str() },
    )
    DropdownChoice(
        options = CookingUnit.entries,
        selected = to,
        onSelect = { to = it },
        label = Res.string.to.str(),
        text = { it.title.str() },
    )
    if (value != null) {
        AnswerCard(
            "${Cooking.convert(value, from, to, ingredient).fmt(2)} ${to.symbol.str()}",
            "${value.fmt(2)} ${from.symbol.str()} · ${ingredient.title.str()} · ${ingredient.density.fmt(2)} ${Res.string.unit_g_ml.str()}",
        )
        ResultCard(Res.string.all_units.str()) {
            CookingUnit.entries.forEach { unit ->
                KeyValueRow(unit.title.str(), "${Cooking.convert(value, from, unit, ingredient).fmt(2)} ${unit.symbol.str()}")
            }
        }
    }
}

@Composable
private fun OvenSection() {
    var input by rememberSaveable { mutableStateOf("180") }
    var scale by rememberSaveable { mutableStateOf(TempScale.C) }
    var fan by rememberSaveable { mutableStateOf(false) }
    val value = input.toDoubleLenient()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NumberField(
            value = input,
            onValueChange = { input = it },
            label = Res.string.temperature.str(),
            modifier = Modifier.weight(1f),
            isError = input.isNotBlank() && value == null,
        )
        SegmentedChoice(
            options = TempScale.entries,
            selected = scale,
            onSelect = { scale = it },
            label = { if (it == TempScale.C) "°C" else "°F" },
            modifier = Modifier.weight(1f),
        )
    }
    SwitchRow(Res.string.oven_recipe_for_fan.str(), fan, { fan = it }, description = Res.string.oven_fan_hint.str())
    if (value != null) {
        val celsius = if (scale == TempScale.C) value else Cooking.fahrenheitToCelsius(value)
        val conventional = if (fan) celsius + Cooking.FAN_OFFSET else celsius
        val withFan = conventional - Cooking.FAN_OFFSET
        val setting = Cooking.gasMark(conventional)
        ResultCard {
            KeyValueRow(Res.string.oven_conventional.str(), "${conventional.fmt(0)} °C · ${Cooking.celsiusToFahrenheit(conventional).fmt(0)} °F")
            KeyValueRow(Res.string.oven_fan.str(), "${withFan.fmt(0)} °C · ${Cooking.celsiusToFahrenheit(withFan).fmt(0)} °F")
            KeyValueRow(Res.string.gas_mark.str(), setting.mark)
            KeyValueRow(Res.string.oven_heat.str(), setting.description.str(), mono = false, copyable = false)
        }
    }
    ToolSection(Res.string.gas_marks.str()) {
        SimpleTable(
            header = listOf(Res.string.gas_mark_short.str(), "°C", "°F", Res.string.oven_fan.str(), Res.string.oven_heat.str()),
            rows = Cooking.oven.map { listOf(it.mark, "${it.celsius}", "${it.fahrenheit}", "${it.celsius - Cooking.FAN_OFFSET}", it.description.str()) },
            weights = listOf(0.7f, 0.7f, 0.7f, 1f, 1.6f),
            mono = false,
        )
        Text(Res.string.gas_marks_note.str(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
