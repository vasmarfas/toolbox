package com.vasmarfas.card.tools.everyday

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

val diceRollerTool = Tool(
    id = "dice-roller",
    category = ToolCategory.EVERYDAY,
    title = Res.string.dice_roller,
    description = Res.string.dice_roller_description,
    icon = Icons.Filled.Casino,
    keywords = listOf("dice", "d20", "roll", "coin", "rpg", "кубик", "кости", "бросок", "монета", "d6"),
) { DiceRollerScreen() }

@Composable
private fun DiceRollerScreen() {
    val heads = Res.string.heads.str()
    val tails = Res.string.tails.str()
    var notation by rememberSaveable { mutableStateOf("2d6+3") }
    var last by remember { mutableStateOf<String?>(null) }
    var lastDetail by remember { mutableStateOf("") }
    val history = remember { mutableStateListOf<String>() }
    val expr = remember(notation) { Dice.parse(notation) }
    fun record(title: String, detail: String) {
        last = title
        lastDetail = detail
        history.add(0, if (detail.isEmpty()) title else "$title  ($detail)")
        if (history.size > 20) history.removeAt(history.lastIndex)
    }
    fun rollExpr(e: DiceExpr) {
        val r = Dice.roll(e)
        record("${e.notation} = ${r.total}", r.detail)
    }

    ToolInputField(
        value = notation,
        onValueChange = { notation = it },
        label = Res.string.notation.str(),
        placeholder = "2d6+3 · d20 · 3d8-1",
        isError = expr == null,
        monospace = true,
    )
    if (expr == null) ErrorText(Res.string.dice_use_ndm_with_optional.str())
    ChoiceChips(
        options = listOf(4, 6, 8, 10, 12, 20, 100),
        selected = null,
        onSelect = {
            notation = "1d$it"
            rollExpr(DiceExpr(listOf(DiceTerm(1, it)), 0))
        },
        label = { "d$it" },
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        ActionButton(
            text = Res.string.roll.str(),
            onClick = { expr?.let { rollExpr(it) } },
            enabled = expr != null,
            icon = Icons.Filled.Casino,
            modifier = Modifier.weight(1f),
        )
        ActionButton(
            text = Res.string.flip_a_coin.str(),
            onClick = {
                record(if (Random.nextBoolean()) heads else tails, "")
            },
            modifier = Modifier.weight(1f),
        )
    }
    ResultCard {
        Text(
            text = last ?: "—",
            style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Monospace),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (lastDetail.isNotEmpty()) {
            Text(
                text = lastDetail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    if (history.isNotEmpty()) {
        ResultCard(Res.string.history.str()) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                history.forEach { Text(it, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)) }
            }
            ActionButton(text = Res.string.clear_history.str(), onClick = { history.clear() })
        }
    }
}
