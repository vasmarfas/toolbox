package com.vasmarfas.card.tools.fitness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
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
import com.vasmarfas.card.ui.components.AnswerCard
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.Hint
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ToolSection
import org.jetbrains.compose.resources.StringResource

val caffeineTool = Tool(
    id = "caffeine-decay",
    category = ToolCategory.FITNESS,
    title = Res.string.caffeine,
    description = Res.string.caffeine_decay_description,
    icon = Icons.Filled.Coffee,
    keywords = listOf(
        "caffeine", "coffee", "half-life", "sleep", "espresso", "energy drink",
        "кофеин", "кофе", "полувыведение", "сон", "эспрессо", "энергетик",
    ),
) { CaffeineScreen() }

private class CaffeineDrink(val name: StringResource, val mg: Int)

// typical amounts: USDA for coffee and tea, the labels of the common energy drinks and colas
private val drinks = listOf(
    CaffeineDrink(Res.string.drink_espresso, 65),
    CaffeineDrink(Res.string.drink_americano, 125),
    CaffeineDrink(Res.string.drink_cappuccino, 65),
    CaffeineDrink(Res.string.drink_instant, 60),
    CaffeineDrink(Res.string.drink_black_tea, 45),
    CaffeineDrink(Res.string.drink_green_tea, 30),
    CaffeineDrink(Res.string.drink_energy_small, 80),
    CaffeineDrink(Res.string.drink_energy_large, 160),
    CaffeineDrink(Res.string.drink_cola, 32),
    CaffeineDrink(Res.string.drink_dark_chocolate, 40),
)

@Composable
private fun CaffeineScreen() {
    var doseText by rememberSaveable { mutableStateOf("150") }
    var halfLifeText by rememberSaveable { mutableStateOf("5") }
    var thresholdText by rememberSaveable { mutableStateOf("50") }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        NumberField(
            value = doseText,
            onValueChange = { doseText = it },
            label = Res.string.dose.str(),
            modifier = Modifier.weight(1f),
            suffix = Res.string.unit_mg.str(),
            isError = doseText.toDoubleLenient().let { it == null || it <= 0 },
        )
        NumberField(
            value = halfLifeText,
            onValueChange = { halfLifeText = it },
            label = Res.string.half_life.str(),
            modifier = Modifier.weight(1f),
            suffix = Res.string.unit_h.str(),
            isError = halfLifeText.toDoubleLenient().let { it == null || it <= 0 },
        )
    }
    ToolSection(Res.string.what_you_drank.str()) {
        ChoiceChips(
            options = drinks,
            selected = drinks.firstOrNull { it.mg == doseText.trim().toIntOrNull() },
            onSelect = { doseText = it.mg.toString() },
            label = { "${it.name.str()} · ${it.mg} ${Res.string.unit_mg.str()}" },
        )
    }
    NumberField(
        value = thresholdText,
        onValueChange = { thresholdText = it },
        label = Res.string.sleep_threshold.str(),
        suffix = Res.string.unit_mg.str(),
        modifier = Modifier.fillMaxWidth(),
        isError = thresholdText.toDoubleLenient().let { it == null || it <= 0 },
    )
    Hint(Res.string.sleep_threshold_hint.str())

    val dose = doseText.toDoubleLenient()
    val halfLife = halfLifeText.toDoubleLenient()
    val threshold = thresholdText.toDoubleLenient()
    if (dose == null || dose <= 0 || halfLife == null || halfLife <= 0 || threshold == null || threshold <= 0) {
        ErrorText(Res.string.caffeine_input_error.str())
        return
    }

    val until = Caffeine.hoursUntil(dose, threshold, halfLife)
    AnswerCard(
        if (until == null) Res.string.already_below.str() else "${until.fmt(1)} ${Res.string.unit_h.str()}",
        Res.string.down_to_threshold.str(),
    )
    SimpleTable(
        header = listOf(Res.string.hours.str(), Res.string.remaining.str()),
        rows = listOf(0, 2, 4, 6, 8, 10, 12).map { hour ->
            listOf("$hour", "${Caffeine.remainingMg(dose, hour.toDouble(), halfLife).fmt(0)} ${Res.string.unit_mg.str()}")
        },
    )
    Hint(Res.string.caffeine_note.str())
}
