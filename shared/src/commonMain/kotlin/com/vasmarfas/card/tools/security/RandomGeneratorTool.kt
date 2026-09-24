package com.vasmarfas.card.tools.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.secureRandomBytes
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.tools.developer.Base64Tools
import com.vasmarfas.card.tools.developer.toHex
import com.vasmarfas.card.tools.text.OutputCard
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.AnswerCard
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.ToolInputField
import kotlin.math.abs
import org.jetbrains.compose.resources.StringResource

private enum class RandomMode(val title: StringResource) {
    INT(Res.string.number),
    UNIQUE(Res.string.unique_numbers),
    DICE(Res.string.dice),
    COIN(Res.string.coin),
    LIST(Res.string.from_a_list),
    BYTES(Res.string.bytes),
}

val randomGeneratorTool = Tool(
    id = "random-generator",
    category = ToolCategory.SECURITY,
    title = Res.string.random_generator,
    description = Res.string.random_generator_description,
    icon = Icons.Filled.Casino,
    keywords = listOf("random", "dice", "coin", "shuffle", "lottery", "pick", "случайное", "кубик", "монетка", "жребий", "перемешать", "розыгрыш"),
) { RandomGeneratorScreen() }

@Composable
private fun RandomGeneratorScreen() {
    var mode by rememberSaveable { mutableStateOf(RandomMode.INT) }
    var fromText by rememberSaveable { mutableStateOf("1") }
    var toText by rememberSaveable { mutableStateOf("100") }
    var countText by rememberSaveable { mutableStateOf("6") }
    var diceText by rememberSaveable { mutableStateOf("3d6+2") }
    var listText by rememberSaveable { mutableStateOf("") }
    var bytesText by rememberSaveable { mutableStateOf("32") }
    var seed by remember { mutableStateOf(0) }
    ChoiceChips(options = RandomMode.entries, selected = mode, onSelect = { mode = it }, label = { it.title.str() })
    when (mode) {
        RandomMode.INT, RandomMode.UNIQUE -> Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(value = fromText, onValueChange = { fromText = it }, label = Res.string.from__3.str(), modifier = Modifier.weight(1f))
            NumberField(value = toText, onValueChange = { toText = it }, label = Res.string.random_to.str(), modifier = Modifier.weight(1f))
            if (mode == RandomMode.UNIQUE) {
                NumberField(value = countText, onValueChange = { countText = it }, label = Res.string.random_count.str(), modifier = Modifier.weight(1f))
            }
        }

        RandomMode.DICE -> ToolInputField(
            value = diceText,
            onValueChange = { diceText = it },
            label = Res.string.dice_notation.str(),
            placeholder = "2d20 · 3d6+2 · d100",
            monospace = true,
        )

        RandomMode.LIST -> ToolInputField(
            value = listText,
            onValueChange = { listText = it },
            label = Res.string.options_one_per_line.str(),
            singleLine = false,
            minLines = 4,
        )

        RandomMode.BYTES -> NumberField(value = bytesText, onValueChange = { bytesText = it }, label = Res.string.bytes_1_1024.str())

        RandomMode.COIN -> {}
    }
    ActionButton(Res.string.roll.str(), onClick = { seed++ }, icon = Icons.Filled.Refresh)
    val from = fromText.trim().toIntOrNull()
    val to = toText.trim().toIntOrNull()
    val count = countText.trim().toIntOrNull()
    when (mode) {
        RandomMode.INT -> {
            if (from == null || to == null || from > to) {
                ErrorText(Res.string.random_enter_a_valid_range.str())
                return
            }
            val value = remember(from, to, seed) { Dice.intInRange(from, to) }
            AnswerCard(value.toString(), "${Res.string.range.str()}: $from … $to")
        }

        RandomMode.UNIQUE -> {
            if (from == null || to == null || from > to || count == null || count < 1) {
                ErrorText(Res.string.enter_a_valid_range_and_count.str())
                return
            }
            val values = remember(from, to, count, seed) { Dice.uniqueInts(from, to, count) }
            if (values == null) {
                ErrorText(Res.string.random_range_is_smaller.str())
                return
            }
            ResultCard {
                KeyValueRow(Res.string.draw_order.str(), values.joinToString(", "))
                KeyValueRow(Res.string.sorted.str(), values.sorted().joinToString(", "))
            }
        }

        RandomMode.DICE -> {
            val roll = remember(diceText, seed) { Dice.roll(diceText) }
            if (roll == null) {
                ErrorText(Res.string.random_notation_like_3d6_2.str())
                return
            }
            ResultCard {
                KeyValueRow(Res.string.random_total.str(), roll.total.toString())
                val modifierText = if (roll.modifier != 0) " ${if (roll.modifier > 0) "+" else "−"} ${abs(roll.modifier)}" else ""
                KeyValueRow(Res.string.rolls.str(), roll.rolls.joinToString(" + ") + modifierText)
                KeyValueRow(Res.string.random_dice.str(), roll.rolls.size.toString(), copyable = false)
            }
        }

        RandomMode.COIN -> {
            val heads = remember(seed) { Dice.intInRange(0, 1) == 0 }
            ResultCard {
                KeyValueRow(
                    Res.string.result.str(),
                    if (heads) Res.string.heads.str() else Res.string.tails.str(),
                    mono = false,
                    copyable = false,
                )
            }
        }

        RandomMode.LIST -> {
            val items = remember(listText) { listText.lines().map { it.trim() }.filter { it.isNotEmpty() } }
            if (items.isEmpty()) {
                ErrorText(Res.string.add_at_least_one_option.str())
                return
            }
            val picked = remember(items, seed) { Dice.pick(items) }
            val shuffled = remember(items, seed) { Dice.shuffled(items) }
            ResultCard {
                KeyValueRow(Res.string.picked.str(), picked.orEmpty(), mono = false)
                KeyValueRow(Res.string.random_options.str(), items.size.toString(), copyable = false)
            }
            OutputCard(shuffled.joinToString("\n"), title = Res.string.shuffled_order.str())
        }

        RandomMode.BYTES -> {
            val size = bytesText.trim().toIntOrNull()
            if (size == null || size !in 1..1024) {
                ErrorText(Res.string.enter_a_size_from_1_to_1024_bytes.str())
                return
            }
            val bytes = remember(size, seed) { secureRandomBytes(size) }
            ResultCard {
                KeyValueRow("hex", bytes.toHex())
                KeyValueRow("HEX", bytes.toHex(upper = true))
                KeyValueRow("Base64", Base64Tools.encode(bytes, urlSafe = false, padding = true))
                KeyValueRow("Base64 URL-safe", Base64Tools.encode(bytes, urlSafe = true, padding = false))
            }
        }
    }
}
