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
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.DropdownChoice
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.MonoTable
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.ToolSection
import org.jetbrains.compose.resources.StringResource

val clothingSizesTool = Tool(
    id = "clothing-sizes",
    category = ToolCategory.CONVERTERS,
    title = Res.string.clothing_and_shoe_sizes,
    description = Res.string.clothing_sizes_description,
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
            label = Res.string.system_title.str(),
            text = { columnName(table.columns[it]) },
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
                KeyValueRow(columnName(name), row[i], copyable = false)
            }
        }
    }
    ToolSection(table.title.str()) {
        MonoTable(table.lines(table.columns.map { headerName(it) }))
    }
}

private val columnNames: Map<String, StringResource> = mapOf(
    "Bust" to Res.string.clothing_column_bust_cm,
    "Chest" to Res.string.clothing_column_chest_cm,
    "Waist" to Res.string.clothing_column_waist_cm,
    "Hips" to Res.string.clothing_column_hips_cm,
    "cm" to Res.string.clothing_column_foot_cm,
    "INT" to Res.string.clothing_column_international,
)

@Composable
private fun columnName(column: String): String = columnNames[column]?.str() ?: column

private val headerNames: Map<String, StringResource> = mapOf(
    "Bust" to Res.string.clothing_header_bust,
    "Chest" to Res.string.clothing_header_bust,
    "Waist" to Res.string.clothing_header_waist,
    "Hips" to Res.string.clothing_header_hips,
    "cm" to Res.string.unit_cm,
)

@Composable
private fun headerName(column: String): String = headerNames[column]?.str() ?: column

