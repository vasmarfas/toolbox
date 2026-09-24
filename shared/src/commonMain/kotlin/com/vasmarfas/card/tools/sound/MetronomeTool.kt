package com.vasmarfas.card.tools.sound

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.currentEpochMillis
import com.vasmarfas.card.core.playTone
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.ChoiceChips
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val metronomeTool = Tool(
    id = "metronome",
    category = ToolCategory.SOUND,
    title = Res.string.metronome,
    description = Res.string.metronome_description,
    icon = Icons.Filled.MusicNote,
    keywords = listOf("bpm", "tempo", "beat", "music", "темп", "ритм", "музыка"),
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
        ) { Text(Res.string.tap_tempo.str()) }
    }
}
