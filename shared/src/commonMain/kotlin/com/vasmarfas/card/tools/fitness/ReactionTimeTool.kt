package com.vasmarfas.card.tools.fitness

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.theme.LocalStatusColors
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlinx.coroutines.delay

val reactionTimeTool = Tool(
    id = "reaction-time",
    category = ToolCategory.FITNESS,
    title = Res.string.reaction_time,
    description = Res.string.reaction_time_description,
    icon = Icons.Filled.TouchApp,
    keywords = listOf(
        "reaction", "reflex", "speed", "test", "milliseconds", "game",
        "реакция", "рефлексы", "скорость", "тест", "миллисекунды", "игра",
    ),
) { ReactionTimeScreen() }

private enum class ReactionPhase { IDLE, WAITING, GO, DONE, EARLY }

private const val KeptTries = 5

@Composable
private fun ReactionTimeScreen() {
    var phase by remember { mutableStateOf(ReactionPhase.IDLE) }
    var round by remember { mutableStateOf(0) }
    var shownAt by remember { mutableStateOf<TimeSource.Monotonic.ValueTimeMark?>(null) }
    val results = remember { mutableStateListOf<Long>() }
    LaunchedEffect(round) {
        if (round == 0) return@LaunchedEffect
        shownAt = null
        delay(Random.nextLong(1500, 4500).milliseconds)
        if (phase != ReactionPhase.WAITING) return@LaunchedEffect
        phase = ReactionPhase.GO
        withFrameNanos { }
        shownAt = TimeSource.Monotonic.markNow()
    }
    val status = LocalStatusColors.current
    val colors = MaterialTheme.colorScheme
    val (background, content) = when (phase) {
        ReactionPhase.WAITING -> status.bad to Color.White
        ReactionPhase.GO -> status.good to Color.White
        else -> colors.primaryContainer to colors.onPrimaryContainer
    }
    val last = results.lastOrNull()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(background)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable {
                phase = when (phase) {
                    ReactionPhase.WAITING -> ReactionPhase.EARLY
                    ReactionPhase.GO -> {
                        shownAt?.let { results.add(it.elapsedNow().inWholeMilliseconds) }
                        ReactionPhase.DONE
                    }
                    else -> {
                        round++
                        ReactionPhase.WAITING
                    }
                }
            }
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            when (phase) {
                ReactionPhase.IDLE -> Res.string.reaction_tap_to_start.str()
                ReactionPhase.WAITING -> Res.string.reaction_wait.str()
                ReactionPhase.GO -> Res.string.reaction_go.str()
                ReactionPhase.EARLY -> Res.string.reaction_too_early.str()
                ReactionPhase.DONE -> "$last ${Res.string.unit_ms.str()}\n${Res.string.reaction_again.str()}"
            },
            style = if (phase == ReactionPhase.DONE) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineSmall,
            color = content,
            textAlign = TextAlign.Center,
        )
    }
    if (results.isNotEmpty()) {
        val ms = Res.string.unit_ms.str()
        ResultCard {
            KeyValueRow(Res.string.reaction_best.str(), "${results.min()} $ms", copyable = false)
            val recent = results.takeLast(KeptTries)
            KeyValueRow(Res.string.reaction_average.str(), "${recent.average().toLong()} $ms", copyable = false)
            KeyValueRow(Res.string.reaction_tries.str(), recent.joinToString(" · "), copyable = false)
        }
    }
    Text(
        Res.string.reaction_note.str(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
