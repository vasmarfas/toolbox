package com.vasmarfas.card.tools.fitness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
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
import com.vasmarfas.card.ui.components.SegmentedChoice

val waterAndMacrosTool = Tool(
    id = "water-and-macros",
    category = ToolCategory.FITNESS,
    title = Res.string.macros_and_meals,
    description = Res.string.calorie_target_from_your_tdee_and_goal_prote,
    icon = Icons.Filled.Restaurant,
    keywords = listOf("macros", "protein", "carbs", "fat", "cutting", "bulking", "meals", "макросы", "белок", "углеводы", "жиры", "сушка", "набор"),
) { WaterAndMacrosScreen() }

@Composable
private fun WaterAndMacrosScreen() {
    var tdeeText by rememberSaveable { mutableStateOf("2600") }
    var goal by rememberSaveable { mutableStateOf(NutritionGoal.MAINTAIN) }
    var percentText by rememberSaveable { mutableStateOf("0") }
    var weightText by rememberSaveable { mutableStateOf("78") }
    var proteinText by rememberSaveable { mutableStateOf("1.8") }
    var fatText by rememberSaveable { mutableStateOf("0.9") }
    var meals by rememberSaveable { mutableStateOf(4) }

    NumberField(
        value = tdeeText,
        onValueChange = { tdeeText = it },
        label = Res.string.daily_calories_tdee.str(),
        suffix = Res.string.kcal.str(),
        isError = tdeeText.toDoubleLenient().let { it == null || it <= 0 },
        supportingText = Res.string.take_it_from_the_bmi_body_tool.str(),
    )
    SegmentedChoice(
        options = NutritionGoal.entries,
        selected = goal,
        onSelect = {
            goal = it
            percentText = it.defaultPercent.fmt(0)
        },
        label = { it.title.str() },
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        NumberField(
            value = percentText,
            onValueChange = { percentText = it },
            label = Res.string.calorie_shift.str(),
            modifier = Modifier.weight(1f),
            suffix = "%",
            isError = percentText.toDoubleLenient().let { it == null || it <= -60 || it >= 60 },
        )
        NumberField(
            value = weightText,
            onValueChange = { weightText = it },
            label = Res.string.weight.str(),
            modifier = Modifier.weight(1f),
            suffix = Res.string.kg.str(),
            isError = weightText.toDoubleLenient().let { it == null || it <= 0 },
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        NumberField(
            value = proteinText,
            onValueChange = { proteinText = it },
            label = Res.string.protein_per_kg.str(),
            modifier = Modifier.weight(1f),
            suffix = Res.string.g.str(),
            isError = proteinText.toDoubleLenient().let { it == null || it < 0 || it > 5 },
        )
        NumberField(
            value = fatText,
            onValueChange = { fatText = it },
            label = Res.string.fat_per_kg.str(),
            modifier = Modifier.weight(1f),
            suffix = Res.string.g.str(),
            isError = fatText.toDoubleLenient().let { it == null || it < 0 || it > 4 },
        )
    }
    ChoiceChips(
        options = listOf(3, 4, 5, 6),
        selected = meals,
        onSelect = { meals = it },
        label = { "$it ${Res.string.meals.str()}" },
    )

    val tdee = tdeeText.toDoubleLenient()
    val percent = percentText.toDoubleLenient()
    val weight = weightText.toDoubleLenient()
    val protein = proteinText.toDoubleLenient()
    val fat = fatText.toDoubleLenient()
    if (tdee == null || tdee <= 0 || percent == null || percent <= -60 || percent >= 60 ||
        weight == null || weight <= 0 || protein == null || protein < 0 || protein > 5 ||
        fat == null || fat < 0 || fat > 4
    ) {
        ErrorText(
            Res.string.check_the_values_shift_within_60_protein_up.str(),
        )
        return
    }

    val split = Macros.split(tdee, percent, weight, protein, fat)
    if (split.carbsKcal < 0) {
        ErrorText(
            Res.string.protein_and_fat_alone_exceed_the_calorie_tar.str(),
        )
        return
    }
    ResultCard(Res.string.daily_target.str()) {
        KeyValueRow(Res.string.calories.str(), "${split.targetKcal.fmt(0, grouping = true)} ${Res.string.kcal.str()}")
        KeyValueRow(Res.string.difference_from_tdee.str(), "${(split.targetKcal - tdee).fmt(0)} ${Res.string.kcal.str()}")
        SimpleTable(
            header = listOf(
                Res.string.macro.str(),
                Res.string.g.str(),
                Res.string.kcal.str(),
                "%",
            ),
            rows = listOf(
                listOf(Res.string.protein.str(), split.proteinG.fmt(0), split.proteinKcal.fmt(0), (split.proteinKcal / split.targetKcal * 100).fmt(0)),
                listOf(Res.string.fat.str(), split.fatG.fmt(0), split.fatKcal.fmt(0), (split.fatKcal / split.targetKcal * 100).fmt(0)),
                listOf(Res.string.carbs.str(), split.carbsG.fmt(0), split.carbsKcal.fmt(0), (split.carbsKcal / split.targetKcal * 100).fmt(0)),
            ),
            weights = listOf(1.5f, 1f, 1f, 0.8f),
            mono = false,
        )
    }
    ResultCard(Res.string.per_meal.str()) {
        SimpleTable(
            header = listOf(
                "#",
                Res.string.kcal.str(),
                "${Res.string.protein.str()}, ${Res.string.g.str()}",
                "${Res.string.fat.str()}, ${Res.string.g.str()}",
                "${Res.string.carbs.str()}, ${Res.string.g.str()}",
            ),
            rows = Macros.mealShares(meals).mapIndexed { index, share ->
                listOf(
                    (index + 1).toString(),
                    (split.targetKcal * share).fmt(0),
                    (split.proteinG * share).fmt(0),
                    (split.fatG * share).fmt(0),
                    (split.carbsG * share).fmt(0),
                )
            },
            weights = listOf(0.5f, 1f, 1f, 1f, 1.2f),
        )
    }
    Text(
        text = Res.string.water_is_not_part_of_this_the_water_intake_t.str(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
