package com.vasmarfas.card.tools.time

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.currentEpochMillis
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.expandedTextStyle
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

val stopwatchTool = Tool(
    id = "stopwatch",
    category = ToolCategory.TIME,
    title = Res.string.stopwatch,
    description = Res.string.millisecond_stopwatch_with_laps_lap_time_and,
    icon = Icons.Filled.Timer,
    keywords = listOf("stopwatch", "lap", "timer", "секундомер", "круг", "отсечка"),
    expandable = true,
) { StopwatchScreen() }

@Composable
private fun StopwatchScreen() {
    var running by rememberSaveable { mutableStateOf(false) }
    var startedAt by rememberSaveable { mutableStateOf(0L) }
    var accumulated by rememberSaveable { mutableStateOf(0L) }
    var now by remember { mutableStateOf(currentEpochMillis()) }
    val laps = remember { mutableStateListOf<Long>() }
    LaunchedEffect(running) {
        while (running) {
            delay(30.milliseconds)
            now = currentEpochMillis()
        }
    }
    val elapsed = if (running) accumulated + (now - startedAt) else accumulated
    Text(
        text = formatStopwatch(elapsed),
        style = expandedTextStyle(MaterialTheme.typography.displayLarge.copy(fontFamily = FontFamily.Monospace)),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        if (running) {
            ActionButton(
                text = Res.string.stop_3.str(),
                onClick = {
                    accumulated += currentEpochMillis() - startedAt
                    running = false
                },
                icon = Icons.Filled.Pause,
                modifier = Modifier.weight(1f),
            )
            ActionButton(
                text = Res.string.lap.str(),
                onClick = { laps.add(accumulated + (currentEpochMillis() - startedAt)) },
                icon = Icons.Filled.Flag,
                modifier = Modifier.weight(1f),
            )
        } else {
            ActionButton(
                text = Res.string.start.str(),
                onClick = {
                    startedAt = currentEpochMillis()
                    now = startedAt
                    running = true
                },
                icon = Icons.Filled.PlayArrow,
                modifier = Modifier.weight(1f),
            )
            ActionButton(
                text = Res.string.reset.str(),
                onClick = {
                    accumulated = 0L
                    laps.clear()
                },
                enabled = accumulated > 0 || laps.isNotEmpty(),
                icon = Icons.Filled.Refresh,
                modifier = Modifier.weight(1f),
            )
        }
    }
    if (laps.isNotEmpty()) {
        val splits = remember(laps.size) { lapSplits(laps.toList()) }
        val fastest = splits.minOf { it.lap }
        val slowest = splits.maxOf { it.lap }
        ResultCard(Res.string.laps.str()) {
            Row(Modifier.fillMaxWidth()) {
                Text("#", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.5f))
                Text(Res.string.lap.str(), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                Text(Res.string.total_2.str(), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                splits.asReversed().forEach { lap ->
                    val color = when {
                        splits.size > 1 && lap.lap == fastest -> MaterialTheme.colorScheme.primary
                        splits.size > 1 && lap.lap == slowest -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(lap.index.toString(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.5f))
                        Text(
                            formatStopwatch(lap.lap),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = color,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            formatStopwatch(lap.total),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
