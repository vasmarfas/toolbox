package com.vasmarfas.card.tools.fitness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
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
import com.vasmarfas.card.tools.network.SimpleTable
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ResultCard

val caffeineTool = Tool(
    id = "caffeine-decay",
    category = ToolCategory.FITNESS,
    title = Res.string.caffeine,
    description = Res.string.caffeine_description,
    icon = Icons.Filled.Coffee,
    keywords = listOf(
        "caffeine", "coffee", "half-life", "sleep", "espresso", "energy drink",
        "кофеин", "кофе", "полувыведение", "сон", "эспрессо", "энергетик",
    ),
) { CaffeineScreen() }

private val servings = listOf(80, 150, 200, 320)

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
            suffix = Res.string.mg.str(),
            isError = doseText.toDoubleLenient().let { it == null || it <= 0 },
        )
        NumberField(
            value = halfLifeText,
            onValueChange = { halfLifeText = it },
            label = Res.string.half_life.str(),
            modifier = Modifier.weight(1f),
            suffix = Res.string.h.str(),
            isError = halfLifeText.toDoubleLenient().let { it == null || it <= 0 },
        )
    }
    ChoiceChips(
        options = servings,
        selected = doseText.trim().toIntOrNull(),
        onSelect = { doseText = it.toString() },
        label = { "$it ${Res.string.mg.str()}" },
    )
    NumberField(
        value = thresholdText,
        onValueChange = { thresholdText = it },
        label = Res.string.sleep_threshold.str(),
        suffix = Res.string.mg.str(),
        modifier = Modifier.fillMaxWidth(),
        isError = thresholdText.toDoubleLenient().let { it == null || it <= 0 },
    )

    val dose = doseText.toDoubleLenient()
    val halfLife = halfLifeText.toDoubleLenient()
    val threshold = thresholdText.toDoubleLenient()
    if (dose == null || dose <= 0 || halfLife == null || halfLife <= 0 || threshold == null || threshold <= 0) {
        ErrorText(Res.string.caffeine_input_error.str())
        return
    }

    val until = Caffeine.hoursUntil(dose, threshold, halfLife)
    ResultCard {
        KeyValueRow(
            Res.string.down_to_threshold.str(),
            if (until == null) Res.string.already_below.str() else "${until.fmt(1)} ${Res.string.h.str()}",
            copyable = false,
        )
    }
    SimpleTable(
        header = listOf(Res.string.hours.str(), Res.string.remaining.str()),
        rows = listOf(0, 2, 4, 6, 8, 10, 12).map { hour ->
            listOf("$hour", "${Caffeine.remainingMg(dose, hour.toDouble(), halfLife).fmt(0)} ${Res.string.mg.str()}")
        },
    )
    Text(
        Res.string.caffeine_note.str(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
