package com.vasmarfas.card.tools.text

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.vasmarfas.card.core.playTone
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.SegmentedChoice
import com.vasmarfas.card.ui.components.ToolInputField
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private enum class MorseMode { ENCODE, DECODE }

val morseCodeTool = Tool(
    id = "morse-code",
    category = ToolCategory.TEXT,
    title = Res.string.morse_code,
    description = Res.string.morse_code_description,
    icon = Icons.Filled.Sensors,
    keywords = listOf("morse", "telegraph", "dots", "dashes", "sos", "морзе", "телеграф", "точки", "тире"),
) { MorseCodeScreen() }

@Composable
private fun MorseCodeScreen() {
    var mode by rememberSaveable { mutableStateOf(MorseMode.ENCODE) }
    var alphabet by rememberSaveable { mutableStateOf(MorseAlphabet.LATIN) }
    var input by rememberSaveable { mutableStateOf("") }
    var playing by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    SegmentedChoice(
        options = MorseMode.entries,
        selected = mode,
        onSelect = { mode = it },
        label = { if (it == MorseMode.ENCODE) Res.string.text_morse.str() else Res.string.morse_text.str() },
    )
    if (mode == MorseMode.DECODE) {
        SegmentedChoice(
            options = MorseAlphabet.entries,
            selected = alphabet,
            onSelect = { alphabet = it },
            label = { if (it == MorseAlphabet.LATIN) Res.string.morse_latin.str() else Res.string.cyrillic.str() },
        )
    }
    ToolInputField(
        value = input,
        onValueChange = { input = it },
        label = if (mode == MorseMode.ENCODE) Res.string.text.str() else Res.string.morse_letters_by_spaces_words_by.str(),
        singleLine = false,
        minLines = 3,
        placeholder = if (mode == MorseMode.ENCODE) "SOS" else "... --- ...",
        monospace = mode == MorseMode.DECODE,
    )
    val output = remember(input, mode, alphabet) {
        if (mode == MorseMode.ENCODE) Morse.encode(input) else Morse.decode(input, alphabet)
    }
    val morse = if (mode == MorseMode.ENCODE) output else input
    if (input.isNotBlank()) {
        OutputCard(output)
        ActionButton(
            text = if (playing) Res.string.stop_short.str() else Res.string.play.str(),
            icon = if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow,
            onClick = {
                if (playing) {
                    job?.cancel()
                } else {
                    job = scope.launch {
                        playing = true
                        try {
                            for (step in Morse.schedule(morse)) {
                                if (step.tone) playTone(700.0, step.ms)
                                delay(step.ms.milliseconds)
                            }
                        } finally {
                            playing = false
                        }
                    }
                }
            },
        )
    }
}
