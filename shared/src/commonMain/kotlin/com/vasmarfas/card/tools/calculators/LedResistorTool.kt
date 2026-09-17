package com.vasmarfas.card.tools.calculators

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.vasmarfas.card.core.fmt
import com.vasmarfas.card.core.str
import com.vasmarfas.card.core.toDoubleLenient
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ResultCard
import org.jetbrains.compose.resources.StringResource

private class LedPreset(val title: StringResource, val forward: Double)

private val ledPresets = listOf(
    LedPreset(Res.string.red, 1.8),
    LedPreset(Res.string.yellow, 2.0),
    LedPreset(Res.string.green, 2.2),
    LedPreset(Res.string.blue, 3.2),
    LedPreset(Res.string.white, 3.2),
    LedPreset(Res.string.ir, 1.4),
    LedPreset(Res.string.uv, 3.4),
)

private val currentPresets = listOf(5, 10, 15, 20, 30)

val ledResistorTool = Tool(
    id = "led-resistor",
    category = ToolCategory.CALCULATORS,
    title = Res.string.led_resistor,
    description = Res.string.series_resistor_for_one_or_more_leds_exact_v,
    icon = Icons.Filled.Lightbulb,
    keywords = listOf("led", "resistor", "e12", "e24", "forward voltage", "светодиод", "резистор", "ряд", "падение напряжения"),
) { LedResistorScreen() }

@Composable
private fun LedResistorScreen() {
    var supplyText by rememberSaveable { mutableStateOf("5") }
    var forwardText by rememberSaveable { mutableStateOf("2") }
    var currentText by rememberSaveable { mutableStateOf("20") }
    var countText by rememberSaveable { mutableStateOf("1") }
    val supply = supplyText.toDoubleLenient()?.takeIf { it > 0 }
    val forward = forwardText.toDoubleLenient()?.takeIf { it > 0 }
    val current = currentText.toDoubleLenient()?.takeIf { it > 0 }
    val count = countText.trim().toIntOrNull()?.takeIf { it >= 1 }
    NumberField(
        value = supplyText,
        onValueChange = { supplyText = it },
        label = Res.string.supply_voltage.str(),
        suffix = "V",
        isError = supplyText.isNotBlank() && supply == null,
    )
    NumberField(
        value = forwardText,
        onValueChange = { forwardText = it },
        label = Res.string.led_forward_voltage.str(),
        suffix = "V",
        isError = forwardText.isNotBlank() && forward == null,
    )
    ChoiceChips(
        options = ledPresets,
        selected = ledPresets.firstOrNull { it.forward == forward },
        onSelect = { forwardText = it.forward.fmt(1) },
        label = { "${it.title.str()} ${it.forward.fmt(1)} V" },
    )
    NumberField(
        value = currentText,
        onValueChange = { currentText = it },
        label = Res.string.led_current.str(),
        suffix = "mA",
        isError = currentText.isNotBlank() && current == null,
    )
    ChoiceChips(
        options = currentPresets,
        selected = current?.toInt()?.takeIf { it.toDouble() == current },
        onSelect = { currentText = it.toString() },
        label = { "$it mA" },
    )
    NumberField(
        value = countText,
        onValueChange = { countText = it },
        label = Res.string.leds_in_series.str(),
        isError = countText.isNotBlank() && count == null,
    )
    if (supply != null && forward != null && current != null && count != null) {
        val result = LedResistor.compute(supply, forward, current, count)
        if (result == null) {
            ErrorText(Res.string.supply_voltage_must_exceed_the_total_forward.str())
        } else {
            ResultCard {
                KeyValueRow(Res.string.exact_resistance.str(), Si.format(result.resistance, "Ω"))
                KeyValueRow(Res.string.nearest_e12_not_below.str(), "${Si.format(result.e12, "Ω")} → ${result.currentE12Ma.fmt(1)} mA")
                KeyValueRow(Res.string.nearest_e24_not_below.str(), "${Si.format(result.e24, "Ω")} → ${result.currentE24Ma.fmt(1)} mA")
                KeyValueRow(Res.string.resistor_power.str(), Si.format(result.power, "W"))
                KeyValueRow(Res.string.recommended_rating.str(), "${result.ratingW.fmt(3)} W")
                KeyValueRow(Res.string.voltage_across_resistor.str(), "${(supply - forward * count).fmt(2)} V")
            }
        }
    }
}
