package com.vasmarfas.card.tools.developer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.vasmarfas.card.core.Tr
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.ToolInputField

private const val MAX_ROWS = 60

val mimeTypesTool = Tool(
    id = "mime-types",
    category = ToolCategory.DEVELOPER,
    title = Res.string.mime_types,
    description = Res.string.mime_types_description,
    icon = Icons.Filled.Description,
    keywords = listOf("mime", "media type", "content-type", "extension", "file type", "тип файла", "расширение", "content type"),
) { MimeTypesScreen() }

@Composable
private fun MimeTypesScreen() {
    var query by rememberSaveable { mutableStateOf("") }
    ToolInputField(
        value = query,
        onValueChange = { query = it },
        label = Res.string.extension_type_or_description.str(),
        placeholder = "docx · image/ · archive",
    )
    val found = remember(query) { MimeTypes.search(query) }
    if (found.isEmpty()) {
        Text(Res.string.nothing_found.str(), style = MaterialTheme.typography.bodyMedium)
        return
    }
    ResultCard("${found.size} ${Res.string.types.str()}") {
        found.take(MAX_ROWS).forEach { t ->
            val extensions = t.extensions.filter { it.isNotEmpty() }.joinToString(" ") { ".$it" }
            KeyValueRow(if (extensions.isEmpty()) t.description else "$extensions — ${t.description}", t.type)
        }
        if (found.size > MAX_ROWS) {
            Text(Tr("Only the first $MAX_ROWS are shown, refine the search.", "Показаны первые $MAX_ROWS, уточните запрос.").str(), style = MaterialTheme.typography.bodySmall)
        }
    }
}
