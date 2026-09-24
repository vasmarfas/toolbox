package com.vasmarfas.card.tools.calculators

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TireRepair
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
import com.vasmarfas.card.ui.components.AnswerCard
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.ToolSection
import com.vasmarfas.card.ui.theme.LocalStatusColors
import kotlin.math.abs
import org.jetbrains.compose.resources.stringResource

val tireSizeTool = Tool(
    id = "tire-size",
    category = ToolCategory.CALCULATORS,
    title = Res.string.tire_size,
    description = Res.string.tire_size_description,
    icon = Icons.Filled.TireRepair,
    keywords = listOf(
        "tire", "tyre", "wheel", "rim", "speedometer", "clearance", "diameter",
        "шины", "шина", "резина", "колёса", "диск", "спидометр", "клиренс", "типоразмер", "шинный калькулятор",
    ),
) { TireSizeScreen() }

private val speedChecks = listOf(60.0, 90.0, 110.0, 130.0)

@Composable
private fun TireSizeScreen() {
    var currentWidth by rememberSaveable { mutableStateOf("205") }
    var currentProfile by rememberSaveable { mutableStateOf("55") }
    var currentRim by rememberSaveable { mutableStateOf("16") }
    var newWidth by rememberSaveable { mutableStateOf("225") }
    var newProfile by rememberSaveable { mutableStateOf("45") }
    var newRim by rememberSaveable { mutableStateOf("17") }
    val current = tireOf(currentWidth, currentProfile, currentRim)
    val other = tireOf(newWidth, newProfile, newRim)
    ToolSection(current?.let { stringResource(Res.string.tires_now_size, it.toString()) } ?: Res.string.tires_now.str()) {
        TireFields(currentWidth, currentProfile, currentRim, { currentWidth = it }, { currentProfile = it }, { currentRim = it })
    }
    ToolSection(other?.let { stringResource(Res.string.tires_new_size, it.toString()) } ?: Res.string.tires_new.str()) {
        TireFields(newWidth, newProfile, newRim, { newWidth = it }, { newProfile = it }, { newRim = it })
    }
    if (current == null || other == null) return
    val kmh = Res.string.unit_kmh.str()
    val mm = Res.string.unit_mm.str()
    val change = Tires.diameterChangePercent(current, other)
    AnswerCard(
        value = "${Tires.actualSpeed(current, other, 100.0).fmt(1)} $kmh",
        caption = Res.string.real_speed_at_100.str(),
    )
    val status = LocalStatusColors.current
    Text(
        when {
            abs(change) < 0.05 -> Res.string.tire_diameters_match.str()
            change > 0 -> stringResource(Res.string.tire_diameter_bigger, "${abs(change).fmt(1)}%", "${Tires.USUAL_TOLERANCE_PERCENT.fmt(0)}%")
            else -> stringResource(Res.string.tire_diameter_smaller, "${abs(change).fmt(1)}%", "${Tires.USUAL_TOLERANCE_PERCENT.fmt(0)}%")
        },
        style = MaterialTheme.typography.bodyMedium,
        color = if (abs(change) <= Tires.USUAL_TOLERANCE_PERCENT) status.good else status.warn,
    )
    ResultCard {
        SimpleTable(
            header = listOf("", Res.string.tires_now.str(), Res.string.tires_new.str(), Res.string.difference.str()),
            rows = listOf(
                row(Res.string.overall_diameter.str() + ", $mm", current.diameterMm, other.diameterMm, 1),
                row(Res.string.sidewall_height.str() + ", $mm", current.sidewallMm, other.sidewallMm, 1),
                row(Res.string.circumference.str() + ", $mm", current.circumferenceMm, other.circumferenceMm, 0),
                row(Res.string.revolutions_per_km.str(), current.revolutionsPerKm, other.revolutionsPerKm, 0),
            ),
            weights = listOf(1.6f, 1f, 1f, 1f),
        )
        KeyValueRow(Res.string.ground_clearance.str(), signed(Tires.clearanceChangeMm(current, other), 1) + " $mm", copyable = false)
    }
    ResultCard(Res.string.speedometer.str()) {
        SimpleTable(
            header = listOf(Res.string.speedometer_shows.str(), Res.string.actual_speed.str()),
            rows = speedChecks.map { listOf("${it.fmt(0)} $kmh", "${Tires.actualSpeed(current, other, it).fmt(1)} $kmh") },
        )
    }
}

private fun tireOf(width: String, profile: String, rim: String): TireSize? {
    val w = width.toDoubleLenient()?.takeIf { it > 0 } ?: return null
    val p = profile.toDoubleLenient()?.takeIf { it > 0 } ?: return null
    val r = rim.toDoubleLenient()?.takeIf { it > 0 } ?: return null
    return TireSize(w, p, r)
}

private fun signed(value: Double, digits: Int): String = (if (value > 0) "+" else "") + value.fmt(digits)

private fun row(label: String, current: Double, other: Double, digits: Int): List<String> =
    listOf(label, current.fmt(digits), other.fmt(digits), signed(other - current, digits))

@Composable
private fun TireFields(
    width: String,
    profile: String,
    rim: String,
    onWidth: (String) -> Unit,
    onProfile: (String) -> Unit,
    onRim: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField(width, onWidth, Res.string.width.str(), Modifier.weight(1f), suffix = Res.string.unit_mm.str())
        NumberField(profile, onProfile, Res.string.tire_profile.str(), Modifier.weight(1f), suffix = "%")
        NumberField(rim, onRim, Res.string.rim.str(), Modifier.weight(1f), suffix = "″")
    }
}
