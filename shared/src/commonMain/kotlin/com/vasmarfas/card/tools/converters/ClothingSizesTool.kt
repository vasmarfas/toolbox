package com.vasmarfas.card.tools.converters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.Res
import com.vasmarfas.card.resources.clothing_and_shoe_sizes
import com.vasmarfas.card.resources.ru_eu_us_uk_and_international_sizes_for_men
import com.vasmarfas.card.resources.size
import com.vasmarfas.card.resources.system_2
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.DropdownChoice
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.MonoTable
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.ToolSection

val clothingSizesTool = Tool(
    id = "clothing-sizes",
    category = ToolCategory.CONVERTERS,
    title = Res.string.clothing_and_shoe_sizes,
    description = Res.string.ru_eu_us_uk_and_international_sizes_for_men,
    icon = Icons.Filled.Checkroom,
    keywords = listOf("clothes", "shoes", "size", "chart", "одежда", "обувь", "размер", "таблица", "xl", "стопа"),
) { ClothingSizesScreen() }

@Composable
private fun ClothingSizesScreen() {
    var tableIndex by rememberSaveable { mutableStateOf(0) }
    var column by rememberSaveable { mutableStateOf(0) }
    var selected by rememberSaveable { mutableStateOf("") }
    val table = ClothingSizes.tables[tableIndex]
    val columnIndex = column.coerceIn(0, table.columns.lastIndex)
    val values = table.values(columnIndex)
    val value = if (selected in values) selected else values[values.size / 2]
    ChoiceChips(
        options = ClothingSizes.tables.indices.toList(),
        selected = tableIndex,
        onSelect = {
            tableIndex = it
            column = 0
            selected = ""
        },
        label = { ClothingSizes.tables[it].title.str() },
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DropdownChoice(
            options = table.columns.indices.toList(),
            selected = columnIndex,
            onSelect = {
                column = it
                selected = ""
            },
            label = Res.string.system_2.str(),
            text = { table.columns[it] },
            modifier = Modifier.weight(1f),
        )
        DropdownChoice(
            options = values,
            selected = value,
            onSelect = { selected = it },
            label = Res.string.size.str(),
            text = { it },
            modifier = Modifier.weight(1f),
        )
    }
    table.lookup(columnIndex, value).forEach { row ->
        ResultCard {
            table.columns.forEachIndexed { i, name ->
                KeyValueRow(name, row[i], copyable = false)
            }
        }
    }
    ToolSection(table.title.str()) {
        MonoTable(table.lines())
    }
}
