package com.vasmarfas.card.tools.measure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.AppPermission
import com.vasmarfas.card.core.PlatformKind
import com.vasmarfas.card.core.Waveform
import com.vasmarfas.card.core.currentEpochMillis
import com.vasmarfas.card.core.ensurePermission
import com.vasmarfas.card.core.fmt
import com.vasmarfas.card.core.hasPermission
import com.vasmarfas.card.core.microphoneLevelFlow
import com.vasmarfas.card.core.microphoneSupported
import com.vasmarfas.card.core.playTone
import com.vasmarfas.card.core.startTone
import com.vasmarfas.card.core.stopTone
import com.vasmarfas.card.core.str
import com.vasmarfas.card.core.toDoubleLenient
import com.vasmarfas.card.core.toneSampleRate
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.ChartKind
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SegmentedChoice
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

val soundMeterTool = Tool(
    id = "sound-meter",
    category = ToolCategory.MEASURE,
    title = Res.string.sound_meter,
    description = Res.string.approximate_loudness_in_db_from_the_micropho,
    icon = Icons.Filled.Mic,
    keywords = listOf("decibel", "noise", "db", "microphone", "loudness", "шум", "децибел", "громкость"),
    platforms = setOf(PlatformKind.ANDROID, PlatformKind.DESKTOP, PlatformKind.WEB),
    expandable = true,
) { SoundMeterScreen() }

@Composable
private fun SoundMeterScreen() {
    var permission by remember { mutableStateOf(hasPermission(AppPermission.MICROPHONE)) }
    var running by remember { mutableStateOf(false) }
    var level by remember { mutableStateOf<Double?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val history = remember { mutableStateListOf<Float>() }
    val scope = rememberCoroutineScope()
    if (!microphoneSupported()) {
        Text(Res.string.not_available_on_this_platform.str())
        return
    }
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        error = null
        microphoneLevelFlow().catch { error = it.message ?: it.toString(); running = false }.collect { db ->
            level = db
            history.add(db.toFloat())
            if (history.size > 200) history.removeAt(0)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionButton(
            text = if (running) Res.string.stop.str() else Res.string.start.str(),
            onClick = {
                if (running) running = false
                else scope.launch {
                    permission = ensurePermission(AppPermission.MICROPHONE)
                    if (permission) running = true
                }
            },
        )
        TextButton(onClick = { history.clear() }) { Text(Res.string.reset.str()) }
    }
    error?.let { ErrorText(it) }
    level?.let { db ->
        Text("${db.fmt(0)} dB", style = MaterialTheme.typography.displayLarge)
        LineChart(
            series = listOf(history.toList()),
            colors = listOf(MaterialTheme.colorScheme.primary),
            symmetric = false,
            kind = ChartKind.AREA,
        )
        ResultCard {
            if (history.isNotEmpty()) KeyValueRow("min / avg / max", "${history.min().toDouble().fmt(0)} / ${history.average().fmt(0)} / ${history.max().toDouble().fmt(0)} dB", copyable = false)
            KeyValueRow(Res.string.reference.str(), noiseReference(db).str(), mono = false, copyable = false)
        }
    }
}

private fun noiseReference(db: Double): StringResource = when {
    db < 30 -> Res.string.whisper_quiet_room
    db < 45 -> Res.string.quiet_office_library
    db < 60 -> Res.string.normal_conversation
    db < 70 -> Res.string.busy_street_vacuum_cleaner
    db < 85 -> Res.string.heavy_traffic_loud_music
    db < 100 -> Res.string.motorcycle_power_tools_hearing_risk_over_8_h
    else -> Res.string.concert_chainsaw_hearing_damage_risk
}

val toneGeneratorTool = Tool(
    id = "tone-generator",
    category = ToolCategory.MEASURE,
    title = Res.string.tone_generator,
    description = Res.string.tone_generator_description,
    icon = Icons.Filled.GraphicEq,
    keywords = listOf(
        "frequency", "hz", "sine", "square", "sawtooth", "noise", "speaker test", "hearing",
        "частота", "звук", "динамик", "синус", "меандр", "пила", "шум", "генератор",
    ),
    platforms = setOf(PlatformKind.ANDROID, PlatformKind.DESKTOP, PlatformKind.WEB),
) { ToneGeneratorScreen() }

private val notePresets = listOf(
    "20 Hz" to 20.0, "50 Hz" to 50.0, "A4 440" to 440.0, "1 kHz" to 1000.0,
    "10 kHz" to 10_000.0, "15 kHz" to 15_000.0, "20 kHz" to 20_000.0,
)

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
        options = notePresets.filter { it.second <= ceiling },
        selected = notePresets.firstOrNull { abs(it.second - frequency) < 0.5 },
        onSelect = {
            frequency = it.second.toFloat()
            retune()
        },
        label = { it.first },
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
        suffix = "Hz",
    )
    SegmentedChoice(
        options = Waveform.entries,
        selected = waveform,
        onSelect = {
            waveform = it
            retune()
        },
        label = { waveformLabel(it).str() },
    )
    Text(Res.string.volume_2.str(), style = MaterialTheme.typography.labelLarge)
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
    KeyValueRow(Res.string.sample_rate.str(), "$sampleRate Hz", copyable = false)
    Text(
        Res.string.tone_nyquist_note.str().replace("%d", (sampleRate / 2).toString()),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        Res.string.above_16_khz_most_adults_hear_nothing_that_i.str(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private const val MinFrequency = 1f

private fun formatFrequency(hz: Float): String =
    if (hz >= 1000f) "${(hz / 1000.0).fmt(2)} kHz" else "${hz.toDouble().fmt(1)} Hz"

val metronomeTool = Tool(
    id = "metronome",
    category = ToolCategory.MEASURE,
    title = Res.string.metronome,
    description = Res.string.click_track_from_30_to_300_bpm_with_accented,
    icon = Icons.Filled.MusicNote,
    keywords = listOf("bpm", "tempo", "beat", "music", "темп", "ритм", "музыка"),
    platforms = setOf(PlatformKind.ANDROID, PlatformKind.DESKTOP, PlatformKind.WEB),
) { MetronomeScreen() }

@Composable
private fun MetronomeScreen() {
    var bpm by rememberSaveable { mutableStateOf(120) }
    var beats by rememberSaveable { mutableStateOf(4) }
    var running by remember { mutableStateOf(false) }
    var beat by remember { mutableStateOf(0) }
    var job by remember { mutableStateOf<Job?>(null) }
    val taps = remember { mutableStateListOf<Long>() }
    val scope = rememberCoroutineScope()

    fun toggle() {
        if (running) {
            job?.cancel(); running = false; beat = 0
        } else {
            running = true
            job = scope.launch {
                var i = 0
                while (true) {
                    beat = i % beats
                    playTone(if (beat == 0) 1200.0 else 800.0, 40, 0.8f)
                    i++
                    delay((60_000.0 / bpm).toLong())
                }
            }
        }
    }

    Text("$bpm BPM", style = MaterialTheme.typography.displayMedium)
    Slider(value = bpm.toFloat(), onValueChange = { bpm = it.roundToInt() }, valueRange = 30f..300f)
    ChoiceChips(options = listOf(2, 3, 4, 6), selected = beats, onSelect = { beats = it }, label = { if (it == 6) "6/8" else "$it/4" })
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(beats) { i ->
            Text(if (running && i == beat) "●" else "○", style = MaterialTheme.typography.headlineMedium, color = if (i == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionButton(text = if (running) Res.string.stop.str() else Res.string.start.str(), onClick = ::toggle)
        TextButton(
            onClick = {
                val now = currentEpochMillis()
                taps.add(now)
                if (taps.size > 8) taps.removeAt(0)
                if (taps.size >= 2) {
                    val intervals = taps.zipWithNext { a, b -> b - a }.filter { it < 3000 }
                    if (intervals.isNotEmpty()) bpm = (60_000.0 / intervals.average()).roundToInt().coerceIn(30, 300)
                }
            },
        ) { Text("Tap tempo") }
    }
}
