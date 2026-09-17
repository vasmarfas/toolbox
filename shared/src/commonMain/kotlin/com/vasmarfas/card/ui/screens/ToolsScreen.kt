package com.vasmarfas.card.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.str
import com.vasmarfas.card.data.LocalSettings
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.tools.ToolRegistry
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.ContentMaxWidth
import com.vasmarfas.card.ui.components.PlatformBadges

@Composable
fun ToolsScreen(onOpenTool: (String) -> Unit) {
    val settings = LocalSettings.current
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedCategory = category?.let { id -> ToolCategory.entries.firstOrNull { it.id == id } }
    val filtered = remember(query, selectedCategory) {
        ToolRegistry.all.filter { (selectedCategory == null || it.category == selectedCategory) && it.matches(query) }
    }
    val favorites = filtered.filter { it.id in settings.favorites }
    val recent = settings.recent.mapNotNull { ToolRegistry.byId(it) }.filter { it in filtered && it.id !in settings.favorites }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(250.dp),
            modifier = Modifier.widthIn(max = ContentMaxWidth).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(Res.string.tools.str(), style = MaterialTheme.typography.headlineMedium)
                    Text(Res.string.tools_intro.str(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text(Res.string.search_tools.str()) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, contentDescription = Res.string.clear.str()) }
                            }
                        },
                        shape = CircleShape,
                    )
                    ChoiceChips(
                        options = listOf<ToolCategory?>(null) + ToolCategory.entries.filter { ToolRegistry.byCategory(it).isNotEmpty() },
                        selected = selectedCategory,
                        onSelect = { category = it?.id },
                        label = { it?.title?.str() ?: Res.string.all_tools.str() },
                        icon = { it?.icon ?: Icons.Filled.Star },
                    )
                }
            }
            if (favorites.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { GroupHeader(Res.string.favorites.str()) }
                items(favorites, key = { "fav-" + it.id }) { ToolCard(it, onOpenTool) }
            }
            if (recent.isNotEmpty() && query.isBlank() && selectedCategory == null) {
                item(span = { GridItemSpan(maxLineSpan) }) { GroupHeader(Res.string.recent.str()) }
                items(recent, key = { "recent-" + it.id }) { ToolCard(it, onOpenTool) }
            }
            val grouped = filtered.groupBy { it.category }
            if (grouped.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(Res.string.nothing_found.str(), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 24.dp))
                }
            }
            ToolCategory.entries.forEach { cat ->
                val tools = grouped[cat] ?: return@forEach
                item(span = { GridItemSpan(maxLineSpan) }) { GroupHeader("${cat.title.str()} · ${tools.size}", cat) }
                items(tools, key = { it.id }) { ToolCard(it, onOpenTool) }
            }
            item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun GroupHeader(text: String, category: ToolCategory? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
        if (category != null) {
            Icon(category.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun ToolCard(tool: Tool, onOpenTool: (String) -> Unit, modifier: Modifier = Modifier) {
    val available = tool.availableHere
    Card(
        onClick = { onOpenTool(tool.id) },
        modifier = modifier.fillMaxWidth().pointerHoverIcon(PointerIcon.Hand),
        colors = CardDefaults.cardColors(
            containerColor = if (available) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Surface(
                shape = CircleShape,
                color = if (available) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        tool.icon,
                        contentDescription = null,
                        tint = if (available) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(tool.title.str(), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    tool.description.str(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (tool.platforms.size < 4) {
                    PlatformBadges(tool.platforms, Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}
