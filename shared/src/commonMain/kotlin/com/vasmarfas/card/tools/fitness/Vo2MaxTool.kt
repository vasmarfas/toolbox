package com.vasmarfas.card.tools.fitness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
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
import com.vasmarfas.card.ui.components.Hint
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SegmentedChoice
import org.jetbrains.compose.resources.StringResource

val vo2MaxTool = Tool(
    id = "vo2-max",
    category = ToolCategory.FITNESS,
    title = Res.string.vo2_max,
    description = Res.string.vo2_max_description,
    icon = Icons.AutoMirrored.Filled.DirectionsRun,
    keywords = listOf(
        "vo2max", "cooper", "rockport", "aerobic", "endurance", "mets",
        "мпк", "выносливость", "купер", "аэробный", "метаболический эквивалент",
    ),
) { Vo2MaxScreen() }

private enum class Vo2Method(val title: StringResource, val how: StringResource) {
    COOPER(Res.string.vo2_cooper, Res.string.vo2_cooper_how),
    ROCKPORT(Res.string.vo2_rockport, Res.string.vo2_rockport_how),
    RESTING_HR(Res.string.vo2_resting_hr, Res.string.vo2_resting_hr_how),
}

private val levels = listOf(
    Res.string.vo2_level_very_poor,
    Res.string.vo2_level_poor,
    Res.string.vo2_level_fair,
    Res.string.vo2_level_good,
    Res.string.vo2_level_excellent,
    Res.string.vo2_level_superior,
)

@Composable
private fun Vo2MaxScreen() {
    var method by rememberSaveable { mutableStateOf(Vo2Method.COOPER) }
    var sex by rememberSaveable { mutableStateOf(Sex.MALE) }
    var ageText by rememberSaveable { mutableStateOf("30") }

    Hint(Res.string.vo2_intro.str())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        SegmentedChoice(
            options = Sex.entries,
            selected = sex,
            onSelect = { sex = it },
            label = { if (it == Sex.MALE) Res.string.male.str() else Res.string.female.str() },
            modifier = Modifier.weight(1.5f),
        )
        NumberField(
            value = ageText,
            onValueChange = { ageText = it },
            label = Res.string.age.str(),
            modifier = Modifier.weight(1f),
            isError = ageText.trim().toIntOrNull().let { it == null || it !in 10..100 },
        )
    }
    SegmentedChoice(
        options = Vo2Method.entries,
        selected = method,
        onSelect = { method = it },
        label = { it.title.str() },
        modifier = Modifier.fillMaxWidth(),
    )
    Hint(method.how.str())

    val age = ageText.trim().toIntOrNull()?.takeIf { it in 10..100 }
    val value = when (method) {
        Vo2Method.COOPER -> cooper()
        Vo2Method.ROCKPORT -> rockport(sex, age)
        Vo2Method.RESTING_HR -> byHeartRate(age)
    }
    when {
        age == null -> ErrorText(Res.string.vo2_input_error.str())
        value != null -> Vo2Result(value, sex, age)
    }
}

@Composable
private fun Vo2Result(value: Double, sex: Sex, age: Int) {
    if (!value.isFinite() || value <= 0) {
        ErrorText(Res.string.vo2_out_of_range.str())
        return
    }
    val norms = Vo2Max.norms(sex, age)
    ResultCard {
        KeyValueRow(Res.string.vo2_max.str(), "${value.fmt(1)} ${Res.string.unit_ml_kg_min.str()}", copyable = false)
        KeyValueRow(Res.string.vo2_level.str(), levels[Vo2Max.rating(value, sex, age)].str(), mono = false, copyable = false)
        KeyValueRow(Res.string.vo2_good_from.str(), "${norms[2].fmt(1)} ${Res.string.unit_ml_kg_min.str()}", copyable = false)
        KeyValueRow(Res.string.mets.str(), Vo2Max.mets(value).fmt(1), copyable = false)
        Hint(Res.string.vo2_mets_hint.str())
    }
    Hint(Res.string.vo2_note.str())
}

@Composable
private fun cooper(): Double? {
    var metresText by rememberSaveable { mutableStateOf("2400") }
    NumberField(
        value = metresText,
        onValueChange = { metresText = it },
        label = Res.string.distance_in_12_minutes.str(),
        suffix = Res.string.unit_m.str(),
        modifier = Modifier.fillMaxWidth(),
        isError = metresText.toDoubleLenient().let { it == null || it <= 0 },
    )
    val metres = metresText.toDoubleLenient()
    if (metres == null || metres <= 0) {
        ErrorText(Res.string.enter_a_positive_number.str())
        return null
    }
    return Vo2Max.cooper(metres)
}

@Composable
private fun rockport(sex: Sex, age: Int?): Double? {
    var massText by rememberSaveable { mutableStateOf("75") }
    var minutesText by rememberSaveable { mutableStateOf("13") }
    var heartRateText by rememberSaveable { mutableStateOf("120") }
    NumberField(
        value = massText,
        onValueChange = { massText = it },
        label = Res.string.weight.str(),
        suffix = Res.string.unit_kg.str(),
        isError = massText.toDoubleLenient().let { it == null || it <= 0 },
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        NumberField(
            value = minutesText,
            onValueChange = { minutesText = it },
            label = Res.string.mile_walk_time.str(),
            modifier = Modifier.weight(1f),
            suffix = Res.string.unit_min.str(),
            isError = minutesText.toDoubleLenient().let { it == null || it <= 0 },
        )
        NumberField(
            value = heartRateText,
            onValueChange = { heartRateText = it },
            label = Res.string.heart_rate_at_finish.str(),
            modifier = Modifier.weight(1f),
            suffix = Res.string.unit_bpm.str(),
            isError = heartRateText.trim().toIntOrNull().let { it == null || it !in 40..240 },
        )
    }
    val mass = massText.toDoubleLenient()
    val minutes = minutesText.toDoubleLenient()
    val heartRate = heartRateText.trim().toIntOrNull()
    if (mass == null || mass <= 0 || minutes == null || minutes <= 0 || heartRate == null || heartRate !in 40..240) {
        ErrorText(Res.string.vo2_input_error.str())
        return null
    }
    return age?.let { Vo2Max.rockport(mass, it, sex, minutes, heartRate) }
}

@Composable
private fun byHeartRate(age: Int?): Double? {
    var maxText by rememberSaveable { mutableStateOf("") }
    var restingText by rememberSaveable { mutableStateOf("60") }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        NumberField(
            value = maxText,
            onValueChange = { maxText = it },
            label = Res.string.max_heart_rate.str(),
            modifier = Modifier.weight(1f),
            suffix = Res.string.unit_bpm.str(),
            supportingText = Res.string.vo2_max_hr_blank.str(),
            isError = maxText.isNotBlank() && maxText.trim().toIntOrNull().let { it == null || it !in 100..230 },
        )
        NumberField(
            value = restingText,
            onValueChange = { restingText = it },
            label = Res.string.resting_heart_rate.str(),
            modifier = Modifier.weight(1f),
            suffix = Res.string.unit_bpm.str(),
            isError = restingText.trim().toIntOrNull().let { it == null || it !in 30..120 },
        )
    }
    val max = if (maxText.isBlank()) age?.let { 220 - it } else maxText.trim().toIntOrNull()
    val resting = restingText.trim().toIntOrNull()
    if (max == null && age == null) return null
    if (max == null || max !in 100..230 || resting == null || resting !in 30..120 || resting >= max) {
        ErrorText(Res.string.vo2_input_error.str())
        return null
    }
    return Vo2Max.heartRateRatio(max, resting)
}
