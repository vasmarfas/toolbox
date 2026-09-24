package com.vasmarfas.card.tools.fitness

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.formatDurationMs
import com.vasmarfas.card.core.str
import com.vasmarfas.card.core.vibrate
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.LocalChrome
import com.vasmarfas.card.ui.components.SwitchRow
import com.vasmarfas.card.ui.components.ToolSection
import com.vasmarfas.card.ui.components.expandedSquare
import kotlin.math.ceil
import kotlin.time.TimeSource
import org.jetbrains.compose.resources.stringResource

val breathingTool = Tool(
    id = "breathing",
    category = ToolCategory.FITNESS,
    title = Res.string.breathing_exercise,
    description = Res.string.breathing_description,
    icon = Icons.Filled.Air,
    keywords = listOf(
        "breathing", "box breathing", "4-7-8", "relax", "calm", "meditation", "sleep", "paced breathing",
        "дыхание", "дыхательная гимнастика", "квадратное дыхание", "успокоиться", "медитация", "уснуть", "расслабиться",
    ),
    expandable = true,
) { BreathingScreen() }

private val sessionMinutes = listOf(1, 3, 5, 10)

@Composable
private fun BreathingScreen() {
    var pattern by rememberSaveable { mutableStateOf(BreathPattern.BOX) }
    var minutes by rememberSaveable { mutableStateOf(3) }
    var vibration by rememberSaveable { mutableStateOf(true) }
    var running by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var elapsed by remember { mutableDoubleStateOf(0.0) }
    val immersive = LocalChrome.current.immersive
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        finished = false
        elapsed = 0.0
        val start = TimeSource.Monotonic.markNow()
        var phase: BreathPhase? = null
        while (true) {
            withFrameMillis { }
            elapsed = start.elapsedNow().inWholeMilliseconds / 1000.0
            if (elapsed >= minutes * 60) {
                finished = true
                running = false
                break
            }
            val current = Breathing.at(pattern, elapsed).phase
            if (phase != null && current != phase && vibration) vibrate(40)
            phase = current
        }
    }

    if (!running && !immersive) {
        ChoiceChips(options = BreathPattern.entries, selected = pattern, onSelect = { pattern = it }, label = { patternName(it) })
        Text(patternHint(pattern), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ToolSection(Res.string.duration.str()) {
            val min = Res.string.unit_min.str()
            ChoiceChips(options = sessionMinutes, selected = minutes, onSelect = { minutes = it }, label = { "$it $min" })
        }
        SwitchRow(Res.string.vibrate_on_phase_change.str(), vibration, { vibration = it })
    }
    val moment = if (running) Breathing.at(pattern, elapsed) else null
    val side = expandedSquare(normal = 280.dp, reserved = 180.dp)
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth().height(side), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(side)) {
            val outer = size.minDimension / 2
            val inner = outer * 0.35f
            drawCircle(colors.outlineVariant, radius = outer - 2.dp.toPx(), style = Stroke(2.dp.toPx()))
            drawCircle(colors.primaryContainer, radius = inner + (outer - inner) * (moment?.fill ?: 0f))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                when (moment?.phase) {
                    null -> if (finished) Res.string.done.str() else patternName(pattern)
                    BreathPhase.INHALE -> Res.string.breath_inhale.str()
                    BreathPhase.HOLD_IN, BreathPhase.HOLD_OUT -> Res.string.breath_hold.str()
                    BreathPhase.EXHALE -> Res.string.breath_exhale.str()
                },
                style = MaterialTheme.typography.headlineSmall,
                color = colors.onPrimaryContainer,
            )
            if (moment != null) {
                Text(ceil(moment.secondsLeft).toInt().toString(), style = MaterialTheme.typography.displaySmall, color = colors.onPrimaryContainer)
            }
        }
    }
    ActionButton(
        text = if (running) Res.string.stop.str() else Res.string.start.str(),
        icon = if (running) Icons.Filled.Stop else Icons.Filled.PlayArrow,
        onClick = { running = !running },
        modifier = Modifier.fillMaxWidth(),
    )
    if (moment != null) {
        Text(
            stringResource(Res.string.breathing_left, formatDurationMs(((minutes * 60 - elapsed) * 1000).toLong()), moment.cycle + 1),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    } else if (!immersive) {
        Text(Res.string.breathing_dizzy_hint.str(), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun patternName(pattern: BreathPattern): String = when (pattern) {
    BreathPattern.BOX -> Res.string.breath_box.str()
    BreathPattern.RELAX -> "4-7-8"
    BreathPattern.COHERENT -> Res.string.breath_coherent.str()
    BreathPattern.CALM -> Res.string.breath_calm.str()
}

@Composable
private fun patternHint(pattern: BreathPattern): String = when (pattern) {
    BreathPattern.BOX -> Res.string.breath_box_hint.str()
    BreathPattern.RELAX -> Res.string.breath_relax_hint.str()
    BreathPattern.COHERENT -> Res.string.breath_coherent_hint.str()
    BreathPattern.CALM -> Res.string.breath_calm_hint.str()
}
