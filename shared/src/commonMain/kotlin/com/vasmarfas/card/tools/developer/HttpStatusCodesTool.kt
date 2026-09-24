package com.vasmarfas.card.tools.developer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Http
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.LocalLang
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.ToolInputField

val httpStatusCodesTool = Tool(
    id = "http-status-codes",
    category = ToolCategory.DEVELOPER,
    title = Res.string.http_status_codes,
    description = Res.string.http_status_codes_description,
    icon = Icons.Filled.Http,
    keywords = listOf("http", "status", "code", "404", "500", "response", "rest", "код ответа", "статус", "ошибка"),
) { HttpStatusCodesScreen() }

@Composable
private fun HttpStatusCodesScreen() {
    var query by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableStateOf(0) }
    val lang = LocalLang.current
    ToolInputField(
        value = query,
        onValueChange = { query = it },
        label = Res.string.search_by_code_name_or_description.str(),
        placeholder = "404 · timeout · redirect",
    )
    ChoiceChips(
        options = listOf(0, 1, 2, 3, 4, 5),
        selected = group,
        onSelect = { group = it },
        label = { if (it == 0) Res.string.all.str() else "${it}xx" },
    )
    val found = remember(query, group, lang) { HttpStatuses.search(query).filter { group == 0 || it.group == group } }
    if (found.isEmpty()) {
        Text(Res.string.nothing_found.str(), style = MaterialTheme.typography.bodyMedium)
        return
    }
    found.groupBy { it.group }.forEach { (g, statuses) ->
        ResultCard(HttpStatuses.groupTitles.getValue(g).str()) {
            statuses.forEach { s ->
                Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text("${s.code} ${s.name}", style = MaterialTheme.typography.titleSmall)
                    Text(s.description.str(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
