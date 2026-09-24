package com.vasmarfas.card.tools.developer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.tools.text.OutputCard
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.MonoTable
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SwitchRow
import com.vasmarfas.card.ui.components.ToolInputField
import org.jetbrains.compose.resources.StringResource

private enum class UrlMode(val title: StringResource) {
    ENCODE(Res.string.encode),
    DECODE(Res.string.decode),
    PARSE(Res.string.parse),
    QUERY(Res.string.query_builder),
    IDN(Res.string.idn_punycode),
}

val urlToolsTool = Tool(
    id = "url-tools",
    category = ToolCategory.DEVELOPER,
    title = Res.string.url_tools,
    description = Res.string.url_tools_description,
    icon = Icons.Filled.Link,
    keywords = listOf("url", "uri", "encode", "decode", "percent", "query", "punycode", "idn", "parse", "урл", "ссылка", "кодирование", "запрос"),
) { UrlToolsScreen() }

@Composable
private fun UrlToolsScreen() {
    var mode by rememberSaveable { mutableStateOf(UrlMode.PARSE) }
    ChoiceChips(options = UrlMode.entries, selected = mode, onSelect = { mode = it }, label = { it.title.str() })
    when (mode) {
        UrlMode.ENCODE -> EncodeSection()
        UrlMode.DECODE -> DecodeSection()
        UrlMode.PARSE -> ParseSection()
        UrlMode.QUERY -> QuerySection()
        UrlMode.IDN -> IdnSection()
    }
}

@Composable
private fun EncodeSection() {
    var input by rememberSaveable { mutableStateOf("") }
    var component by rememberSaveable { mutableStateOf(true) }
    var spaceAsPlus by rememberSaveable { mutableStateOf(false) }
    ToolInputField(value = input, onValueChange = { input = it }, label = Res.string.text.str(), singleLine = false, minLines = 3)
    SwitchRow(
        Res.string.encode_as_component_also.str(),
        component,
        { component = it },
        description = Res.string.url_off_keep_url_delimiters.str(),
    )
    if (component) SwitchRow(Res.string.space_as_form_encoding.str(), spaceAsPlus, { spaceAsPlus = it })
    if (input.isNotEmpty()) OutputCard(if (component) UrlCodec.encodeComponent(input, spaceAsPlus) else UrlCodec.encodeFull(input))
}

@Composable
private fun DecodeSection() {
    var input by rememberSaveable { mutableStateOf("") }
    var plusAsSpace by rememberSaveable { mutableStateOf(true) }
    ToolInputField(value = input, onValueChange = { input = it }, label = Res.string.encoded_text.str(), singleLine = false, minLines = 3, monospace = true)
    SwitchRow(Res.string.as_space.str(), plusAsSpace, { plusAsSpace = it })
    if (input.isNotEmpty()) {
        val decoded = remember(input, plusAsSpace) { UrlCodec.decode(input, plusAsSpace) }
        if (decoded == null) ErrorText(Res.string.malformed_percent_encoding.str()) else OutputCard(decoded)
    }
}

@Composable
private fun ParseSection() {
    var input by rememberSaveable { mutableStateOf("https://user:pass@example.com:8443/path/to/page?q=kotlin&lang=ru#top") }
    val parsed = remember(input) { UrlCodec.parse(input) }
    ToolInputField(
        value = input,
        onValueChange = { input = it },
        label = "URL",
        keyboardType = KeyboardType.Uri,
        isError = input.isNotBlank() && parsed == null,
        monospace = true,
    )
    if (parsed == null) {
        if (input.isNotBlank()) ErrorText(Res.string.cannot_parse_this_url.str())
        return
    }
    val none = "—"
    ResultCard {
        KeyValueRow(Res.string.scheme.str(), parsed.scheme ?: none)
        KeyValueRow(Res.string.user_info.str(), parsed.userInfo ?: none)
        KeyValueRow(Res.string.host.str(), parsed.host ?: none)
        val portText = parsed.port?.toString() ?: parsed.defaultPort?.let { "$it (${Res.string.default.str()})" } ?: none
        KeyValueRow(Res.string.port.str(), portText)
        KeyValueRow(Res.string.path.str(), parsed.path.ifEmpty { "/" })
        KeyValueRow(Res.string.query.str(), parsed.query ?: none)
        KeyValueRow(Res.string.fragment.str(), parsed.fragment ?: none)
        if (parsed.scheme != null && parsed.host != null) {
            KeyValueRow(Res.string.origin.str(), parsed.scheme + "://" + parsed.host + (parsed.port?.let { ":$it" } ?: ""))
        }
    }
    if (parsed.segments.isNotEmpty()) {
        ResultCard(Res.string.path_segments.str()) {
            MonoTable(parsed.segments.mapIndexed { i, s -> "${(i + 1).toString().padStart(2)}  $s" })
        }
    }
    if (parsed.params.isNotEmpty()) {
        ResultCard(Res.string.query_parameters.str()) {
            parsed.params.forEach { (k, v) -> KeyValueRow(k, v) }
        }
    }
}

@Composable
private fun QuerySection() {
    var base by rememberSaveable { mutableStateOf("https://example.com/search") }
    val params = remember { mutableStateListOf("q" to "", "page" to "1") }
    ToolInputField(value = base, onValueChange = { base = it }, label = Res.string.base_url.str(), keyboardType = KeyboardType.Uri, monospace = true)
    params.forEachIndexed { index, (key, value) ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ToolInputField(value = key, onValueChange = { params[index] = it to value }, label = Res.string.key.str(), modifier = Modifier.weight(1f))
            ToolInputField(value = value, onValueChange = { params[index] = key to it }, label = Res.string.value_.str(), modifier = Modifier.weight(1f))
            IconButton(onClick = { params.removeAt(index) }) {
                Icon(Icons.Filled.Delete, contentDescription = Res.string.remove.str())
            }
        }
    }
    ActionButton(Res.string.add_parameter.str(), onClick = { params.add("" to "") }, icon = Icons.Filled.Add)
    val query = UrlCodec.buildQuery(params.toList())
    val separator = if (base.contains('?')) "&" else "?"
    OutputCard(if (query.isEmpty()) base else base + separator + query)
}

@Composable
private fun IdnSection() {
    var input by rememberSaveable { mutableStateOf("münchen.de") }
    ToolInputField(value = input, onValueChange = { input = it }, label = Res.string.host_name.str(), keyboardType = KeyboardType.Uri, placeholder = "пример.рф · xn--e1afmkfd.xn--p1ai")
    if (input.isBlank()) return
    val ascii = remember(input) { Punycode.toAscii(input) }
    val unicode = remember(input) { Punycode.toUnicode(input) }
    ResultCard {
        KeyValueRow("ASCII (punycode)", ascii)
        if (unicode == null) ErrorText(Res.string.invalid_punycode_label.str()) else KeyValueRow("Unicode", unicode)
    }
}
