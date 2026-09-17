package com.vasmarfas.card.tools.everyday

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import com.vasmarfas.card.core.fmt
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.tools.time.pad2
import com.vasmarfas.card.tools.time.parseHhMm
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SegmentedChoice
import com.vasmarfas.card.ui.components.ToolInputField
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private enum class SleepMode { WAKE_AT, BED_AT }

private fun nowHhMm(): String {
    val t = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return "${t.hour.pad2()}:${t.minute.pad2()}"
}

val sleepCalculatorTool = Tool(
    id = "sleep-calculator",
    category = ToolCategory.EVERYDAY,
    title = Res.string.sleep_calculator,
    description = Res.string.when_to_go_to_bed_for_a_given_wake_up_time_o,
    icon = Icons.Filled.Bedtime,
    keywords = listOf("sleep", "bedtime", "wake up", "cycles", "сон", "лечь спать", "проснуться", "циклы сна"),
) { SleepCalculatorScreen() }

@Composable
private fun SleepCalculatorScreen() {
    var mode by rememberSaveable { mutableStateOf(SleepMode.WAKE_AT) }
    var wakeText by rememberSaveable { mutableStateOf("07:00") }
    var bedText by rememberSaveable { mutableStateOf(nowHhMm()) }
    SegmentedChoice(
        options = SleepMode.entries,
        selected = mode,
        onSelect = { mode = it },
        label = { if (it == SleepMode.WAKE_AT) Res.string.wake_up_at.str() else Res.string.go_to_bed_at.str() },
    )
    val text = if (mode == SleepMode.WAKE_AT) wakeText else bedText
    ToolInputField(
        value = text,
        onValueChange = { if (mode == SleepMode.WAKE_AT) wakeText = it else bedText = it },
        label = (if (mode == SleepMode.WAKE_AT) Res.string.wake_up_time else Res.string.bedtime).str(),
        placeholder = "HH:MM",
        isError = parseHhMm(text) == null,
        trailingIcon = {
            IconButton(onClick = { if (mode == SleepMode.WAKE_AT) wakeText = nowHhMm() else bedText = nowHhMm() }) {
                Icon(Icons.Filled.Schedule, contentDescription = Res.string.now.str())
            }
        },
        monospace = true,
    )
    val minutes = parseHhMm(text)
    if (minutes == null) {
        ErrorText(Res.string.enter_time_as_hh_mm.str())
        return
    }
    val options = if (mode == SleepMode.WAKE_AT) SleepMath.bedtimes(minutes) else SleepMath.wakeTimes(minutes)
    ResultCard((if (mode == SleepMode.WAKE_AT) Res.string.go_to_bed_at_2 else Res.string.wake_up_at_2).str()) {
        options.forEach { option ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = option.time,
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace),
                    color = if (option.cycles >= 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${option.cycles} ${Res.string.cycles.str()} · ${option.hours.fmt(1)} ${Res.string.h_of_sleep.str()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = Res.string.a_cycle_lasts_about_90_minutes_falling_aslee.str(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
