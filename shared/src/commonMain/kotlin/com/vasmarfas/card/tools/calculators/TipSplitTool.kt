package com.vasmarfas.card.tools.calculators

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.vasmarfas.card.core.fmt
import com.vasmarfas.card.core.str
import com.vasmarfas.card.core.toDoubleLenient
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SwitchRow

private val tipPresets = listOf(5, 10, 12, 15, 18, 20, 25)

val tipSplitTool = Tool(
    id = "tip-split",
    category = ToolCategory.CALCULATORS,
    title = Res.string.tip_and_bill_split,
    description = Res.string.tip_amount_total_and_the_share_per_person_op,
    icon = Icons.Filled.Group,
    keywords = listOf("tip", "bill", "split", "restaurant", "чаевые", "счёт", "разделить", "ресторан"),
) { TipSplitScreen() }

@Composable
private fun TipSplitScreen() {
    var billText by rememberSaveable { mutableStateOf("2500") }
    var tipText by rememberSaveable { mutableStateOf("10") }
    var peopleText by rememberSaveable { mutableStateOf("2") }
    var roundUp by rememberSaveable { mutableStateOf(false) }
    val bill = billText.toDoubleLenient()
    val tip = tipText.toDoubleLenient()
    val people = peopleText.trim().toIntOrNull()?.takeIf { it >= 1 }
    NumberField(
        value = billText,
        onValueChange = { billText = it },
        label = Res.string.bill.str(),
        isError = billText.isNotBlank() && bill == null,
    )
    NumberField(
        value = tipText,
        onValueChange = { tipText = it },
        label = Res.string.tip.str(),
        suffix = "%",
        isError = tipText.isNotBlank() && tip == null,
    )
    ChoiceChips(
        options = tipPresets,
        selected = tip?.toInt()?.takeIf { it.toDouble() == tip },
        onSelect = { tipText = it.toString() },
        label = { "$it%" },
    )
    NumberField(
        value = peopleText,
        onValueChange = { peopleText = it },
        label = Res.string.people.str(),
        isError = peopleText.isNotBlank() && people == null,
    )
    SwitchRow(
        label = Res.string.round_each_share_up_to_a_whole_number.str(),
        checked = roundUp,
        onCheckedChange = { roundUp = it },
    )
    if (bill != null && tip != null && people != null) {
        val result = TipSplit.compute(bill, tip, people, roundUp)
        ResultCard {
            KeyValueRow(Res.string.tip.str(), result.tip.fmt(2, grouping = true))
            KeyValueRow(Res.string.total.str(), result.total.fmt(2, grouping = true))
            KeyValueRow(Res.string.per_person.str(), result.perPerson.fmt(2, grouping = true))
            KeyValueRow(Res.string.tip_per_person.str(), result.tipPerPerson.fmt(2, grouping = true))
        }
    }
}
