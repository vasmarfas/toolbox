package com.vasmarfas.card.tools.printing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Straighten
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
import com.vasmarfas.card.tools.network.SimpleTable
import com.vasmarfas.card.ui.components.DropdownChoice
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SegmentedChoice
import com.vasmarfas.card.ui.components.ToolSection

private enum class FilamentUnit { GRAMS, METRES, VOLUME }

val filamentLengthWeightTool = Tool(
    id = "filament-length-weight",
    category = ToolCategory.PRINTING,
    title = Res.string.filament_length_and_weight,
    description = Res.string.filament_length_weight_description,
    icon = Icons.Filled.Straighten,
    keywords = listOf("filament", "spool", "grams", "metres", "density", "remaining", "пруток", "катушка", "граммы", "метры", "плотность", "остаток"),
) { FilamentLengthWeightScreen() }

@Composable
private fun FilamentLengthWeightScreen() {
    var material by rememberSaveable { mutableStateOf(FilamentMaterial.PLA) }
    var diameter by rememberSaveable { mutableStateOf(1.75) }
    var unit by rememberSaveable { mutableStateOf(FilamentUnit.GRAMS) }
    var amountText by rememberSaveable { mutableStateOf("100") }
    var measuredText by rememberSaveable { mutableStateOf("1180") }
    var emptySpoolText by rememberSaveable { mutableStateOf("230") }

    DropdownChoice(
        options = FilamentMaterial.entries,
        selected = material,
        onSelect = { material = it },
        label = Res.string.material.str(),
        text = { "${it.title.str()} · ${it.density.fmt(2)} ${Res.string.unit_g_cm3.str()}" },
    )
    SegmentedChoice(
        options = filamentDiameters,
        selected = diameter,
        onSelect = { diameter = it },
        label = { "${it.fmt(2)} ${Res.string.unit_mm.str()}" },
    )
    SegmentedChoice(
        options = FilamentUnit.entries,
        selected = unit,
        onSelect = { unit = it },
        label = {
            when (it) {
                FilamentUnit.GRAMS -> Res.string.grams.str()
                FilamentUnit.METRES -> Res.string.metres.str()
                FilamentUnit.VOLUME -> Res.string.unit_cm3.str()
            }
        },
    )
    val amount = amountText.toDoubleLenient()
    NumberField(
        value = amountText,
        onValueChange = { amountText = it },
        label = Res.string.amount.str(),
        suffix = when (unit) {
            FilamentUnit.GRAMS -> Res.string.unit_g.str()
            FilamentUnit.METRES -> Res.string.unit_m.str()
            FilamentUnit.VOLUME -> Res.string.unit_cm3.str()
        },
        isError = amount == null || amount < 0,
    )
    if (amount == null || amount < 0) {
        ErrorText(Res.string.enter_a_non_negative_number.str())
        return
    }

    val grams = when (unit) {
        FilamentUnit.GRAMS -> amount
        FilamentUnit.METRES -> Filament.grams(amount, diameter, material.density)
        FilamentUnit.VOLUME -> amount * material.density
    }
    val metres = Filament.lengthM(grams, diameter, material.density)
    val volume = grams / material.density
    ResultCard {
        KeyValueRow(Res.string.weight.str(), "${grams.fmt(2)} ${Res.string.unit_g.str()}")
        KeyValueRow(Res.string.length.str(), "${metres.fmt(3)} ${Res.string.unit_m.str()}")
        KeyValueRow(Res.string.volume.str(), "${volume.fmt(3)} ${Res.string.unit_cm3.str()}")
        KeyValueRow(Res.string.grams_per_metre.str(), Filament.gramsPerMetre(diameter, material.density).fmt(3))
    }

    ToolSection(Res.string.spool_remaining.str()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            NumberField(
                value = measuredText,
                onValueChange = { measuredText = it },
                label = Res.string.measured_total_weight.str(),
                modifier = Modifier.weight(1f),
                suffix = Res.string.unit_g.str(),
                isError = measuredText.toDoubleLenient() == null,
            )
            NumberField(
                value = emptySpoolText,
                onValueChange = { emptySpoolText = it },
                label = Res.string.empty_spool_weight.str(),
                modifier = Modifier.weight(1f),
                suffix = Res.string.unit_g.str(),
                isError = emptySpoolText.toDoubleLenient() == null,
            )
        }
    }
    val measured = measuredText.toDoubleLenient()
    val emptySpool = emptySpoolText.toDoubleLenient()
    if (measured == null || emptySpool == null) {
        ErrorText(Res.string.check_the_weights.str())
        return
    }
    val remaining = measured - emptySpool
    if (remaining < 0) {
        ErrorText(Res.string.filament_spool_alone_weighs_more.str())
        return
    }
    ResultCard(Res.string.left_on_the_spool.str()) {
        KeyValueRow(Res.string.filament.str(), "${remaining.fmt(1)} ${Res.string.unit_g.str()}")
        KeyValueRow(Res.string.length.str(), "${Filament.lengthM(remaining, diameter, material.density).fmt(2)} ${Res.string.unit_m.str()}")
        KeyValueRow(Res.string.prints_of_this_model.str(), if (grams > 0) (remaining / grams).fmt(1) else "—")
    }
    ResultCard(Res.string.grams_per_metre_by_material.str()) {
        SimpleTable(
            header = listOf(
                Res.string.material.str(),
                Res.string.unit_g_cm3.str(),
                "1.75 ${Res.string.unit_mm.str()}",
                "2.85 ${Res.string.unit_mm.str()}",
            ),
            rows = FilamentMaterial.entries.map {
                listOf(
                    it.title.str(),
                    it.density.fmt(2),
                    Filament.gramsPerMetre(1.75, it.density).fmt(3),
                    Filament.gramsPerMetre(2.85, it.density).fmt(3),
                )
            },
            weights = listOf(1.6f, 1f, 1f, 1f),
            mono = false,
        )
    }
}
