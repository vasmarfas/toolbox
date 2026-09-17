package com.vasmarfas.card.tools.fitness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
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
import com.vasmarfas.card.tools.calculators.Sex
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SegmentedChoice

val vo2MaxTool = Tool(
    id = "vo2-max",
    category = ToolCategory.FITNESS,
    title = Res.string.vo2_max,
    description = Res.string.vo2_max_description,
    icon = Icons.Filled.DirectionsRun,
    keywords = listOf(
        "vo2max", "cooper", "rockport", "aerobic", "endurance", "mets",
        "мпк", "выносливость", "купер", "аэробный", "метаболический эквивалент",
    ),
) { Vo2MaxScreen() }

private enum class Vo2Method { COOPER, ROCKPORT, RESTING_HR }

@Composable
private fun Vo2MaxScreen() {
    var method by rememberSaveable { mutableStateOf(Vo2Method.COOPER) }

    SegmentedChoice(
        options = Vo2Method.entries,
        selected = method,
        onSelect = { method = it },
        label = {
            when (it) {
                Vo2Method.COOPER -> Res.string.vo2_cooper.str()
                Vo2Method.ROCKPORT -> Res.string.vo2_rockport.str()
                Vo2Method.RESTING_HR -> Res.string.vo2_resting_hr.str()
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )

    when (method) {
        Vo2Method.COOPER -> CooperInputs()
        Vo2Method.ROCKPORT -> RockportInputs()
        Vo2Method.RESTING_HR -> RestingHeartRateInputs()
    }
}

@Composable
private fun Vo2Result(value: Double) {
    if (!value.isFinite() || value <= 0) {
        ErrorText(Res.string.vo2_out_of_range.str())
        return
    }
    ResultCard {
        KeyValueRow(Res.string.vo2_max.str(), "${value.fmt(1)} ${Res.string.unit_ml_kg_min.str()}", copyable = false)
        KeyValueRow(Res.string.mets.str(), Vo2Max.mets(value).fmt(1), copyable = false)
    }
    Text(
        Res.string.vo2_note.str(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun CooperInputs() {
    var metresText by rememberSaveable { mutableStateOf("2400") }
    NumberField(
        value = metresText,
        onValueChange = { metresText = it },
        label = Res.string.distance_in_12_minutes.str(),
        suffix = Res.string.m.str(),
        modifier = Modifier.fillMaxWidth(),
        isError = metresText.toDoubleLenient().let { it == null || it <= 0 },
    )
    val metres = metresText.toDoubleLenient()
    if (metres == null || metres <= 0) {
        ErrorText(Res.string.enter_a_positive_number.str())
        return
    }
    Vo2Result(Vo2Max.cooper(metres))
}

@Composable
private fun RockportInputs() {
    var massText by rememberSaveable { mutableStateOf("75") }
    var ageText by rememberSaveable { mutableStateOf("30") }
    var minutesText by rememberSaveable { mutableStateOf("13") }
    var heartRateText by rememberSaveable { mutableStateOf("120") }
    var sex by rememberSaveable { mutableStateOf(Sex.MALE) }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        NumberField(
            value = massText,
            onValueChange = { massText = it },
            label = Res.string.weight.str(),
            modifier = Modifier.weight(1f),
            suffix = Res.string.kg.str(),
            isError = massText.toDoubleLenient().let { it == null || it <= 0 },
        )
        NumberField(
            value = ageText,
            onValueChange = { ageText = it },
            label = Res.string.age.str(),
            modifier = Modifier.weight(1f),
            isError = ageText.trim().toIntOrNull().let { it == null || it !in 10..100 },
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        NumberField(
            value = minutesText,
            onValueChange = { minutesText = it },
            label = Res.string.mile_walk_time.str(),
            modifier = Modifier.weight(1f),
            suffix = Res.string.min.str(),
            isError = minutesText.toDoubleLenient().let { it == null || it <= 0 },
        )
        NumberField(
            value = heartRateText,
            onValueChange = { heartRateText = it },
            label = Res.string.heart_rate_at_finish.str(),
            modifier = Modifier.weight(1f),
            suffix = Res.string.bpm.str(),
            isError = heartRateText.trim().toIntOrNull().let { it == null || it !in 40..240 },
        )
    }
    SegmentedChoice(
        options = Sex.entries,
        selected = sex,
        onSelect = { sex = it },
        label = { if (it == Sex.MALE) Res.string.male.str() else Res.string.female.str() },
        modifier = Modifier.fillMaxWidth(),
    )

    val mass = massText.toDoubleLenient()
    val age = ageText.trim().toIntOrNull()
    val minutes = minutesText.toDoubleLenient()
    val heartRate = heartRateText.trim().toIntOrNull()
    if (mass == null || mass <= 0 || age == null || age !in 10..100 ||
        minutes == null || minutes <= 0 || heartRate == null || heartRate !in 40..240
    ) {
        ErrorText(Res.string.vo2_input_error.str())
        return
    }
    Vo2Result(Vo2Max.rockport(mass, age, sex, minutes, heartRate))
}

@Composable
private fun RestingHeartRateInputs() {
    var maxText by rememberSaveable { mutableStateOf("190") }
    var restingText by rememberSaveable { mutableStateOf("60") }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        NumberField(
            value = maxText,
            onValueChange = { maxText = it },
            label = Res.string.max_heart_rate.str(),
            modifier = Modifier.weight(1f),
            suffix = Res.string.bpm.str(),
            isError = maxText.trim().toIntOrNull().let { it == null || it !in 100..230 },
        )
        NumberField(
            value = restingText,
            onValueChange = { restingText = it },
            label = Res.string.resting_heart_rate.str(),
            modifier = Modifier.weight(1f),
            suffix = Res.string.bpm.str(),
            isError = restingText.trim().toIntOrNull().let { it == null || it !in 30..120 },
        )
    }
    val max = maxText.trim().toIntOrNull()
    val resting = restingText.trim().toIntOrNull()
    if (max == null || max !in 100..230 || resting == null || resting !in 30..120 || resting >= max) {
        ErrorText(Res.string.vo2_input_error.str())
        return
    }
    Vo2Result(Vo2Max.heartRateRatio(max, resting))
}
