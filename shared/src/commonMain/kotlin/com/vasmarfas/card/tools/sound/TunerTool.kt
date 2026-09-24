package com.vasmarfas.card.tools.sound

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.AppPermission
import com.vasmarfas.card.core.ensurePermission
import com.vasmarfas.card.core.fmt
import com.vasmarfas.card.core.microphoneSpectrumFlow
import com.vasmarfas.card.core.microphoneSupported
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.theme.LocalStatusColors
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val tunerTool = Tool(
    id = "tuner",
    category = ToolCategory.SOUND,
    title = Res.string.tuner,
    description = Res.string.tuner_description,
    icon = Icons.Filled.Tune,
    keywords = listOf(
        "tuner", "guitar", "bass", "ukulele", "violin", "pitch", "cents", "strings",
        "тюнер", "гитара", "бас", "укулеле", "скрипка", "настройка", "струны", "нота",
    ),
) { TunerScreen() }

private val references = listOf(432.0, 440.0, 442.0)

private const val FftSize = 16384

private const val MedianOf = 5

private const val SilentFramesBeforeReset = 12

private const val InTuneCents = 5

@Composable
private fun TunerScreen() {
    var instrument by rememberSaveable { mutableStateOf(Instrument.GUITAR) }
    var a4 by rememberSaveable { mutableStateOf(440.0) }
    var locked by rememberSaveable { mutableStateOf<String?>(null) }
    var running by remember { mutableStateOf(false) }
    var frequency by remember { mutableStateOf<Double?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    if (!microphoneSupported()) {
        Text(Res.string.not_available_on_this_platform.str())
        return
    }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        error = null
        val recent = ArrayDeque<Double>()
        var silent = 0
        microphoneSpectrumFlow(FftSize)
            .map { Tuning.fundamental(it) }
            .flowOn(Dispatchers.Default)
            .catch { error = it.message ?: it.toString(); running = false }
            .collect { detected ->
                if (detected == null) {
                    if (++silent > SilentFramesBeforeReset) {
                        recent.clear()
                        frequency = null
                    }
                    return@collect
                }
                silent = 0
                if (recent.isNotEmpty() && abs(Tuning.cents(detected, recent.last())) > 100) recent.clear()
                recent.addLast(detected)
                if (recent.size > MedianOf) recent.removeFirst()
                frequency = recent.sorted()[recent.size / 2]
            }
    }

    ChoiceChips(
        options = Instrument.entries,
        selected = instrument,
        onSelect = {
            instrument = it
            locked = null
        },
        label = { it.title.str() },
    )
    Text(Res.string.tuner_reference.str(), style = MaterialTheme.typography.labelLarge)
    ChoiceChips(
        options = references,
        selected = a4,
        onSelect = { a4 = it },
        label = { "${it.fmt(0)} ${Res.string.unit_hz.str()}" },
    )
    ActionButton(
        text = if (running) Res.string.stop.str() else Res.string.start.str(),
        onClick = {
            if (running) running = false
            else scope.launch { if (ensurePermission(AppPermission.MICROPHONE)) running = true }
        },
    )
    error?.let { ErrorText(micErrorLabel(it).str()) }

    val heard = frequency
    val target = locked?.let { Tuning.pitch(it, a4) } ?: heard?.let { Tuning.target(it, instrument, a4) }
    if (instrument.strings.isNotEmpty()) {
        ChoiceChips(
            options = instrument.strings,
            selected = locked ?: target?.label,
            onSelect = { locked = if (locked == it) null else it },
            label = { it },
        )
    }
    val cents = if (heard != null && target != null) Tuning.cents(heard, target.frequencyHz) else null
    ResultCard {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                target?.label ?: "—",
                style = MaterialTheme.typography.displayLarge,
            )
            Text(
                when {
                    !running -> Res.string.tuner_press_start.str()
                    cents == null -> Res.string.tuner_play_a_note.str()
                    else -> "${if (cents > 0) "+" else ""}${cents.roundToInt()} ¢"
                },
                style = MaterialTheme.typography.titleLarge,
            )
        }
        CentsGauge(cents)
        if (heard != null && target != null && cents != null) {
            Text(
                "${heard.fmt(1)} ${Res.string.unit_hz.str()} · ${target.label} = ${target.frequencyHz.fmt(1)} ${Res.string.unit_hz.str()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                when {
                    abs(cents) <= InTuneCents -> Res.string.tuner_in_tune
                    instrument.strings.isEmpty() -> if (cents < 0) Res.string.tuner_flat else Res.string.tuner_sharp
                    else -> if (cents < 0) Res.string.tuner_tighten else Res.string.tuner_loosen
                }.str(),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun CentsGauge(cents: Double?) {
    val status = LocalStatusColors.current
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val tick = MaterialTheme.colorScheme.onSurfaceVariant
    val shown by animateFloatAsState((cents ?: 0.0).coerceIn(-50.0, 50.0).toFloat())
    val color = when {
        cents == null -> tick
        abs(cents) <= InTuneCents -> status.good
        abs(cents) <= 15 -> status.warn
        else -> status.bad
    }
    Canvas(Modifier.fillMaxWidth().height(48.dp)) {
        val middle = size.height / 2
        val bar = 10.dp.toPx()
        drawRoundRect(track, Offset(0f, middle - bar / 2), Size(size.width, bar), CornerRadius(bar / 2))
        for (mark in listOf(-50, -25, 0, 25, 50)) {
            val x = size.width * (mark + 50) / 100f
            val half = (if (mark == 0) 18 else 10).dp.toPx()
            drawLine(tick, Offset(x, middle - half), Offset(x, middle + half), strokeWidth = 2.dp.toPx())
        }
        if (cents != null) drawCircle(color, 11.dp.toPx(), Offset(size.width * (shown + 50) / 100f, middle))
    }
}
