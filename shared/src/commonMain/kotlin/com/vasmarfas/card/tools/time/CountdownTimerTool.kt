package com.vasmarfas.card.tools.time

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.currentEpochMillis
import com.vasmarfas.card.core.playTone
import com.vasmarfas.card.core.str
import com.vasmarfas.card.core.vibrate
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SegmentedChoice
import com.vasmarfas.card.ui.components.expandedTextStyle
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private enum class TimerMode { TIMER, POMODORO }

private enum class PomodoroPhase { WORK, BREAK }

val countdownTimerTool = Tool(
    id = "countdown-timer",
    category = ToolCategory.TIME,
    title = Res.string.countdown_timer,
    description = Res.string.countdown_with_presets_and_a_sound_at_the_en,
    icon = Icons.Filled.HourglassBottom,
    keywords = listOf("timer", "countdown", "pomodoro", "alarm", "таймер", "отсчёт", "помодоро", "будильник"),
    expandable = true,
) { CountdownTimerScreen() }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CountdownTimerScreen() {
    var mode by rememberSaveable { mutableStateOf(TimerMode.TIMER) }
    var hText by rememberSaveable { mutableStateOf("0") }
    var mText by rememberSaveable { mutableStateOf("5") }
    var sText by rememberSaveable { mutableStateOf("0") }
    var workText by rememberSaveable { mutableStateOf("25") }
    var breakText by rememberSaveable { mutableStateOf("5") }
    var running by rememberSaveable { mutableStateOf(false) }
    var started by rememberSaveable { mutableStateOf(false) }
    var endAt by rememberSaveable { mutableStateOf(0L) }
    var remainingPaused by rememberSaveable { mutableStateOf(0L) }
    var totalMs by rememberSaveable { mutableStateOf(0L) }
    var phase by rememberSaveable { mutableStateOf(PomodoroPhase.WORK) }
    var cycles by rememberSaveable { mutableStateOf(0) }
    var alarmSeq by remember { mutableStateOf(0) }
    var now by remember { mutableStateOf(currentEpochMillis()) }

    val h = hText.trim().toIntOrNull()
    val m = mText.trim().toIntOrNull()
    val s = sText.trim().toIntOrNull()
    val work = workText.trim().toIntOrNull()
    val brk = breakText.trim().toIntOrNull()
    val timerMs = if (h != null && m != null && s != null && h >= 0 && m >= 0 && s >= 0) (h * 3600L + m * 60L + s) * 1000L else -1L
    val workMs = if (work != null && work > 0) work * 60_000L else -1L
    val breakMs = if (brk != null && brk > 0) brk * 60_000L else -1L
    val configured = when (mode) {
        TimerMode.TIMER -> timerMs
        TimerMode.POMODORO -> if (phase == PomodoroPhase.WORK) workMs else breakMs
    }
    val valid = when (mode) {
        TimerMode.TIMER -> timerMs > 0
        TimerMode.POMODORO -> workMs > 0 && breakMs > 0
    }

    LaunchedEffect(running) {
        while (running) {
            delay(30)
            now = currentEpochMillis()
            if (now >= endAt) {
                alarmSeq++
                if (mode == TimerMode.POMODORO) {
                    if (phase == PomodoroPhase.WORK) cycles++
                    phase = if (phase == PomodoroPhase.WORK) PomodoroPhase.BREAK else PomodoroPhase.WORK
                    val next = if (phase == PomodoroPhase.WORK) workMs else breakMs
                    totalMs = next
                    endAt = now + next
                } else {
                    running = false
                    started = false
                }
            }
        }
    }
    LaunchedEffect(alarmSeq) {
        if (alarmSeq > 0) {
            vibrate(300)
            repeat(3) {
                playTone(880.0, 400)
                delay(500.milliseconds)
            }
        }
    }

    val remaining = when {
        running -> maxOf(0L, endAt - now)
        started -> remainingPaused
        else -> maxOf(0L, configured)
    }
    val total = if (started || running) totalMs else configured
    val fraction = if (total > 0) (1f - remaining.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f

    SegmentedChoice(
        options = TimerMode.entries,
        selected = mode,
        onSelect = {
            if (!started && !running) {
                mode = it
                phase = PomodoroPhase.WORK
                cycles = 0
            }
        },
        label = { if (it == TimerMode.TIMER) Res.string.timer.str() else "Pomodoro" },
    )
    if (mode == TimerMode.TIMER) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            NumberField(hText, { hText = it; started = false }, Res.string.hours_2.str(), Modifier.weight(1f), isError = h == null)
            NumberField(mText, { mText = it; started = false }, Res.string.minutes_2.str(), Modifier.weight(1f), isError = m == null)
            NumberField(sText, { sText = it; started = false }, Res.string.seconds_2.str(), Modifier.weight(1f), isError = s == null)
        }
        ChoiceChips(
            options = listOf(1, 5, 10, 25),
            selected = null,
            onSelect = {
                hText = "0"
                mText = it.toString()
                sText = "0"
                started = false
                running = false
            },
            label = { "$it ${Res.string.min.str()}" },
        )
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            NumberField(workText, { workText = it; started = false }, Res.string.work_min.str(), Modifier.weight(1f), isError = workMs <= 0)
            NumberField(breakText, { breakText = it; started = false }, Res.string.break_min.str(), Modifier.weight(1f), isError = breakMs <= 0)
        }
    }
    if (!valid) ErrorText(Res.string.enter_a_duration_greater_than_zero.str())

    ResultCard {
        if (mode == TimerMode.POMODORO) {
            Text(
                text = (if (phase == PomodoroPhase.WORK) Res.string.work else Res.string.break_).str() +
                    " · " + Res.string.cycles.str() + ": " + cycles,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            text = formatCountdown(remaining),
            style = expandedTextStyle(MaterialTheme.typography.displayLarge.copy(fontFamily = FontFamily.Monospace)),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        LinearWavyProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
        if (alarmSeq > 0 && !running && !started) {
            Text(
                text = Res.string.time_is_up.str(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        if (running) {
            ActionButton(
                text = Res.string.pause.str(),
                onClick = {
                    remainingPaused = maxOf(0L, endAt - currentEpochMillis())
                    running = false
                },
                icon = Icons.Filled.Pause,
                modifier = Modifier.weight(1f),
            )
        } else {
            ActionButton(
                text = (if (started) Res.string.resume else Res.string.start).str(),
                onClick = {
                    val nowMs = currentEpochMillis()
                    if (!started) {
                        totalMs = configured
                        remainingPaused = configured
                        started = true
                    }
                    now = nowMs
                    endAt = nowMs + remainingPaused
                    running = true
                },
                enabled = valid,
                icon = Icons.Filled.PlayArrow,
                modifier = Modifier.weight(1f),
            )
        }
        ActionButton(
            text = Res.string.reset.str(),
            onClick = {
                running = false
                started = false
                phase = PomodoroPhase.WORK
                cycles = 0
                alarmSeq = 0
            },
            enabled = started || running || cycles > 0,
            icon = Icons.Filled.Refresh,
            modifier = Modifier.weight(1f),
        )
    }
}
