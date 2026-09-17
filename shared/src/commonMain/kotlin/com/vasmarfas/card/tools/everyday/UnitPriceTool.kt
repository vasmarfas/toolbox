package com.vasmarfas.card.tools.everyday

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.fmt
import com.vasmarfas.card.core.str
import com.vasmarfas.card.core.toDoubleLenient
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.DropdownChoice
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ToolInputField

private const val MAX_ITEMS = 5

val unitPriceTool = Tool(
    id = "unit-price",
    category = ToolCategory.EVERYDAY,
    title = Res.string.unit_price,
    description = Res.string.compare_up_to_five_products_by_price_per_kil,
    icon = Icons.Filled.PriceCheck,
    keywords = listOf("price per kg", "compare", "shopping", "cheapest", "цена за кг", "сравнить", "покупки", "выгодно"),
) { UnitPriceScreen() }

@Composable
private fun UnitPriceScreen() {
    val items = remember { mutableStateListOf(PriceItem(unit = QuantityUnit.G), PriceItem(unit = QuantityUnit.G)) }
    val results = items.mapIndexedNotNull { index, item ->
        val price = item.price.toDoubleLenient() ?: return@mapIndexedNotNull null
        val qty = item.quantity.toDoubleLenient() ?: return@mapIndexedNotNull null
        UnitPrice.perBaseUnit(price, qty, item.unit)?.let { UnitPriceResult(index, it, item.unit.dimension) }
    }
    val best = UnitPrice.cheapest(results)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { index, item ->
            val result = results.firstOrNull { it.index == index }
            val isBest = result != null && best[result.dimension]?.index == index && results.count { it.dimension == result.dimension } > 1
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isBest) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ToolInputField(
                            value = item.name,
                            onValueChange = { items[index] = item.copy(name = it) },
                            label = "${Res.string.product.str()} ${index + 1}",
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { items.removeAt(index) }, enabled = items.size > 2) {
                            Icon(Icons.Filled.Close, contentDescription = Res.string.remove.str())
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        NumberField(
                            value = item.price,
                            onValueChange = { items[index] = item.copy(price = it) },
                            label = Res.string.price.str(),
                            modifier = Modifier.weight(1f),
                            isError = item.price.isNotBlank() && item.price.toDoubleLenient() == null,
                        )
                        NumberField(
                            value = item.quantity,
                            onValueChange = { items[index] = item.copy(quantity = it) },
                            label = Res.string.quantity.str(),
                            modifier = Modifier.weight(1f),
                            isError = item.quantity.isNotBlank() && (item.quantity.toDoubleLenient() ?: 0.0) <= 0.0,
                        )
                        DropdownChoice(
                            options = QuantityUnit.entries,
                            selected = item.unit,
                            onSelect = { items[index] = item.copy(unit = it) },
                            label = Res.string.unit_2.str(),
                            text = { it.label },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (result != null) {
                        val perHundred = if (result.dimension != Dimension.COUNT) " · ${(result.perBase / 10).fmt(2)} / 100 ${if (result.dimension == Dimension.MASS) "g" else "ml"}" else ""
                        val extra = best[result.dimension]?.let { UnitPrice.extraPercent(result.perBase, it.perBase) } ?: 0.0
                        Text(
                            text = "${result.perBase.fmt(2)} / ${result.dimension.baseLabel}$perHundred" + when {
                                isBest -> " · " + Res.string.cheapest.str()
                                extra > 0.005 -> " · +${extra.fmt(1)} %"
                                else -> ""
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isBest) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
    if (items.size < MAX_ITEMS) {
        ActionButton(
            text = Res.string.add_product.str(),
            onClick = { items.add(PriceItem(unit = items.last().unit)) },
            icon = Icons.Filled.Add,
        )
    }
}
