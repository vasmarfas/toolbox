package com.vasmarfas.card.tools.calculators

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import com.vasmarfas.card.core.fmt
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SegmentedChoice
import com.vasmarfas.card.ui.components.ToolInputField

private enum class DividerTarget { VOUT, R1, R2 }

val voltageDividerTool = Tool(
    id = "voltage-divider",
    category = ToolCategory.CALCULATORS,
    title = Res.string.voltage_divider,
    description = Res.string.output_voltage_of_an_r1_r2_divider_or_the_re,
    icon = Icons.AutoMirrored.Filled.CallSplit,
    keywords = listOf("divider", "resistor", "vout", "voltage", "делитель", "напряжение", "резистор", "vout"),
) { VoltageDividerScreen() }

@Composable
private fun VoltageDividerScreen() {
    var target by rememberSaveable { mutableStateOf(DividerTarget.VOUT) }
    var vinText by rememberSaveable { mutableStateOf("12") }
    var voutText by rememberSaveable { mutableStateOf("5") }
    var r1Text by rememberSaveable { mutableStateOf("10k") }
    var r2Text by rememberSaveable { mutableStateOf("4.7k") }
    val vin = Si.parse(vinText)?.takeIf { it > 0 }
    val vout = Si.parse(voutText)?.takeIf { it > 0 }
    val r1 = Si.parse(r1Text)?.takeIf { it > 0 }
    val r2 = Si.parse(r2Text)?.takeIf { it > 0 }
    SegmentedChoice(
        options = DividerTarget.entries,
        selected = target,
        onSelect = { target = it },
        label = {
            when (it) {
                DividerTarget.VOUT -> Res.string.find_vout.str()
                DividerTarget.R1 -> Res.string.find_r1.str()
                DividerTarget.R2 -> Res.string.find_r2.str()
            }
        },
    )
    DividerField(vinText, { vinText = it }, "Vin", "V", vin)
    if (target != DividerTarget.VOUT) DividerField(voutText, { voutText = it }, "Vout", "V", vout)
    if (target != DividerTarget.R1) DividerField(r1Text, { r1Text = it }, "R1", "Ω", r1)
    if (target != DividerTarget.R2) DividerField(r2Text, { r2Text = it }, "R2", "Ω", r2)
    val solved = when (target) {
        DividerTarget.VOUT -> if (vin != null && r1 != null && r2 != null) Triple(VoltageDivider.vout(vin, r1, r2), r1, r2) else null
        DividerTarget.R1 -> if (vin != null && vout != null && r2 != null && vout < vin) Triple(vout, VoltageDivider.r1(vin, vout, r2), r2) else null
        DividerTarget.R2 -> if (vin != null && vout != null && r1 != null && vout < vin) Triple(vout, r1, VoltageDivider.r2(vin, vout, r1)) else null
    }
    if (target != DividerTarget.VOUT && vin != null && vout != null && vout >= vin) {
        ErrorText(Res.string.vout_must_be_lower_than_vin.str())
    }
    if (vin != null && solved != null) {
        val (outVoltage, res1, res2) = solved
        val current = VoltageDivider.current(vin, res1, res2)
        ResultCard {
            KeyValueRow("Vout", Si.format(outVoltage, "V"))
            KeyValueRow("R1", Si.format(res1, "Ω"))
            KeyValueRow("R2", Si.format(res2, "Ω"))
            KeyValueRow(Res.string.current.str(), Si.format(current, "A"))
            KeyValueRow(Res.string.power_in_r1.str(), Si.format(current * current * res1, "W"))
            KeyValueRow(Res.string.power_in_r2.str(), Si.format(current * current * res2, "W"))
            KeyValueRow(Res.string.ratio_vout_vin.str(), (outVoltage / vin).fmt(4), copyable = false)
        }
        if (target != DividerTarget.VOUT) {
            val standard = LedResistor.nearestInSeries(if (target == DividerTarget.R1) res1 else res2, LedResistor.e24)
            val actual = if (target == DividerTarget.R1) VoltageDivider.vout(vin, standard, res2) else VoltageDivider.vout(vin, res1, standard)
            ResultCard(Res.string.nearest_e24_value.str()) {
                KeyValueRow(if (target == DividerTarget.R1) "R1" else "R2", Si.format(standard, "Ω"))
                KeyValueRow(Res.string.vout_with_it.str(), Si.format(actual, "V"))
            }
        }
    }
}

@Composable
private fun DividerField(value: String, onValueChange: (String) -> Unit, label: String, unit: String, parsed: Double?) {
    ToolInputField(
        value = value,
        onValueChange = onValueChange,
        label = "$label ($unit)",
        placeholder = "4.7k · 100m · 1e3",
        keyboardType = KeyboardType.Ascii,
        isError = value.isNotBlank() && parsed == null,
        monospace = true,
    )
}
