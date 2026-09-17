package com.vasmarfas.card.tools.fitness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonitorHeart
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
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ResultCard
import org.jetbrains.compose.resources.StringResource

val bloodPressureTool = Tool(
    id = "blood-pressure",
    category = ToolCategory.FITNESS,
    title = Res.string.blood_pressure,
    description = Res.string.blood_pressure_description,
    icon = Icons.Filled.MonitorHeart,
    keywords = listOf(
        "blood pressure", "hypertension", "systolic", "diastolic", "map", "pulse pressure",
        "давление", "гипертония", "систолическое", "диастолическое", "пульсовое", "тонометр",
    ),
) { BloodPressureScreen() }

private fun ahaLabel(category: BpCategoryAha): StringResource = when (category) {
    BpCategoryAha.NORMAL -> Res.string.bp_normal
    BpCategoryAha.ELEVATED -> Res.string.bp_elevated
    BpCategoryAha.STAGE_1 -> Res.string.bp_stage_1
    BpCategoryAha.STAGE_2 -> Res.string.bp_stage_2
    BpCategoryAha.CRISIS -> Res.string.bp_crisis
}

private fun escLabel(category: BpCategoryEsc): StringResource = when (category) {
    BpCategoryEsc.OPTIMAL -> Res.string.bp_optimal
    BpCategoryEsc.NORMAL -> Res.string.bp_normal
    BpCategoryEsc.HIGH_NORMAL -> Res.string.bp_high_normal
    BpCategoryEsc.GRADE_1 -> Res.string.bp_grade_1
    BpCategoryEsc.GRADE_2 -> Res.string.bp_grade_2
    BpCategoryEsc.GRADE_3 -> Res.string.bp_grade_3
    BpCategoryEsc.ISOLATED_SYSTOLIC -> Res.string.bp_isolated_systolic
}

@Composable
private fun BloodPressureScreen() {
    var systolicText by rememberSaveable { mutableStateOf("120") }
    var diastolicText by rememberSaveable { mutableStateOf("80") }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        NumberField(
            value = systolicText,
            onValueChange = { systolicText = it },
            label = Res.string.systolic.str(),
            modifier = Modifier.weight(1f),
            suffix = Res.string.mm_hg.str(),
            isError = systolicText.trim().toIntOrNull().let { it == null || it !in 50..300 },
        )
        NumberField(
            value = diastolicText,
            onValueChange = { diastolicText = it },
            label = Res.string.diastolic.str(),
            modifier = Modifier.weight(1f),
            suffix = Res.string.mm_hg.str(),
            isError = diastolicText.trim().toIntOrNull().let { it == null || it !in 30..200 },
        )
    }

    val systolic = systolicText.trim().toIntOrNull()
    val diastolic = diastolicText.trim().toIntOrNull()
    if (systolic == null || systolic !in 50..300 || diastolic == null || diastolic !in 30..200) {
        ErrorText(Res.string.bp_input_range.str())
        return
    }
    if (diastolic >= systolic) {
        ErrorText(Res.string.bp_diastolic_below_systolic.str())
        return
    }

    ResultCard(title = Res.string.bp_aha.str()) {
        KeyValueRow(Res.string.category.str(), ahaLabel(BloodPressure.aha(systolic, diastolic)).str(), copyable = false)
    }
    ResultCard(title = Res.string.bp_esc.str()) {
        KeyValueRow(Res.string.category.str(), escLabel(BloodPressure.esc(systolic, diastolic)).str(), copyable = false)
    }
    ResultCard {
        KeyValueRow(
            Res.string.pulse_pressure.str(),
            "${BloodPressure.pulsePressure(systolic, diastolic)} ${Res.string.mm_hg.str()}",
            copyable = false,
        )
        KeyValueRow(
            Res.string.mean_arterial_pressure.str(),
            "${BloodPressure.meanArterial(systolic, diastolic).fmt(1)} ${Res.string.mm_hg.str()}",
            copyable = false,
        )
    }
    Text(
        Res.string.bp_measurement_note.str(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
