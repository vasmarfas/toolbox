package com.vasmarfas.card.tools.sound

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.vasmarfas.card.core.Waveform
import com.vasmarfas.card.core.fmt
import com.vasmarfas.card.core.startTone
import com.vasmarfas.card.core.stopTone
import com.vasmarfas.card.core.str
import com.vasmarfas.card.core.toDoubleLenient
import com.vasmarfas.card.core.toneSampleRate
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.NumberField
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

val toneGeneratorTool = Tool(
    id = "tone-generator",
    category = ToolCategory.SOUND,
    title = Res.string.tone_generator,
    description = Res.string.tone_generator_description,
    icon = Icons.Filled.GraphicEq,
    keywords = listOf(
        "frequency", "hz", "sine", "square", "sawtooth", "noise", "speaker test", "hearing",
        "частота", "звук", "динамик", "синус", "меандр", "пила", "шум", "генератор",
    ),
) { ToneGeneratorScreen() }

private val presets = listOf(20f, 50f, 440f, 1000f, 10_000f, 15_000f, 20_000f)

private fun waveformLabel(waveform: Waveform): StringResource = when (waveform) {
    Waveform.SINE -> Res.string.wave_sine
    Waveform.SQUARE -> Res.string.wave_square
    Waveform.TRIANGLE -> Res.string.wave_triangle
    Waveform.SAWTOOTH -> Res.string.wave_sawtooth
    Waveform.NOISE -> Res.string.wave_noise
}

@Composable
private fun ToneGeneratorScreen() {
    val sampleRate = remember { toneSampleRate() }
    val ceiling = (sampleRate / 2).toFloat()
    var frequency by rememberSaveable { mutableStateOf(440f) }
    var volume by rememberSaveable { mutableStateOf(0.6f) }
    var waveform by rememberSaveable { mutableStateOf(Waveform.SINE) }
    var playing by remember { mutableStateOf(false) }

    DisposableEffect(Unit) { onDispose { stopTone() } }

    // retuning is driven from the controls rather than an effect on the state they write:
    // the oscillator has to follow a drag without waiting for anything to be scheduled
    fun retune() {
        if (playing) startTone(frequency.toDouble(), waveform, volume)
    }

    Text(formatFrequency(frequency), style = MaterialTheme.typography.displayMedium)
    Slider(
        value = log2(frequency.coerceIn(MinFrequency, ceiling)),
        onValueChange = {
            frequency = 2f.pow(it).coerceIn(MinFrequency, ceiling)
            retune()
        },
        valueRange = log2(MinFrequency)..log2(ceiling),
    )
    ChoiceChips(
        options = presets.filter { it <= ceiling },
        selected = presets.firstOrNull { abs(it - frequency) < 0.5f },
        onSelect = {
            frequency = it
            retune()
        },
        label = { if (it == 440f) "A4 440" else formatFrequency(it) },
    )
    NumberField(
        value = frequency.toDouble().fmt(2),
        onValueChange = { text ->
            text.toDoubleLenient()?.let {
                frequency = it.toFloat().coerceIn(MinFrequency, ceiling)
                retune()
            }
        },
        label = Res.string.frequency.str(),
        suffix = Res.string.unit_hz.str(),
    )
    ChoiceChips(
        options = Waveform.entries,
        selected = waveform,
        onSelect = {
            waveform = it
            retune()
        },
        label = { waveformLabel(it).str() },
    )
    Text(Res.string.sound_volume.str(), style = MaterialTheme.typography.labelLarge)
    Slider(
        value = volume,
        onValueChange = {
            volume = it
            retune()
        },
    )
    ActionButton(
        text = if (playing) Res.string.stop.str() else Res.string.start.str(),
        onClick = {
            playing = !playing
            if (playing) startTone(frequency.toDouble(), waveform, volume) else stopTone()
        },
    )
    KeyValueRow(Res.string.sample_rate.str(), "$sampleRate ${Res.string.unit_hz.str()}", copyable = false)
    Text(
        stringResource(Res.string.tone_nyquist_note, sampleRate / 2),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        Res.string.tone_above_16_khz_most.str(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private const val MinFrequency = 1f

@Composable
private fun formatFrequency(hz: Float): String =
    if (hz >= 1000f) "${(hz / 1000.0).fmt(2)} ${Res.string.unit_khz.str()}" else "${hz.toDouble().fmt(1)} ${Res.string.unit_hz.str()}"
