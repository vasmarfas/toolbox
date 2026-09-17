package com.vasmarfas.card.tools.calculators

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.tools.converters.fmtSig
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.ToolInputField

val ohmsLawTool = Tool(
    id = "ohms-law",
    category = ToolCategory.CALCULATORS,
    title = Res.string.ohm_s_law,
    description = Res.string.enter_any_two_of_voltage_current_resistance,
    icon = Icons.Filled.Bolt,
    keywords = listOf("ohm", "voltage", "current", "resistance", "power", "watt", "ампер", "вольт", "ом", "сопротивление", "мощность", "ток", "напряжение"),
) { OhmsLawScreen() }

@Composable
private fun OhmsLawScreen() {
    var vText by rememberSaveable { mutableStateOf("12") }
    var iText by rememberSaveable { mutableStateOf("") }
    var rText by rememberSaveable { mutableStateOf("4.7k") }
    var pText by rememberSaveable { mutableStateOf("") }
    val v = Si.parse(vText)
    val i = Si.parse(iText)
    val r = Si.parse(rText)
    val p = Si.parse(pText)
    SiField(vText, { vText = it }, Res.string.voltage_v.str(), "V", v)
    SiField(iText, { iText = it }, Res.string.current_i.str(), "A", i)
    SiField(rText, { rText = it }, Res.string.resistance_r.str(), "Ω", r)
    SiField(pText, { pText = it }, Res.string.power_p.str(), "W", p)
    val filled = listOf(vText, iText, rText, pText).count { it.isNotBlank() }
    val result = OhmsLaw.solve(v, i, r, p)
    when {
        filled != 2 -> ErrorText(Res.string.fill_exactly_two_fields_leave_the_other_two.str())
        result == null -> ErrorText(Res.string.check_the_values_use_numbers_with_optional_s.str())
        else -> ResultCard {
            KeyValueRow(Res.string.voltage.str(), Si.format(result.voltage, "V"))
            KeyValueRow(Res.string.current.str(), Si.format(result.current, "A"))
            KeyValueRow(Res.string.resistance.str(), Si.format(result.resistance, "Ω"))
            KeyValueRow(Res.string.power.str(), Si.format(result.power, "W"))
            KeyValueRow(
                Res.string.plain_values.str(),
                "${result.voltage.fmtSig(6)} V · ${result.current.fmtSig(6)} A · ${result.resistance.fmtSig(6)} Ω · ${result.power.fmtSig(6)} W",
                copyable = false,
            )
        }
    }
}

@Composable
private fun SiField(value: String, onValueChange: (String) -> Unit, label: String, unit: String, parsed: Double?) {
    ToolInputField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = "4.7k · 100m · 22u · 1e3 $unit",
        keyboardType = KeyboardType.Ascii,
        isError = value.isNotBlank() && parsed == null,
        monospace = true,
    )
}
