package com.vasmarfas.card.tools.everyday

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
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SegmentedChoice

val waterIntakeTool = Tool(
    id = "water-intake",
    category = ToolCategory.EVERYDAY,
    title = Res.string.water_intake,
    description = Res.string.daily_water_estimate_from_body_weight_activi,
    icon = Icons.Filled.WaterDrop,
    keywords = listOf("water", "hydration", "glasses", "health", "вода", "норма", "стаканы", "здоровье"),
) { WaterIntakeScreen() }

@Composable
private fun WaterIntakeScreen() {
    var weightText by rememberSaveable { mutableStateOf("70") }
    var activity by rememberSaveable { mutableStateOf(Activity.LOW) }
    var climate by rememberSaveable { mutableStateOf(Climate.TEMPERATE) }
    var glassMl by rememberSaveable { mutableStateOf(250) }
    NumberField(weightText, { weightText = it }, Res.string.body_weight.str(), suffix = "kg")
    SegmentedChoice(
        options = Activity.entries,
        selected = activity,
        onSelect = { activity = it },
        label = {
            when (it) {
                Activity.LOW -> Res.string.low_activity.str()
                Activity.MODERATE -> Res.string.moderate_2.str()
                Activity.HIGH -> Res.string.high.str()
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
        label = { "${Res.string.glass.str()} $it ml" },
    )
    val weight = weightText.toDoubleLenient()
    if (weight == null || weight < 20 || weight > 300) {
        ErrorText(Res.string.enter_a_weight_between_20_and_300_kg.str())
        return
    }
    val ml = WaterIntake.dailyMl(weight, activity, climate)
    ResultCard {
        KeyValueRow(Res.string.per_day.str(), "$ml ml")
        KeyValueRow(Res.string.litres.str(), (ml / 1000.0).fmt(2))
        KeyValueRow(Res.string.glasses.str(), "${WaterIntake.glasses(ml, glassMl)} × $glassMl ml")
        Text(
            text = Res.string.estimate_33_ml_per_kg_plus_350_700_ml_for_ex.str(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
