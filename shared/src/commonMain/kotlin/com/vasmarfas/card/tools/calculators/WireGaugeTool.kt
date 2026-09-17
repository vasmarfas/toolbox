package com.vasmarfas.card.tools.calculators

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
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
import com.vasmarfas.card.ui.components.DropdownChoice
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.MonoTable
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SegmentedChoice
import com.vasmarfas.card.ui.components.ToolSection

private enum class WireLookup { AWG, AREA, DIAMETER }

val wireGaugeTool = Tool(
    id = "wire-gauge",
    category = ToolCategory.CALCULATORS,
    title = Res.string.wire_gauge,
    description = Res.string.awg_mm_diameter_with_copper_resistance_per_k,
    icon = Icons.Filled.Cable,
    keywords = listOf("awg", "wire", "gauge", "mm2", "ampacity", "copper", "cable", "провод", "сечение", "кабель", "ток", "медь", "диаметр"),
) { WireGaugeScreen() }

@Composable
private fun WireGaugeScreen() {
    var lookup by rememberSaveable { mutableStateOf(WireLookup.AWG) }
    var awg by rememberSaveable { mutableStateOf(14) }
    var valueText by rememberSaveable { mutableStateOf("2.5") }
    val value = valueText.toDoubleLenient()?.takeIf { it > 0 }
    SegmentedChoice(
        options = WireLookup.entries,
        selected = lookup,
        onSelect = { lookup = it },
        label = {
            when (it) {
                WireLookup.AWG -> "AWG"
                WireLookup.AREA -> "mm²"
                WireLookup.DIAMETER -> Res.string.diameter.str()
            }
        },
    )
    val row = when (lookup) {
        WireLookup.AWG -> {
            DropdownChoice(
                options = WireGauge.table,
                selected = WireGauge.table.first { it.awg == awg },
                onSelect = { awg = it.awg },
                label = "AWG",
                text = { "AWG ${it.label} · ${it.areaMm2.fmt(2)} mm²" },
            )
            WireGauge.table.first { it.awg == awg }
        }
        WireLookup.AREA -> {
            NumberField(
                value = valueText,
                onValueChange = { valueText = it },
                label = Res.string.cross_section.str(),
                suffix = "mm²",
                isError = valueText.isNotBlank() && value == null,
            )
            value?.let { WireGauge.nearest(WireGauge.awgFromArea(it)) }
        }
        WireLookup.DIAMETER -> {
            NumberField(
                value = valueText,
                onValueChange = { valueText = it },
                label = Res.string.conductor_diameter.str(),
                suffix = "mm",
                isError = valueText.isNotBlank() && value == null,
            )
            value?.let { WireGauge.nearest(WireGauge.awgFromDiameter(it)) }
        }
    }
    if (row != null) {
        ResultCard {
            if (lookup != WireLookup.AWG && value != null) {
                val exact = if (lookup == WireLookup.AREA) WireGauge.awgFromArea(value) else WireGauge.awgFromDiameter(value)
                KeyValueRow(Res.string.exact_awg.str(), exact.fmt(2))
            }
            KeyValueRow(Res.string.nearest_awg.str(), row.label)
            KeyValueRow(Res.string.diameter.str(), "${row.diameterMm.fmt(3)} mm")
            KeyValueRow(Res.string.cross_section.str(), "${row.areaMm2.fmt(3)} mm²")
            KeyValueRow(Res.string.copper_resistance.str(), "${row.ohmsPerKm.fmt(3)} Ω/km")
            KeyValueRow(Res.string.ampacity_chassis_wiring.str(), "${row.chassisAmps.fmt(2)} A")
            KeyValueRow(Res.string.ampacity_power_transmission.str(), "${row.transmissionAmps.fmt(3)} A")
        }
    }
    ToolSection(Res.string.awg_table_copper.str()) {
        MonoTable(
            listOf("AWG   mm      mm²      Ω/km      A chassis  A power") +
                WireGauge.table.map {
                    it.label.padEnd(6) + it.diameterMm.fmt(3).padEnd(8) + it.areaMm2.fmt(3).padEnd(9) +
                        it.ohmsPerKm.fmt(3).padEnd(10) + it.chassisAmps.fmt(2).padEnd(11) + it.transmissionAmps.fmt(3)
                },
        )
    }
}
