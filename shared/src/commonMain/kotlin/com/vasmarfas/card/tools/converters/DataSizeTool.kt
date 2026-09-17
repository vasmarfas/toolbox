package com.vasmarfas.card.tools.converters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.str
import com.vasmarfas.card.core.toDoubleLenient
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.DropdownChoice
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ResultCard

val dataSizeTool = Tool(
    id = "data-size",
    category = ToolCategory.CONVERTERS,
    title = Res.string.data_size_and_transfer_time,
    description = Res.string.bits_and_bytes_with_decimal_kb_mb_and_binary,
    icon = Icons.Filled.Storage,
    keywords = listOf("bytes", "bits", "kib", "mib", "gib", "download", "bandwidth", "байты", "биты", "мегабайт", "гигабайт", "скачивание", "скорость"),
) { DataSizeScreen() }

private val referenceSpeeds = listOf(10.0 to SpeedUnit.MBIT_S, 100.0 to SpeedUnit.MBIT_S, 1.0 to SpeedUnit.GBIT_S)

@Composable
private fun DataSizeScreen() {
    var input by rememberSaveable { mutableStateOf("1") }
    var unit by rememberSaveable { mutableStateOf(DataUnit.GB) }
    var speedText by rememberSaveable { mutableStateOf("100") }
    var speedUnit by rememberSaveable { mutableStateOf(SpeedUnit.MBIT_S) }
    val value = input.toDoubleLenient()
    val speed = speedText.toDoubleLenient()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NumberField(
            value = input,
            onValueChange = { input = it },
            label = Res.string.size_2.str(),
            modifier = Modifier.weight(1f),
            isError = input.isNotBlank() && value == null,
        )
        DropdownChoice(
            options = DataUnit.entries,
            selected = unit,
            onSelect = { unit = it },
            label = Res.string.unit.str(),
            text = { it.symbol },
            modifier = Modifier.weight(1f),
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NumberField(
            value = speedText,
            onValueChange = { speedText = it },
            label = Res.string.speed.str(),
            modifier = Modifier.weight(1f),
            isError = speedText.isNotBlank() && speed == null,
        )
        DropdownChoice(
            options = SpeedUnit.entries,
            selected = speedUnit,
            onSelect = { speedUnit = it },
            label = Res.string.speed_unit.str(),
            text = { it.symbol },
            modifier = Modifier.weight(1f),
        )
    }
    if (value != null) {
        ResultCard(Res.string.transfer_time.str()) {
            if (speed != null && speed > 0) {
                KeyValueRow(
                    "${speed.fmtSig()} ${speedUnit.symbol}",
                    DataSize.formatDuration(DataSize.transferSeconds(value, unit, speed, speedUnit)),
                )
            }
            referenceSpeeds.forEach { (refSpeed, refUnit) ->
                KeyValueRow("${refSpeed.fmtSig()} ${refUnit.symbol}", DataSize.formatDuration(DataSize.transferSeconds(value, unit, refSpeed, refUnit)))
            }
        }
        UnitGroupCard(Res.string.decimal_si.str(), DataGroup.DECIMAL, value, unit)
        UnitGroupCard(Res.string.binary_iec.str(), DataGroup.BINARY, value, unit)
        UnitGroupCard(Res.string.bits.str(), DataGroup.BITS, value, unit)
    }
}

@Composable
private fun UnitGroupCard(title: String, group: DataGroup, value: Double, unit: DataUnit) {
    ResultCard(title) {
        DataUnit.entries.filter { it.group == group }.forEach { target ->
            KeyValueRow(target.symbol, DataSize.convert(value, unit, target).fmtSig())
        }
    }
}
