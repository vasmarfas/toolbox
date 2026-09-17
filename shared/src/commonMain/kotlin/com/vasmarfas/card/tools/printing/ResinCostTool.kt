package com.vasmarfas.card.tools.printing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.fmt
import com.vasmarfas.card.core.str
import com.vasmarfas.card.core.toDoubleLenient
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.ToolSection

val resinCostTool = Tool(
    id = "resin-cost",
    category = ToolCategory.PRINTING,
    title = Res.string.resin_print_cost,
    description = Res.string.msla_print_cost_resin_with_support_waste_ipa,
    icon = Icons.Filled.ViewInAr,
    keywords = listOf("resin", "msla", "sla", "lcd", "ipa", "cost", "смола", "фотополимер", "изопропанол", "себестоимость"),
) { ResinCostScreen() }

@Composable
private fun ResinCostScreen() {
    var volumeText by rememberSaveable { mutableStateOf("35") }
    var wasteText by rememberSaveable { mutableStateOf("25") }
    var priceText by rememberSaveable { mutableStateOf("3000") }
    var ipaMlText by rememberSaveable { mutableStateOf("80") }
    var ipaPriceText by rememberSaveable { mutableStateOf("400") }
    var glovesText by rememberSaveable { mutableStateOf("1") }
    var glovePriceText by rememberSaveable { mutableStateOf("20") }
    var hoursText by rememberSaveable { mutableStateOf("5") }
    var minutesText by rememberSaveable { mutableStateOf("0") }
    var powerText by rememberSaveable { mutableStateOf("60") }
    var kwhText by rememberSaveable { mutableStateOf("6") }

    Text(
        text = Res.string.for_msla_lcd_printers_the_resin_volume_comes.str(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        NumberField(
            value = volumeText,
            onValueChange = { volumeText = it },
            label = Res.string.model_volume.str(),
            modifier = Modifier.weight(1f),
            suffix = Res.string.ml.str(),
            isError = volumeText.toDoubleLenient().let { it == null || it <= 0 },
        )
        NumberField(
            value = wasteText,
            onValueChange = { wasteText = it },
            label = Res.string.supports_and_waste.str(),
            modifier = Modifier.weight(1f),
            suffix = "%",
            isError = wasteText.toDoubleLenient().let { it == null || it < 0 },
        )
    }
    NumberField(
        value = priceText,
        onValueChange = { priceText = it },
        label = Res.string.resin_price_per_litre.str(),
        isError = priceText.toDoubleLenient().let { it == null || it < 0 },
    )
    ToolSection(Res.string.consumables.str()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            NumberField(
                value = ipaMlText,
                onValueChange = { ipaMlText = it },
                label = Res.string.ipa_per_print.str(),
                modifier = Modifier.weight(1f),
                suffix = Res.string.ml.str(),
                isError = ipaMlText.toDoubleLenient().let { it == null || it < 0 },
            )
            NumberField(
                value = ipaPriceText,
                onValueChange = { ipaPriceText = it },
                label = Res.string.ipa_per_litre.str(),
                modifier = Modifier.weight(1f),
                isError = ipaPriceText.toDoubleLenient().let { it == null || it < 0 },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            NumberField(
                value = glovesText,
                onValueChange = { glovesText = it },
                label = Res.string.glove_pairs.str(),
                modifier = Modifier.weight(1f),
                isError = glovesText.toDoubleLenient().let { it == null || it < 0 },
            )
            NumberField(
                value = glovePriceText,
                onValueChange = { glovePriceText = it },
                label = Res.string.price_per_pair.str(),
                modifier = Modifier.weight(1f),
                isError = glovePriceText.toDoubleLenient().let { it == null || it < 0 },
            )
        }
    }
    ToolSection(Res.string.machine_time.str()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            NumberField(
                value = hoursText,
                onValueChange = { hoursText = it },
                label = Res.string.hours_2.str(),
                modifier = Modifier.weight(1f),
                isError = hoursText.toDoubleLenient() == null,
            )
            NumberField(
                value = minutesText,
                onValueChange = { minutesText = it },
                label = Res.string.minutes_2.str(),
                modifier = Modifier.weight(1f),
                isError = minutesText.toDoubleLenient() == null,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            NumberField(
                value = powerText,
                onValueChange = { powerText = it },
                label = Res.string.printer_power.str(),
                modifier = Modifier.weight(1f),
                suffix = Res.string.w.str(),
                isError = powerText.toDoubleLenient() == null,
            )
            NumberField(
                value = kwhText,
                onValueChange = { kwhText = it },
                label = Res.string.price_per_kwh.str(),
                modifier = Modifier.weight(1f),
                isError = kwhText.toDoubleLenient() == null,
            )
        }
    }

    val values = listOf(
        volumeText, wasteText, priceText, ipaMlText, ipaPriceText,
        glovesText, glovePriceText, hoursText, minutesText, powerText, kwhText,
    ).map { it.toDoubleLenient() }
    if (values.any { it == null || it < 0 } || values[0]!! <= 0) {
        ErrorText(Res.string.fill_in_every_field_with_non_negative_number.str())
        return
    }
    val hours = values[7]!! + values[8]!! / 60.0
    val cost = PrintEconomics.resinCost(
        modelMl = values[0]!!,
        supportWastePercent = values[1]!!,
        pricePerLitre = values[2]!!,
        ipaMl = values[3]!!,
        ipaPricePerLitre = values[4]!!,
        glovePairs = values[5]!!,
        pricePerPair = values[6]!!,
        hours = hours,
        powerW = values[9]!!,
        pricePerKwh = values[10]!!,
    )
    ResultCard {
        KeyValueRow(Res.string.resin_used.str(), "${cost.resinMl.fmt(1)} ${Res.string.ml.str()}")
        KeyValueRow(Res.string.resin.str(), cost.resin.fmt(2, grouping = true))
        KeyValueRow(Res.string.ipa.str(), cost.ipa.fmt(2, grouping = true))
        KeyValueRow(Res.string.gloves.str(), cost.gloves.fmt(2, grouping = true))
        KeyValueRow(
            Res.string.electricity.str(),
            "${cost.energy.fmt(2, grouping = true)} · ${cost.energyKwh.fmt(3)} ${Res.string.kwh.str()}",
        )
        KeyValueRow(Res.string.total_per_model.str(), cost.total.fmt(2, grouping = true))
        KeyValueRow(Res.string.per_millilitre.str(), (cost.total / cost.resinMl).fmt(3))
        KeyValueRow(Res.string.models_per_litre.str(), (1000.0 / cost.resinMl).fmt(1))
    }
}
