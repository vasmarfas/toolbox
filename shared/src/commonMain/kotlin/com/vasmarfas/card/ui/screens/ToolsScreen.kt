package com.vasmarfas.card.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vasmarfas.card.core.PlatformKind
import com.vasmarfas.card.core.str
import com.vasmarfas.card.data.LocalSettings
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.tools.ToolRegistry
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.ContentMaxWidth
import com.vasmarfas.card.ui.components.EmptyState
import com.vasmarfas.card.ui.components.LayoutSize
import com.vasmarfas.card.ui.components.LocalLayoutSize

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
    val collapsed = rememberSaveable(
        saver = listSaver(save = { it.toList() }, restore = { it.toMutableStateList() }),
    ) { ToolCategory.entries.map { it.id }.toMutableStateList() }
    var categoriesExpanded by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        CatalogHeader(
            query = query,
            onQueryChange = { query = it },
            selected = selectedCategory,
            onSelect = { category = it?.id },
            expanded = categoriesExpanded,
            onToggleExpanded = { categoriesExpanded = !categoriesExpanded },
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(250.dp),
            modifier = Modifier.align(Alignment.CenterHorizontally).widthIn(max = ContentMaxWidth).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (query.isBlank() && selectedCategory == null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        Res.string.tools_intro.str(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
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
                    EmptyState(
                        icon = Icons.Filled.SearchOff,
                        title = Res.string.nothing_found.str(),
                        description = Res.string.nothing_found_hint.str(),
                    ) {
                        ActionButton(
                            text = Res.string.reset_search.str(),
                            onClick = {
                                query = ""
                                category = null
                            },
                        )
                    }
                }
            }
            ToolCategory.entries.forEach { cat ->
                val tools = grouped[cat] ?: return@forEach
                val foldable = selectedCategory == null && query.isBlank()
                val folded = foldable && cat.id in collapsed
                item(span = { GridItemSpan(maxLineSpan) }, key = "header-" + cat.id) {
                    GroupHeader(
                        text = "${cat.title.str()} · ${tools.size}",
                        category = cat,
                        collapsed = folded,
                        onToggle = if (foldable) {
                            { if (folded) collapsed.remove(cat.id) else collapsed.add(cat.id) }
                        } else {
                            null
                        },
                    )
                }
                if (!folded) items(tools, key = { it.id }) { ToolCard(it, onOpenTool) }
            }
            item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CatalogHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    selected: ToolCategory?,
    onSelect: (ToolCategory?) -> Unit,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
) {
    val categories = remember { listOf<ToolCategory?>(null) + ToolCategory.entries.filter { ToolRegistry.byCategory(it).isNotEmpty() } }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                Res.string.tools.str(),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            )
            Row(verticalAlignment = if (expanded) Alignment.Top else Alignment.CenterVertically) {
                if (expanded) {
                    FlowRow(
                        modifier = Modifier.weight(1f).padding(start = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        categories.forEach { CategoryChip(it, selected, onSelect) }
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(categories, key = { it?.id ?: "all" }) { CategoryChip(it, selected, onSelect) }
                    }
                }
                IconButton(
                    onClick = onToggleExpanded,
                    modifier = Modifier.padding(end = 4.dp).pointerHoverIcon(PointerIcon.Hand),
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) {
                            Res.string.show_fewer_categories.str()
                        } else {
                            Res.string.show_all_categories.str()
                        },
                    )
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                singleLine = true,
                placeholder = { Text(Res.string.search_tools.str()) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = Res.string.clear.str())
                        }
                    }
                },
                shape = CircleShape,
            )
        }
    }
}

@Composable
private fun CategoryChip(option: ToolCategory?, selected: ToolCategory?, onSelect: (ToolCategory?) -> Unit) {
    FilterChip(
        selected = option == selected,
        onClick = { onSelect(option) },
        label = { Text(option?.title?.str() ?: Res.string.all_tools.str(), maxLines = 1) },
        leadingIcon = {
            Icon(option?.icon ?: Icons.Filled.Apps, contentDescription = null, modifier = Modifier.size(18.dp))
        },
        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
    )
}

@Composable
private fun GroupHeader(
    text: String,
    category: ToolCategory? = null,
    collapsed: Boolean = false,
    onToggle: (() -> Unit)? = null,
) {
    val row = Modifier
        .fillMaxWidth()
        .padding(top = 12.dp)
        .then(
            if (onToggle == null) {
                Modifier
            } else {
                Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(onClick = onToggle)
                    .pointerHoverIcon(PointerIcon.Hand)
            },
        )
        .padding(vertical = 4.dp)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = row) {
        if (category != null) {
            Icon(category.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (onToggle != null) {
            Icon(
                imageVector = if (collapsed) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                contentDescription = if (collapsed) Res.string.expand.str() else Res.string.collapse.str(),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun ToolCard(tool: Tool, onOpenTool: (String) -> Unit, modifier: Modifier = Modifier) {
    val available = tool.availableHere
    val typography = MaterialTheme.typography
    val lines = (typography.titleMedium.lineHeight.value + typography.bodySmall.lineHeight.value) * 2 +
        typography.labelSmall.lineHeight.value
    val textHeight = with(LocalDensity.current) { lines.sp.toDp() }
    val uniform = LocalLayoutSize.current != LayoutSize.COMPACT
    val platforms = if (tool.platforms.size < PlatformKind.all.size) {
        PlatformKind.entries.filter { it in tool.platforms }.map { it.title.str() }.joinToString(" · ")
    } else {
        null
    }
    Card(
        onClick = { onOpenTool(tool.id) },
        modifier = modifier.fillMaxWidth().pointerHoverIcon(PointerIcon.Hand),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).alpha(if (available) 1f else 0.6f),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        tool.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f).then(if (uniform) Modifier.height(textHeight + 4.dp) else Modifier),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    tool.title.str(),
                    style = typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    tool.description.str(),
                    style = typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (platforms != null) {
                    Text(
                        platforms,
                        style = typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
