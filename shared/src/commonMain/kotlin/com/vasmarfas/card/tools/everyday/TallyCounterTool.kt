package com.vasmarfas.card.tools.everyday

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.Prefs
import com.vasmarfas.card.core.str
import com.vasmarfas.card.core.vibrate
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.ToolInputField

val tallyCounterTool = Tool(
    id = "tally-counter",
    category = ToolCategory.EVERYDAY,
    title = Res.string.tally_counter,
    description = Res.string.tally_counter_description,
    icon = Icons.Filled.Add,
    keywords = listOf("counter", "tally", "count", "clicker", "счётчик", "кликер", "подсчёт"),
) { TallyCounterScreen() }

@Composable
private fun TallyCounterScreen() {
    val defaultName = Res.string.counter.str()
    val counters = remember {
        mutableStateListOf<Counter>().apply {
            addAll(Tally.decode(Prefs.store.get(Tally.PREF_KEY)) ?: listOf(Counter(defaultName)))
        }
    }
    fun persist() = Prefs.store.put(Tally.PREF_KEY, Tally.encode(counters))
    var newName by rememberSaveable { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        counters.forEachIndexed { index, counter ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(counter.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = counter.value.toString(),
                            style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Monospace),
                        )
                    }
                    FilledTonalIconButton(
                        onClick = {
                            counters[index] = counter.copy(value = counter.value - 1)
                            persist()
                            vibrate(20)
                        },
                    ) { Icon(Icons.Filled.Remove, contentDescription = "−1") }
                    FilledTonalIconButton(
                        onClick = {
                            counters[index] = counter.copy(value = counter.value + 1)
                            persist()
                            vibrate(20)
                        },
                    ) { Icon(Icons.Filled.Add, contentDescription = "+1") }
                    IconButton(
                        onClick = {
                            counters[index] = counter.copy(value = 0)
                            persist()
                        },
                        enabled = counter.value != 0,
                    ) { Icon(Icons.Filled.Refresh, contentDescription = Res.string.reset.str()) }
                    IconButton(
                        onClick = {
                            counters.removeAt(index)
                            persist()
                        },
                    ) { Icon(Icons.Filled.Delete, contentDescription = Res.string.delete.str()) }
                }
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        ToolInputField(
            value = newName,
            onValueChange = { newName = it },
            label = Res.string.new_counter_name.str(),
            modifier = Modifier.weight(1f),
        )
        ActionButton(
            text = Res.string.add.str(),
            onClick = {
                counters.add(Counter(newName.trim().ifEmpty { "$defaultName ${counters.size + 1}" }))
                persist()
                newName = ""
            },
            icon = Icons.Filled.Add,
        )
    }
}
