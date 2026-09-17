package com.vasmarfas.card.tools.network

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.Res
import com.vasmarfas.card.resources.port_number_or_service
import com.vasmarfas.card.resources.port_reference
import com.vasmarfas.card.resources.well_known_and_commonly_used_tcp_udp_ports_w
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.ToolInputField

val portsReferenceTool = Tool(
    id = "ports-reference",
    category = ToolCategory.NETWORK,
    title = Res.string.port_reference,
    description = Res.string.well_known_and_commonly_used_tcp_udp_ports_w,
    icon = Icons.AutoMirrored.Filled.ListAlt,
    keywords = listOf("tcp", "udp", "iana", "service", "порт", "сервис"),
    expandable = true,
) { PortsReferenceScreen() }

@Composable
private fun PortsReferenceScreen() {
    var query by rememberSaveable { mutableStateOf("") }
    ToolInputField(value = query, onValueChange = { query = it }, label = Res.string.port_number_or_service.str())
    val rows = WellKnownPorts.search(query)
    ResultCard(title = "${rows.size}") {
        SimpleTable(
            header = listOf("Port", "Proto", "Service", "Description"),
            rows = rows.map { listOf(it.port.toString(), it.protocol, it.service, it.description) },
            weights = listOf(0.7f, 0.7f, 1.3f, 3f),
            mono = false,
        )
    }
}
