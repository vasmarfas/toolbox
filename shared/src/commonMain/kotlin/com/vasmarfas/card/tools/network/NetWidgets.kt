package com.vasmarfas.card.tools.network

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.data.LocalSettings
import com.vasmarfas.card.resources.*

object NetStrings {
    val host = Res.string.host_or_ip_address
    val port = Res.string.port
    val timeout = Res.string.timeout_ms
    val count = Res.string.count
    val running = Res.string.running_2
    val noResults = Res.string.no_results
    val query = Res.string.query_2
    val lookup = Res.string.lookup
    val raw = Res.string.raw_response
    val sent = Res.string.sent
    val received = Res.string.received
    val lost = Res.string.lost
    val min = Res.string.min_2
    val avg = Res.string.avg
    val max = Res.string.max
    val open = Res.string.open_3
    val closed = Res.string.closed
    val service = Res.string.service
    val scanning = Res.string.scanning
    val stop = Res.string.stop_3
    val start = Res.string.start
    val jvmOnlyNote = Res.string.the_browser_has_no_sockets_this_works_in_the
    val corsNote = Res.string.in_the_browser_requests_are_limited_by_cors
}

private const val WIDE_COLUMN_UNIT = 110

@Composable
fun TableRow(cells: List<String>, header: Boolean = false, weights: List<Float>? = null, mono: Boolean = true) {
    val wide = LocalSettings.current.wideTables
    val style = when {
        header -> MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
        mono -> MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
        else -> MaterialTheme.typography.bodySmall
    }
    val color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = if (wide) Modifier.padding(vertical = 4.dp) else Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        cells.forEachIndexed { index, cell ->
            val weight = weights?.getOrNull(index) ?: 1f
            val cellModifier = if (wide) Modifier.width((WIDE_COLUMN_UNIT * weight).dp) else Modifier.weight(weight)
            SelectionContainer(cellModifier) {
                Text(cell, style = style, color = color, softWrap = !wide, maxLines = if (wide) 1 else Int.MAX_VALUE)
            }
        }
    }
}

@Composable
fun TableBlock(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val wide = LocalSettings.current.wideTables
    val scroll = rememberScrollState()
    Column(
        modifier = if (wide) modifier.fillMaxWidth().horizontalScroll(scroll) else modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content,
    )
}

@Composable
fun SimpleTable(header: List<String>, rows: List<List<String>>, weights: List<Float>? = null, mono: Boolean = true) {
    TableBlock {
        TableRow(header, header = true, weights = weights)
        HorizontalDivider()
        rows.forEach { TableRow(it, weights = weights, mono = mono) }
    }
}

fun looksLikeIp(text: String): Boolean = Ipv4.parse(text) != null || Ipv6Address.parse(text) != null

fun hostFrom(input: String): String {
    var t = input.trim()
    if (t.contains("://")) t = t.substringAfter("://")
    t = t.substringBefore('/').substringBefore('?')
    if (t.startsWith("[")) return t.substringBefore(']').trimStart('[')
    if (t.count { it == ':' } == 1) t = t.substringBefore(':')
    return t
}
