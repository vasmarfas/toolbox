package com.vasmarfas.card.tools.fitness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
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
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SegmentedChoice
import com.vasmarfas.card.ui.components.ToolInputField

private enum class PaceTarget { TIME, PACE, DISTANCE }

private enum class PaceUnit { KM, MILE }

val paceCalculatorTool = Tool(
    id = "pace-calculator",
    category = ToolCategory.FITNESS,
    title = Res.string.pace_calculator,
    description = Res.string.pace_calculator_description,
    icon = Icons.AutoMirrored.Filled.DirectionsRun,
    keywords = listOf("pace", "running", "splits", "riegel", "marathon", "speed", "темп", "бег", "отрезки", "марафон", "скорость"),
) { PaceCalculatorScreen() }

@Composable
private fun PaceCalculatorScreen() {
    var target by rememberSaveable { mutableStateOf(PaceTarget.PACE) }
    var unit by rememberSaveable { mutableStateOf(PaceUnit.KM) }
    var distanceText by rememberSaveable { mutableStateOf("10") }
    var timeText by rememberSaveable { mutableStateOf("50:00") }
    var paceText by rememberSaveable { mutableStateOf("5:00") }

    val unitKm = if (unit == PaceUnit.KM) 1.0 else KM_PER_MILE
    val unitLabel = if (unit == PaceUnit.KM) Res.string.unit_km.str() else Res.string.unit_mi.str()

    SegmentedChoice(
        options = PaceUnit.entries,
        selected = unit,
        onSelect = { unit = it },
        label = { if (it == PaceUnit.KM) Res.string.kilometres.str() else Res.string.miles.str() },
    )
    SegmentedChoice(
        options = PaceTarget.entries,
        selected = target,
        onSelect = { target = it },
        label = {
            when (it) {
                PaceTarget.TIME -> Res.string.time.str()
                PaceTarget.PACE -> Res.string.pace.str()
                PaceTarget.DISTANCE -> Res.string.pace_distance.str()
            }
        },
    )
    if (target != PaceTarget.DISTANCE) {
        NumberField(
            value = distanceText,
            onValueChange = { distanceText = it },
            label = Res.string.pace_distance.str(),
            suffix = unitLabel,
            isError = distanceText.toDoubleLenient().let { it == null || it <= 0 },
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        if (target != PaceTarget.TIME) {
            ToolInputField(
                value = timeText,
                onValueChange = { timeText = it },
                label = Res.string.time.str(),
                modifier = Modifier.weight(1f),
                placeholder = "1:23:45",
                isError = Pace.parseTime(timeText) == null,
                monospace = true,
            )
        }
        if (target != PaceTarget.PACE) {
            ToolInputField(
                value = paceText,
                onValueChange = { paceText = it },
                label = "${Res.string.pace.str()}, ${Res.string.unit_min.str()}/$unitLabel",
                modifier = Modifier.weight(1f),
                placeholder = "5:00",
                isError = Pace.parseTime(paceText) == null,
                monospace = true,
            )
        }
    }

    val distanceUnits = distanceText.toDoubleLenient()
    val timeSeconds = Pace.parseTime(timeText)
    val paceSeconds = Pace.parseTime(paceText)
    val resolved = when (target) {
        PaceTarget.TIME -> {
            if (distanceUnits == null || distanceUnits <= 0 || paceSeconds == null || paceSeconds <= 0) null
            else Triple(distanceUnits, distanceUnits * paceSeconds, paceSeconds)
        }

        PaceTarget.PACE -> {
            if (distanceUnits == null || distanceUnits <= 0 || timeSeconds == null || timeSeconds <= 0) null
            else Triple(distanceUnits, timeSeconds, Pace.paceSeconds(timeSeconds, distanceUnits))
        }

        PaceTarget.DISTANCE -> {
            if (timeSeconds == null || timeSeconds <= 0 || paceSeconds == null || paceSeconds <= 0) null
            else Triple(timeSeconds / paceSeconds, timeSeconds, paceSeconds)
        }
    }
    if (resolved == null) {
        ErrorText(
            Res.string.pace_enter_a_positive_distance.str(),
        )
        return
    }
    val (units, seconds, pacePerUnit) = resolved
    val km = units * unitKm
    val perKm = pacePerUnit / unitKm
    val speedKmh = Pace.speed(km, seconds)

    ResultCard {
        KeyValueRow(Res.string.pace_distance.str(), "${units.fmt(3)} $unitLabel · ${km.fmt(3)} ${Res.string.unit_km.str()}")
        KeyValueRow(Res.string.time.str(), Pace.formatTime(seconds))
        KeyValueRow(Res.string.pace.str(), "${Pace.formatPace(pacePerUnit)} ${Res.string.unit_min.str()}/$unitLabel")
        KeyValueRow(Res.string.pace_per_km.str(), "${Pace.formatPace(perKm)} ${Res.string.unit_min.str()}/${Res.string.unit_km.str()}")
        KeyValueRow(Res.string.pace_per_mile.str(), "${Pace.formatPace(perKm * KM_PER_MILE)} ${Res.string.unit_min.str()}/${Res.string.unit_mi.str()}")
        KeyValueRow(Res.string.speed.str(), "${speedKmh.fmt(2)} ${Res.string.unit_kmh.str()} · ${(speedKmh / KM_PER_MILE).fmt(2)} ${Res.string.unit_mph.str()}")
    }

    val splits = Pace.splits(km, perKm, unitKm)
    if (splits.isNotEmpty()) {
        ResultCard(Res.string.splits.str()) {
            SimpleTable(
                header = listOf(
                    unitLabel,
                    Res.string.split.str(),
                    Res.string.cumulative.str(),
                ),
                rows = splits.map { (number, split, cumulative) ->
                    listOf(number.toString(), Pace.formatTime(split), Pace.formatTime(cumulative))
                },
                weights = listOf(0.6f, 1f, 1.2f),
            )
        }
    }
    ResultCard(Res.string.race_predictions_riegel.str()) {
        SimpleTable(
            header = listOf(
                Res.string.pace_distance.str(),
                Res.string.time.str(),
                Res.string.pace_per_km.str(),
            ),
            rows = raceDistances.map {
                val predicted = Pace.riegel(seconds, km, it.km)
                listOf(it.title.str(), Pace.formatTime(predicted), Pace.formatPace(predicted / it.km))
            },
            weights = listOf(1.4f, 1f, 1f),
            mono = false,
        )
    }
    Text(
        text = Res.string.pace_on_a_treadmill_1.str(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
