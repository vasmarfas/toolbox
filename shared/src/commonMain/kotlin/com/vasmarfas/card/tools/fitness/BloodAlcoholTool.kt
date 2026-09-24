package com.vasmarfas.card.tools.fitness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalBar
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
import com.vasmarfas.card.ui.components.AnswerCard
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SegmentedChoice

val bloodAlcoholTool = Tool(
    id = "blood-alcohol",
    category = ToolCategory.FITNESS,
    title = Res.string.blood_alcohol,
    description = Res.string.blood_alcohol_description,
    icon = Icons.Filled.LocalBar,
    keywords = listOf(
        "alcohol", "bac", "widmark", "promille", "ethanol", "sober",
        "алкоголь", "промилле", "видмарк", "этанол", "вытрезвление", "опьянение",
    ),
) { BloodAlcoholScreen() }

private class Drink(val volumeMl: Double, val abv: Double, val label: StringKey)

private enum class StringKey { BEER, WINE, SPIRIT }

private val presets = listOf(
    Drink(500.0, 5.0, StringKey.BEER),
    Drink(150.0, 12.0, StringKey.WINE),
    Drink(50.0, 40.0, StringKey.SPIRIT),
)

@Composable
private fun BloodAlcoholScreen() {
    var volumeText by rememberSaveable { mutableStateOf("500") }
    var abvText by rememberSaveable { mutableStateOf("5") }
    var massText by rememberSaveable { mutableStateOf("75") }
    var hoursText by rememberSaveable { mutableStateOf("1") }
    var sex by rememberSaveable { mutableStateOf(Sex.MALE) }

    ChoiceChips(
        options = presets,
        selected = null,
        onSelect = {
            volumeText = it.volumeMl.fmt(0)
            abvText = it.abv.fmt(0)
        },
        label = {
            when (it.label) {
                StringKey.BEER -> Res.string.drink_beer.str()
                StringKey.WINE -> Res.string.drink_wine.str()
                StringKey.SPIRIT -> Res.string.drink_spirit.str()
            }
        },
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        NumberField(
            value = volumeText,
            onValueChange = { volumeText = it },
            label = Res.string.volume.str(),
            modifier = Modifier.weight(1f),
            suffix = Res.string.ml.str(),
            isError = volumeText.toDoubleLenient().let { it == null || it <= 0 },
        )
        NumberField(
            value = abvText,
            onValueChange = { abvText = it },
            label = Res.string.abv.str(),
            modifier = Modifier.weight(1f),
            suffix = "%",
            isError = abvText.toDoubleLenient().let { it == null || it < 0 || it > 100 },
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        NumberField(
            value = massText,
            onValueChange = { massText = it },
            label = Res.string.weight.str(),
            modifier = Modifier.weight(1f),
            suffix = Res.string.unit_kg.str(),
            isError = massText.toDoubleLenient().let { it == null || it <= 0 },
        )
        NumberField(
            value = hoursText,
            onValueChange = { hoursText = it },
            label = Res.string.hours_since.str(),
            modifier = Modifier.weight(1f),
            isError = hoursText.toDoubleLenient().let { it == null || it < 0 },
        )
    }
    SegmentedChoice(
        options = Sex.entries,
        selected = sex,
        onSelect = { sex = it },
        label = { if (it == Sex.MALE) Res.string.male.str() else Res.string.female.str() },
        modifier = Modifier.fillMaxWidth(),
    )

    val volume = volumeText.toDoubleLenient()
    val abv = abvText.toDoubleLenient()
    val mass = massText.toDoubleLenient()
    val hours = hoursText.toDoubleLenient()
    if (volume == null || volume <= 0 || abv == null || abv < 0 || abv > 100 ||
        mass == null || mass <= 0 || hours == null || hours < 0
    ) {
        ErrorText(Res.string.alcohol_input_error.str())
        return
    }

    val grams = Widmark.gramsOfEthanol(volume, abv)
    val typical = Widmark.promille(grams, mass, sex, hours)
    val low = Widmark.promille(grams, mass, sex, hours, Widmark.EliminationHigh)
    val high = Widmark.promille(grams, mass, sex, hours, Widmark.EliminationLow)
    val clear = Widmark.hoursToClear(grams, mass, sex) - hours

    AnswerCard(
        "${typical.fmt(2)} ‰",
        Res.string.time_to_zero.str() + ": " + if (clear <= 0) Res.string.already_zero.str() else "${clear.fmt(1)} ${Res.string.unit_h.str()}",
        copyValue = typical.fmt(2),
    )
    ResultCard {
        KeyValueRow(Res.string.pure_ethanol.str(), "${grams.fmt(1)} ${Res.string.unit_g.str()}", copyable = false)
        KeyValueRow(Res.string.plausible_spread.str(), "${low.fmt(2)}—${high.fmt(2)} ‰", copyable = false)
        KeyValueRow(
            Res.string.time_to_zero.str(),
            if (clear <= 0) Res.string.already_zero.str() else "${clear.fmt(1)} ${Res.string.unit_h.str()}",
            copyable = false,
        )
    }
    Text(
        Res.string.alcohol_warning.str(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}
