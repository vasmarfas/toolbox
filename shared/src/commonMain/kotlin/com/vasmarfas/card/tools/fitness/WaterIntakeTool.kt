package com.vasmarfas.card.tools.fitness

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.vasmarfas.card.ui.components.AnswerCard
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SegmentedChoice

val waterIntakeTool = Tool(
    id = "water-intake",
    category = ToolCategory.FITNESS,
    title = Res.string.water_intake,
    description = Res.string.water_intake_description,
    icon = Icons.Filled.WaterDrop,
    keywords = listOf("water", "hydration", "glasses", "health", "вода", "норма", "стаканы", "здоровье"),
) { WaterIntakeScreen() }

@Composable
private fun WaterIntakeScreen() {
    var weightText by rememberSaveable { mutableStateOf("70") }
    var activity by rememberSaveable { mutableStateOf(WaterActivity.LOW) }
    var climate by rememberSaveable { mutableStateOf(Climate.TEMPERATE) }
    var glassMl by rememberSaveable { mutableStateOf(250) }
    NumberField(weightText, { weightText = it }, Res.string.body_weight.str(), suffix = Res.string.unit_kg.str())
    SegmentedChoice(
        options = WaterActivity.entries,
        selected = activity,
        onSelect = { activity = it },
        label = {
            when (it) {
                WaterActivity.LOW -> Res.string.low_activity.str()
                WaterActivity.MODERATE -> Res.string.water_moderate.str()
                WaterActivity.HIGH -> Res.string.high.str()
            }
        },
    )
    SegmentedChoice(
        options = Climate.entries,
        selected = climate,
        onSelect = { climate = it },
        label = { if (it == Climate.TEMPERATE) Res.string.temperate_climate.str() else Res.string.hot_climate.str() },
    )
    ChoiceChips(
        options = listOf(200, 250, 300, 500),
        selected = glassMl,
        onSelect = { glassMl = it },
        label = { "${Res.string.glass.str()} $it ${Res.string.ml.str()}" },
    )
    val weight = weightText.toDoubleLenient()
    if (weight == null || weight < 20 || weight > 300) {
        ErrorText(Res.string.water_enter_a_weight_between.str())
        return
    }
    val ml = WaterIntake.dailyMl(weight, activity, climate)
    AnswerCard("$ml ${Res.string.ml.str()}", Res.string.per_day.str(), copyValue = ml.toString())
    ResultCard {
        KeyValueRow(Res.string.litres.str(), (ml / 1000.0).fmt(2))
        KeyValueRow(Res.string.glasses.str(), "${WaterIntake.glasses(ml, glassMl)} × $glassMl ${Res.string.ml.str()}")
        Text(
            text = Res.string.water_estimate_33_ml.str(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
