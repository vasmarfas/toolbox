package com.vasmarfas.card.tools.time

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.playTone
import com.vasmarfas.card.core.str
import com.vasmarfas.card.core.toDoubleLenient
import com.vasmarfas.card.core.vibrate
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.LocalChrome
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.expandedHeight
import com.vasmarfas.card.ui.components.expandedTextStyle
import kotlin.time.TimeSource
import org.jetbrains.compose.resources.stringResource

val chessClockTool = Tool(
    id = "chess-clock",
    category = ToolCategory.TIME,
    title = Res.string.chess_clock,
    description = Res.string.chess_clock_description,
    icon = Icons.Filled.HourglassTop,
    keywords = listOf(
        "chess", "clock", "blitz", "rapid", "bullet", "fischer", "increment", "board game", "go",
        "шахматы", "шахматные часы", "блиц", "рапид", "пуля", "добавка", "настольная игра", "шашки", "го",
    ),
    expandable = true,
) { ChessClockScreen() }

private class ChessPreset(val minutes: Int, val incrementSeconds: Int)

private val presets = listOf(
    ChessPreset(1, 0),
    ChessPreset(3, 0),
    ChessPreset(3, 2),
    ChessPreset(5, 0),
    ChessPreset(5, 3),
    ChessPreset(10, 0),
    ChessPreset(15, 10),
    ChessPreset(30, 0),
)

@Composable
private fun ChessClockScreen() {
    var minutesText by rememberSaveable { mutableStateOf("5") }
    var incrementText by rememberSaveable { mutableStateOf("0") }
    val origin = remember { TimeSource.Monotonic.markNow() }
    val base = ((minutesText.toDoubleLenient() ?: 0.0) * 60_000).toLong()
    val increment = ((incrementText.toDoubleLenient() ?: 0.0) * 1000).toLong()
    var clock by remember { mutableStateOf(ChessClock(base, increment)) }
    var now by remember { mutableLongStateOf(0L) }
    val immersive = LocalChrome.current.immersive
    LaunchedEffect(base, increment) {
        if (!clock.started) clock = ChessClock(base, increment)
    }
    LaunchedEffect(clock.running, clock.turn) {
        while (clock.running) {
            withFrameMillis { }
            now = origin.elapsedNow().inWholeMilliseconds
            val ticked = clock.tick(now)
            if (ticked.flagged != null) {
                vibrate(600)
                playTone(440.0, 700)
            }
            clock = ticked
        }
    }
    fun press(side: ChessSide) {
        val next = clock.press(side, origin.elapsedNow().inWholeMilliseconds)
        if (next != clock) vibrate(20)
        clock = next
    }

    val half = expandedHeight(normal = 400.dp, reserved = 96.dp) / 2
    ClockHalf(clock, ChessSide.TOP, now, half, rotated = true) { press(ChessSide.TOP) }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(
            onClick = {
                val at = origin.elapsedNow().inWholeMilliseconds
                clock = if (clock.running) clock.pause(at) else clock.resume(at)
            },
            enabled = clock.started && clock.flagged == null,
        ) {
            Icon(
                if (clock.running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (clock.running) Res.string.pause.str() else Res.string.resume.str(),
            )
        }
        FilledTonalIconButton(onClick = { clock = ChessClock(base, increment) }, enabled = clock.started) {
            Icon(Icons.Filled.RestartAlt, contentDescription = Res.string.reset.str())
        }
    }
    ClockHalf(clock, ChessSide.BOTTOM, now, half, rotated = false) { press(ChessSide.BOTTOM) }
    if (!clock.started && !immersive) {
        ChoiceChips(
            options = presets,
            selected = presets.firstOrNull { it.minutes.toString() == minutesText.trim() && it.incrementSeconds.toString() == incrementText.trim() },
            onSelect = {
                minutesText = it.minutes.toString()
                incrementText = it.incrementSeconds.toString()
            },
            label = { "${it.minutes} + ${it.incrementSeconds}" },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(minutesText, { minutesText = it }, Res.string.minutes_per_game.str(), Modifier.weight(1f))
            NumberField(incrementText, { incrementText = it }, Res.string.increment_per_move.str(), Modifier.weight(1f), suffix = Res.string.unit_s.str())
        }
    }
}

@Composable
private fun ClockHalf(clock: ChessClock, side: ChessSide, now: Long, height: Dp, rotated: Boolean, onPress: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val active = clock.running && clock.turn == side
    val flagged = clock.flagged == side
    Surface(
        onClick = onPress,
        modifier = Modifier.fillMaxWidth().height(height).then(if (rotated) Modifier.rotate(180f) else Modifier),
        shape = MaterialTheme.shapes.extraLarge,
        color = when {
            flagged -> colors.errorContainer
            active -> colors.primaryContainer
            else -> colors.surfaceContainerHigh
        },
        contentColor = when {
            flagged -> colors.onErrorContainer
            active -> colors.onPrimaryContainer
            else -> colors.onSurface
        },
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    formatChessTime(clock.remaining(side, now)),
                    style = expandedTextStyle(MaterialTheme.typography.displayLarge).copy(fontFeatureSettings = "tnum"),
                    maxLines = 1,
                )
                Text(
                    when {
                        flagged -> Res.string.time_is_up.str()
                        !clock.started -> Res.string.chess_press_to_start.str()
                        !clock.running && clock.turn == side -> Res.string.paused.str()
                        else -> stringResource(Res.string.chess_moves, clock.moves(side))
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
