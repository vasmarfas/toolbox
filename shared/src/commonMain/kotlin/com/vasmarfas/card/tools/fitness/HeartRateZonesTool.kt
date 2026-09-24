package com.vasmarfas.card.tools.fitness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonitorHeart
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
import com.vasmarfas.card.ui.components.SwitchRow
import kotlin.math.roundToInt

val heartRateZonesTool = Tool(
    id = "heart-rate-zones",
    category = ToolCategory.FITNESS,
    title = Res.string.heart_rate_zones,
    description = Res.string.heart_rate_zones_description,
    icon = Icons.Filled.MonitorHeart,
    keywords = listOf("heart rate", "zones", "karvonen", "tanaka", "hrmax", "resting", "пульс", "зоны", "карвонен", "покой", "чсс"),
) { HeartRateZonesScreen() }

@Composable
private fun HeartRateZonesScreen() {
    var ageText by rememberSaveable { mutableStateOf("30") }
    var restingText by rememberSaveable { mutableStateOf("60") }
    var formula by rememberSaveable { mutableStateOf(MaxHrFormula.TANAKA) }
    var useMeasured by rememberSaveable { mutableStateOf(false) }
    var measuredText by rememberSaveable { mutableStateOf("190") }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        NumberField(
            value = ageText,
            onValueChange = { ageText = it },
            label = Res.string.age.str(),
            modifier = Modifier.weight(1f),
            isError = ageText.trim().toIntOrNull().let { it == null || it !in 5..100 },
        )
        NumberField(
            value = restingText,
            onValueChange = { restingText = it },
            label = Res.string.resting_hr.str(),
            modifier = Modifier.weight(1f),
            suffix = Res.string.unit_bpm.str(),
            isError = restingText.toDoubleLenient().let { it == null || it < 25 || it > 120 },
        )
    }
    DropdownChoice(
        options = MaxHrFormula.entries,
        selected = formula,
        onSelect = { formula = it },
        label = Res.string.max_hr_formula.str(),
        text = { it.title.str() },
    )
    SwitchRow(
        Res.string.measured_max_hr.str(),
        useMeasured,
        { useMeasured = it },
        description = Res.string.from_a_ramp_test_or_a_race_finish.str(),
    )
    if (useMeasured) {
        NumberField(
            value = measuredText,
            onValueChange = { measuredText = it },
            label = Res.string.max_hr.str(),
            suffix = Res.string.unit_bpm.str(),
            isError = measuredText.toDoubleLenient().let { it == null || it < 100 || it > 230 },
        )
    }

    val age = ageText.trim().toIntOrNull()
    val resting = restingText.toDoubleLenient()
    val measured = measuredText.toDoubleLenient()
    if (age == null || age !in 5..100 || resting == null || resting < 25 || resting > 120 ||
        (useMeasured && (measured == null || measured < 100 || measured > 230))
    ) {
        ErrorText(Res.string.hr_age_5_100_resting.str())
        return
    }
    val maxHr = if (useMeasured) measured!! else HeartRate.maxHr(formula, age)
    if (maxHr <= resting) {
        ErrorText(Res.string.hr_max_hr.str())
        return
    }

    ResultCard {
        KeyValueRow(Res.string.max_hr.str(), "${maxHr.roundToInt()} ${Res.string.unit_bpm.str()}")
        KeyValueRow(Res.string.heart_rate_reserve.str(), "${HeartRate.reserve(maxHr, resting).roundToInt()} ${Res.string.unit_bpm.str()}")
        MaxHrFormula.entries.forEach {
            KeyValueRow(it.title.str(), "${HeartRate.maxHr(it, age).fmt(0)} ${Res.string.unit_bpm.str()}", mono = false, copyable = false)
        }
    }
    ResultCard(Res.string.zones.str()) {
        SimpleTable(
            header = listOf(
                Res.string.zone.str(),
                "%",
                "%HRmax",
                Res.string.karvonen.str(),
                Res.string.trains.str(),
            ),
            rows = hrZones.map { zone ->
                listOf(
                    "${zone.index} ${zone.title.str()}",
                    "${zone.lowPercent}–${zone.highPercent}",
                    "${HeartRate.byMaxPercent(maxHr, zone.lowPercent).roundToInt()}–${HeartRate.byMaxPercent(maxHr, zone.highPercent).roundToInt()}",
                    "${HeartRate.karvonen(maxHr, resting, zone.lowPercent).roundToInt()}–${HeartRate.karvonen(maxHr, resting, zone.highPercent).roundToInt()}",
                    zone.trains.str(),
                )
            },
            weights = listOf(1.5f, 0.8f, 1f, 1f, 3f),
            mono = false,
        )
    }
}
