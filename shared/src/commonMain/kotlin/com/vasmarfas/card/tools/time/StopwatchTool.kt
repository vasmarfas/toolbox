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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.Prefs
import com.vasmarfas.card.core.currentEpochMillis
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.CopyIconButton
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.expandedTextStyle
import com.vasmarfas.card.ui.components.monoFamily
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

val stopwatchTool = Tool(
    id = "stopwatch",
    category = ToolCategory.TIME,
    title = Res.string.stopwatch,
    description = Res.string.stopwatch_description,
    icon = Icons.Filled.Timer,
    keywords = listOf("stopwatch", "lap", "timer", "секундомер", "круг", "отсечка"),
    expandable = true,
) { StopwatchScreen() }

@Composable
private fun StopwatchScreen() {
    var state by remember { mutableStateOf(StopwatchState.decode(Prefs.store.get(StopwatchState.PREF_KEY)) ?: StopwatchState()) }
    var now by remember { mutableStateOf(currentEpochMillis()) }
    fun update(next: StopwatchState) {
        state = next
        Prefs.store.put(StopwatchState.PREF_KEY, next.encode())
    }
    LaunchedEffect(state.running) {
        while (state.running) {
            delay(30.milliseconds)
            now = currentEpochMillis()
        }
    }
    val elapsed = state.elapsed(now)
    Text(
        text = formatStopwatch(elapsed),
        style = expandedTextStyle(MaterialTheme.typography.displayLarge.copy(fontFamily = monoFamily())),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    if (state.laps.isNotEmpty()) {
        Text(
            text = Res.string.current_lap.str() + " " + formatStopwatch(elapsed - state.laps.last()),
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = monoFamily()),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        if (state.running) {
            ActionButton(
                text = Res.string.stop_short.str(),
                onClick = { update(state.copy(running = false, accumulated = state.elapsed(currentEpochMillis()))) },
                icon = Icons.Filled.Pause,
                modifier = Modifier.weight(1f),
            )
            ActionButton(
                text = Res.string.lap.str(),
                onClick = { update(state.copy(laps = state.laps + state.elapsed(currentEpochMillis()))) },
                icon = Icons.Filled.Flag,
                modifier = Modifier.weight(1f),
            )
        } else {
            ActionButton(
                text = (if (state.accumulated > 0) Res.string.resume else Res.string.start).str(),
                onClick = {
                    now = currentEpochMillis()
                    update(state.copy(running = true, startedAt = now))
                },
                icon = Icons.Filled.PlayArrow,
                modifier = Modifier.weight(1f),
            )
            ActionButton(
                text = Res.string.reset.str(),
                onClick = { update(StopwatchState()) },
                enabled = state.accumulated > 0 || state.laps.isNotEmpty(),
                icon = Icons.Filled.Refresh,
                modifier = Modifier.weight(1f),
            )
        }
    }
    if (state.laps.isEmpty()) return
    val splits = remember(state.laps) { lapSplits(state.laps) }
    val fastest = splits.minOf { it.lap }
    val slowest = splits.maxOf { it.lap }
    val header = listOf("#", Res.string.lap.str(), Res.string.total_all.str())
    ResultCard(Res.string.laps.str()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${Res.string.lap_average.str()} ${formatStopwatch(splits.sumOf { it.lap } / splits.size)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            CopyIconButton(lapsTable(splits, header))
        }
        Row(Modifier.fillMaxWidth()) {
            Text(header[0], style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.5f))
            Text(header[1], style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1.4f))
            Text(header[2], style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            splits.asReversed().forEach { lap ->
                val color = when {
                    splits.size > 1 && lap.lap == fastest -> MaterialTheme.colorScheme.primary
                    splits.size > 1 && lap.lap == slowest -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
                // against the lap before: minus is faster
                val change = splits.getOrNull(lap.index - 2)?.let { lap.lap - it.lap }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(lap.index.toString(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.5f))
                    Row(Modifier.weight(1.4f), verticalAlignment = Alignment.CenterVertically) {
                        Text(formatStopwatch(lap.lap), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFamily()), color = color)
                        if (change != null) {
                            Text(
                                " " + (if (change > 0) "+" else "−") + formatStopwatch(abs(change)),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = monoFamily()),
                                color = if (change > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Text(
                        formatStopwatch(lap.total),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFamily()),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
