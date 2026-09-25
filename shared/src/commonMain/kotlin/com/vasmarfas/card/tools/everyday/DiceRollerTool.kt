package com.vasmarfas.card.tools.everyday

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.Stepper
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
    val coinName = Res.string.flip_a_coin.str()
    var count by rememberSaveable { mutableStateOf(2) }
    var faces by rememberSaveable { mutableStateOf(6) }
    var dice by remember { mutableStateOf<List<Int>>(emptyList()) }
    var diceFaces by remember { mutableStateOf(6) }
    var rolls by remember { mutableStateOf(0) }
    var coin by remember { mutableStateOf<Boolean?>(null) }
    var flips by remember { mutableStateOf(0) }
    val history = remember { mutableStateListOf<String>() }
    fun record(line: String) {
        history.add(0, line)
        if (history.size > 20) history.removeAt(history.lastIndex)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Stepper(Res.string.dice_count.str(), count, 1..12) { count = it }
        Stepper(Res.string.dice_faces.str(), faces, 2..100) { faces = it }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        ActionButton(
            text = Res.string.roll.str(),
            onClick = {
                val values = Dice.roll(DiceExpr(listOf(DiceTerm(count, faces)), 0)).rolls.single()
                dice = values
                diceFaces = faces
                rolls++
                record("$count × $faces: " + values.joinToString(" + ") + if (values.size > 1) " = ${values.sum()}" else "")
            },
            icon = Icons.Filled.Casino,
            modifier = Modifier.weight(1f),
        )
        ActionButton(
            text = coinName,
            onClick = {
                val side = Random.nextBoolean()
                coin = side
                flips++
                record("$coinName: " + if (side) heads else tails)
            },
            icon = Icons.Filled.Toll,
            modifier = Modifier.weight(1f),
        )
    }
    if (dice.isNotEmpty()) {
        ResultCard {
            DiceTray(dice, diceFaces, rolls)
            if (dice.size > 1) {
                Text(
                    "${Res.string.total.str()}: ${dice.sum()}",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    coin?.let { side ->
        ResultCard { Coin(side, flips, heads, tails) }
    }
    if (history.isNotEmpty()) {
        ResultCard(Res.string.history.str()) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                history.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
            ActionButton(text = Res.string.clear_history.str(), onClick = { history.clear() })
        }
    }
}
