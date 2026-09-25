package com.vasmarfas.card.tools.electronics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.fmt
import com.vasmarfas.card.core.str
import com.vasmarfas.card.core.toDoubleLenient
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.Hint
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.Stepper
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private class LedPreset(val title: StringResource, val forward: Double, val argb: Long)

private val ledPresets = listOf(
    LedPreset(Res.string.red, 1.8, 0xFFE53935),
    LedPreset(Res.string.yellow, 2.0, 0xFFFDD835),
    LedPreset(Res.string.green, 2.2, 0xFF43A047),
    LedPreset(Res.string.blue, 3.2, 0xFF1E88E5),
    LedPreset(Res.string.white, 3.2, 0xFFFAFAFA),
    LedPreset(Res.string.ir, 1.4, 0xFF6D4C41),
    LedPreset(Res.string.uv, 3.4, 0xFF8E24AA),
)

private class SupplyPreset(val title: StringResource?, val volts: Double)

// a car runs at 14.4 V while the engine charges the battery, a LED sized for 12 V would be overdriven
private val supplyPresets = listOf(
    SupplyPreset(Res.string.led_supply_usb, 5.0),
    SupplyPreset(Res.string.led_supply_board, 3.3),
    SupplyPreset(Res.string.led_supply_coin, 3.0),
    SupplyPreset(Res.string.led_supply_9v, 9.0),
    SupplyPreset(Res.string.led_supply_12v, 12.0),
    SupplyPreset(Res.string.led_supply_car, 14.4),
    SupplyPreset(null, 24.0),
)

private val currentPresets = listOf(5, 10, 15, 20, 30)

// below this share of the supply left for the resistor the current follows every wobble of the supply
private const val LOW_HEADROOM = 0.15

private const val MAX_LEDS = 50

val ledResistorTool = Tool(
    id = "led-resistor",
    category = ToolCategory.ELECTRONICS,
    title = Res.string.led_resistor,
    description = Res.string.led_resistor_description,
    icon = Icons.Filled.Lightbulb,
    keywords = listOf("led", "resistor", "e12", "e24", "forward voltage", "светодиод", "резистор", "ряд", "падение напряжения", "лента", "подключить"),
) { LedResistorScreen() }

@Composable
private fun LedResistorScreen() {
    var supplyText by rememberSaveable { mutableStateOf("5") }
    var forwardText by rememberSaveable { mutableStateOf("2") }
    var currentText by rememberSaveable { mutableStateOf("20") }
    var count by rememberSaveable { mutableStateOf(1) }
    val supply = supplyText.toDoubleLenient()?.takeIf { it > 0 }
    val forward = forwardText.toDoubleLenient()?.takeIf { it > 0 }
    val current = currentText.toDoubleLenient()?.takeIf { it > 0 }
    val volt = Res.string.unit_v.str()
    val ohm = Res.string.unit_ohm.str()
    val preset = ledPresets.firstOrNull { it.forward == forward }
    val result = if (supply != null && forward != null && current != null) LedResistor.compute(supply, forward, current, count) else null
    LedCircuit(
        supply = supply?.let { "${it.fmt(1)} $volt" } ?: "? $volt",
        resistor = result?.let { Si.format(it.e12, ohm) } ?: "R",
        count = count,
        led = preset?.let { Color(it.argb) } ?: MaterialTheme.colorScheme.primary,
    )
    Hint(Res.string.led_scheme_hint.str())
    NumberField(
        value = supplyText,
        onValueChange = { supplyText = it },
        label = Res.string.supply_voltage.str(),
        suffix = volt,
        isError = supplyText.isNotBlank() && supply == null,
    )
    ChoiceChips(
        options = supplyPresets,
        selected = supplyPresets.firstOrNull { it.volts == supply },
        onSelect = { supplyText = it.volts.fmt(1) },
        label = { preset -> (preset.title?.let { it.str() + " " } ?: "") + "${preset.volts.fmt(1)} $volt" },
    )
    NumberField(
        value = forwardText,
        onValueChange = { forwardText = it },
        label = Res.string.led_forward_voltage.str(),
        suffix = volt,
        isError = forwardText.isNotBlank() && forward == null,
    )
    ChoiceChips(
        options = ledPresets,
        selected = preset,
        onSelect = { forwardText = it.forward.fmt(1) },
        label = { "${it.title.str()} ${it.forward.fmt(1)} $volt" },
    )
    Hint(Res.string.led_forward_hint.str())
    NumberField(
        value = currentText,
        onValueChange = { currentText = it },
        label = Res.string.led_current.str(),
        suffix = Res.string.unit_ma.str(),
        isError = currentText.isNotBlank() && current == null,
    )
    ChoiceChips(
        options = currentPresets,
        selected = current?.toInt()?.takeIf { it.toDouble() == current },
        onSelect = { currentText = it.toString() },
        label = { "$it ${Res.string.unit_ma.str()}" },
    )
    Hint(Res.string.led_current_hint.str())
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(Res.string.leds_in_series.str(), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Stepper(Res.string.pieces_short.str(), count, 1..MAX_LEDS) { count = it }
    }
    if (supply == null || forward == null || current == null) return
    if (result == null) {
        val fits = LedResistor.maxInSeries(supply, forward)
        if (fits >= 1) ErrorText(pluralStringResource(Res.plurals.led_too_many, fits, "${supply.fmt(1)} $volt", fits))
        else ErrorText(Res.string.led_supply_voltage_must_exceed.str())
        return
    }
    val drop = supply - forward * count
    val bands = ResistorCode.encode(result.e12, 4, ResistorColor.GOLD, null)
    ResultCard {
        Text(
            stringResource(Res.string.led_buy, Si.format(result.e12, ohm), "${result.ratingW.fmt(3)} ${Res.string.unit_w.str()}"),
            style = MaterialTheme.typography.titleMedium,
        )
        if (bands != null) {
            ResistorImage(bands)
            Hint(bands.map { it.title.str().lowercase() }.joinToString(", ").replaceFirstChar { it.uppercase() })
        }
        KeyValueRow(Res.string.led_actual_current.str(), "${result.currentE12Ma.fmt(1)} ${Res.string.unit_ma.str()}")
        KeyValueRow(Res.string.exact_resistance.str(), Si.format(result.resistance, ohm))
        KeyValueRow(Res.string.nearest_e24_not_below.str(), "${Si.format(result.e24, ohm)} → ${result.currentE24Ma.fmt(1)} ${Res.string.unit_ma.str()}")
        KeyValueRow(Res.string.voltage_across_resistor.str(), "${drop.fmt(2)} $volt")
        KeyValueRow(Res.string.resistor_power.str(), Si.format(result.power, Res.string.unit_w.str()))
    }
    if (drop < supply * LOW_HEADROOM) Hint(stringResource(Res.string.led_low_headroom, "${drop.fmt(2)} $volt"))
}

// the supply on the left with its long plate for plus, the resistor and the LEDs in one loop.
// Past four LEDs the chain is drawn short and signed with the count
@Composable
private fun LedCircuit(supply: String, resistor: String, count: Int, led: Color) {
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val label = MaterialTheme.typography.labelMedium.copy(color = colors.onSurface)
    Canvas(Modifier.fillMaxWidth().height(112.dp)) {
        val stroke = 2.dp.toPx()
        val left = 20.dp.toPx()
        val right = size.width - 12.dp.toPx()
        val top = 36.dp.toPx()
        val bottom = size.height - 12.dp.toPx()
        val mid = (top + bottom) / 2
        val plate = 5.dp.toPx()
        val wire = colors.onSurfaceVariant
        drawLine(wire, Offset(left, top), Offset(right, top), stroke)
        drawLine(wire, Offset(right, top), Offset(right, bottom), stroke)
        drawLine(wire, Offset(right, bottom), Offset(left, bottom), stroke)
        drawLine(wire, Offset(left, top), Offset(left, mid - plate), stroke)
        drawLine(wire, Offset(left, mid + plate), Offset(left, bottom), stroke)
        drawLine(colors.onSurface, Offset(left - 12.dp.toPx(), mid - plate), Offset(left + 12.dp.toPx(), mid - plate), stroke)
        drawLine(colors.onSurface, Offset(left - 6.dp.toPx(), mid + plate), Offset(left + 6.dp.toPx(), mid + plate), 4.dp.toPx())
        val plus = measurer.measure("+", label)
        drawText(plus, topLeft = Offset(left + 8.dp.toPx(), mid - plate - plus.size.height))
        val volts = measurer.measure(supply, label)
        drawText(volts, topLeft = Offset(left + 16.dp.toPx(), mid - volts.size.height / 2f))

        val rx = left + (right - left) * 0.28f
        val rw = 40.dp.toPx()
        val rh = 16.dp.toPx()
        drawRect(colors.surface, Offset(rx - rw / 2, top - rh / 2), Size(rw, rh))
        drawRect(colors.onSurface, Offset(rx - rw / 2, top - rh / 2), Size(rw, rh), style = Stroke(stroke))
        val value = measurer.measure(resistor, label)
        drawText(value, topLeft = Offset(rx - value.size.width / 2f, top - rh / 2 - 4.dp.toPx() - value.size.height))

        val shown = count.coerceAtMost(4)
        val start = left + (right - left) * 0.5f
        val step = (right - 16.dp.toPx() - start) / shown
        val half = 9.dp.toPx()
        repeat(shown) { i ->
            val cx = start + step * (i + 0.5f)
            val triangle = Path().apply {
                moveTo(cx - half, top - half)
                lineTo(cx + half, top)
                lineTo(cx - half, top + half)
                close()
            }
            drawPath(triangle, led)
            drawPath(triangle, colors.onSurface, style = Stroke(1.5.dp.toPx()))
            drawLine(colors.onSurface, Offset(cx + half, top - half), Offset(cx + half, top + half), stroke)
            for (k in 0..1) {
                val from = Offset(cx - 2.dp.toPx() + k * 6.dp.toPx(), top - half - 2.dp.toPx())
                val to = Offset(from.x + 6.dp.toPx(), from.y - 6.dp.toPx())
                drawLine(colors.onSurfaceVariant, from, to, 1.5.dp.toPx())
                drawLine(colors.onSurfaceVariant, to, Offset(to.x - 3.dp.toPx(), to.y), 1.5.dp.toPx())
                drawLine(colors.onSurfaceVariant, to, Offset(to.x, to.y + 3.dp.toPx()), 1.5.dp.toPx())
            }
        }
        if (count > shown) {
            val times = measurer.measure("× $count", label)
            drawText(times, topLeft = Offset(start + (right - 16.dp.toPx() - start) / 2 - times.size.width / 2f, top + half + 6.dp.toPx()))
        }
    }
}
