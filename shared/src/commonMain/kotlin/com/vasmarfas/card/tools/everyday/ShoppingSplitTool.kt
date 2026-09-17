package com.vasmarfas.card.tools.everyday

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.Prefs
import com.vasmarfas.card.core.fmt
import com.vasmarfas.card.core.str
import com.vasmarfas.card.core.toDoubleLenient
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.ToolInputField

val shoppingSplitTool = Tool(
    id = "shopping-split",
    category = ToolCategory.EVERYDAY,
    title = Res.string.split_expenses,
    description = Res.string.who_paid_how_much_for_a_shared_purchase_or_t,
    icon = Icons.Filled.Groups,
    keywords = listOf("split bill", "expenses", "settle", "debts", "trip", "разделить счёт", "расходы", "долги", "скинуться"),
) { ShoppingSplitScreen() }

@Composable
private fun ShoppingSplitScreen() {
    val people = remember {
        mutableStateListOf<SplitPerson>().apply {
            addAll(Split.decode(Prefs.store.get(Split.PREF_KEY)) ?: listOf(SplitPerson(), SplitPerson()))
        }
    }
    fun persist() = Prefs.store.put(Split.PREF_KEY, Split.encode(people))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        people.forEachIndexed { index, person ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                ToolInputField(
                    value = person.name,
                    onValueChange = {
                        people[index] = person.copy(name = it)
                        persist()
                    },
                    label = "${Res.string.name.str()} ${index + 1}",
                    modifier = Modifier.weight(1f),
                )
                NumberField(
                    value = person.paid,
                    onValueChange = {
                        people[index] = person.copy(paid = it)
                        persist()
                    },
                    label = Res.string.paid.str(),
                    modifier = Modifier.weight(1f),
                    isError = person.paid.isNotBlank() && person.paid.toDoubleLenient() == null,
                )
                IconButton(
                    onClick = {
                        people.removeAt(index)
                        persist()
                    },
                    enabled = people.size > 2,
                ) {
                    Icon(Icons.Filled.Close, contentDescription = Res.string.remove.str())
                }
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionButton(
            text = Res.string.add_person.str(),
            onClick = {
                people.add(SplitPerson())
                persist()
            },
            icon = Icons.Filled.Add,
        )
        ActionButton(
            text = Res.string.clear.str(),
            onClick = {
                people.clear()
                people.addAll(listOf(SplitPerson(), SplitPerson()))
                persist()
            },
        )
    }

    val amounts = people.map { it.paid.ifBlank { "0" }.toDoubleLenient() }
    if (amounts.any { it == null }) {
        ErrorText(Res.string.check_the_amounts.str())
        return
    }
    val total = amounts.sumOf { it!! }
    val share = total / people.size
    val names = people.mapIndexed { i, p -> p.name.ifBlank { "${Res.string.person.str()} ${i + 1}" } }
    val balances = names.mapIndexed { i, name -> name to amounts[i]!! - share }
    val transfers = Split.settle(balances)
    ResultCard {
        KeyValueRow(Res.string.total_2.str(), total.fmt(2, grouping = true))
        KeyValueRow(Res.string.share_per_person.str(), share.fmt(2, grouping = true))
        balances.forEach { (name, balance) ->
            KeyValueRow(
                name,
                when {
                    balance > 0.005 -> "+${balance.fmt(2)} · ${Res.string.gets_back.str()}"
                    balance < -0.005 -> "${balance.fmt(2)} · ${Res.string.owes.str()}"
                    else -> Res.string.settled.str()
                },
                mono = false,
                copyable = false,
            )
        }
    }
    if (transfers.isNotEmpty()) {
        ResultCard(Res.string.transfers.str()) {
            transfers.forEach { t ->
                Text("${t.from} → ${t.to}: ${t.amount.fmt(2)}")
            }
        }
    }
}
