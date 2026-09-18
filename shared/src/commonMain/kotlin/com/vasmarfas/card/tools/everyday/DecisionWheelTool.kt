package com.vasmarfas.card.tools.everyday

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.LocalLang
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.ToolInputField
import kotlin.random.Random
import kotlinx.coroutines.delay

val decisionWheelTool = Tool(
    id = "decision-wheel",
    category = ToolCategory.EVERYDAY,
    title = Res.string.decision_wheel,
    description = Res.string.random_pick_from_your_own_list_of_options_wi,
    icon = Icons.Filled.RotateRight,
    keywords = listOf("random", "pick", "wheel", "choice", "lottery", "случайный выбор", "колесо", "жребий", "рандом"),
) { DecisionWheelScreen() }

@Composable
private fun DecisionWheelScreen() {
    val initialOptions = Res.string.pizza_sushi_burgers_pasta.str()
    var text by rememberSaveable { mutableStateOf(initialOptions) }
    var highlighted by remember { mutableStateOf(0) }
    var spinning by remember { mutableStateOf(false) }
    var spinSeq by remember { mutableStateOf(0) }
    var result by remember { mutableStateOf<String?>(null) }
    val history = remember { mutableStateListOf<String>() }
    val options = remember(text) { DecisionWheel.parseOptions(text) }

    LaunchedEffect(spinSeq) {
        if (spinSeq == 0 || options.size < 2) return@LaunchedEffect
        spinning = true
        result = null
        val target = Random.nextInt(options.size)
        val steps = DecisionWheel.stepsToReach(highlighted, target, options.size)
        DecisionWheel.delays(steps).forEach { pause ->
            highlighted = (highlighted + 1) % options.size
            delay(pause)
        }
        val picked = options[target]
        result = picked
        history.add(0, picked)
        if (history.size > 20) history.removeAt(history.lastIndex)
        spinning = false
    }

    ToolInputField(
        value = text,
        onValueChange = { text = it },
        label = Res.string.options_one_per_line.str(),
        singleLine = false,
        minLines = 4,
    )
    if (options.size < 2) {
        ErrorText(Res.string.add_at_least_two_options.str())
    }
    ChoiceChips(
        options = options,
        selected = options.getOrNull(highlighted % maxOf(1, options.size)),
        onSelect = {},
        label = { it },
    )
    ActionButton(
        text = Res.string.spin.str(),
        onClick = { spinSeq++ },
        enabled = options.size >= 2 && !spinning,
        icon = Icons.Filled.RotateRight,
        modifier = Modifier.fillMaxWidth(),
    )
    ResultCard {
        Text(
            text = result ?: options.getOrNull(highlighted % maxOf(1, options.size)) ?: "—",
            style = MaterialTheme.typography.headlineLarge,
            color = if (result != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (history.isNotEmpty()) {
        ResultCard(Res.string.history.str()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text(history.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            }
            ActionButton(text = Res.string.clear_history.str(), onClick = { history.clear() })
        }
    }
}
