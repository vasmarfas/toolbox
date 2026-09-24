package com.vasmarfas.card.tools.electronics

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
    category = ToolCategory.ELECTRONICS,
    title = Res.string.wire_gauge,
    description = Res.string.wire_gauge_description,
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
                WireLookup.AREA -> Res.string.unit_mm2.str()
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
                text = { "AWG ${it.label} · ${it.areaMm2.fmt(2)} ${Res.string.unit_mm2.str()}" },
            )
            WireGauge.table.first { it.awg == awg }
        }
        WireLookup.AREA -> {
            NumberField(
                value = valueText,
                onValueChange = { valueText = it },
                label = Res.string.cross_section.str(),
                suffix = Res.string.unit_mm2.str(),
                isError = valueText.isNotBlank() && value == null,
            )
            value?.let { WireGauge.nearest(WireGauge.awgFromArea(it)) }
        }
        WireLookup.DIAMETER -> {
            NumberField(
                value = valueText,
                onValueChange = { valueText = it },
                label = Res.string.conductor_diameter.str(),
                suffix = Res.string.unit_mm.str(),
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
            KeyValueRow(Res.string.diameter.str(), "${row.diameterMm.fmt(3)} ${Res.string.unit_mm.str()}")
            KeyValueRow(Res.string.cross_section.str(), "${row.areaMm2.fmt(3)} ${Res.string.unit_mm2.str()}")
            KeyValueRow(Res.string.copper_resistance.str(), "${row.ohmsPerKm.fmt(3)} ${Res.string.unit_ohm_km.str()}")
            KeyValueRow(Res.string.ampacity_chassis_wiring.str(), "${row.chassisAmps.fmt(2)} ${Res.string.unit_a.str()}")
            KeyValueRow(Res.string.ampacity_power_transmission.str(), "${row.transmissionAmps.fmt(3)} ${Res.string.unit_a.str()}")
        }
    }
    ToolSection(Res.string.awg_table_copper.str()) {
        val header = listOf("AWG", Res.string.unit_mm.str(), Res.string.unit_mm2.str(), Res.string.unit_ohm_km.str(), Res.string.wire_header_chassis.str(), Res.string.wire_header_power.str())
        MonoTable(
            listOf(header.zip(listOf(6, 8, 9, 10, 11, 0)) { name, width -> name.padEnd(width) }.joinToString("")) +
                WireGauge.table.map {
                    it.label.padEnd(6) + it.diameterMm.fmt(3).padEnd(8) + it.areaMm2.fmt(3).padEnd(9) +
                        it.ohmsPerKm.fmt(3).padEnd(10) + it.chassisAmps.fmt(2).padEnd(11) + it.transmissionAmps.fmt(3)
                },
        )
    }
}
