package com.vasmarfas.card.tools.network

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.vasmarfas.card.core.AppPermission
import com.vasmarfas.card.core.InterfaceInfo
import com.vasmarfas.card.core.PlatformKind
import com.vasmarfas.card.core.currentPlatform
import com.vasmarfas.card.core.ensurePermission
import com.vasmarfas.card.core.hasPermission
import com.vasmarfas.card.core.networkInterfaces
import com.vasmarfas.card.core.str
import com.vasmarfas.card.core.wifiDetails
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SwitchRow
import kotlinx.coroutines.launch

val networkInterfacesTool = Tool(
    id = "network-interfaces",
    category = ToolCategory.NETWORK,
    title = Res.string.network_interfaces,
    description = Res.string.network_interfaces_description,
    icon = Icons.Filled.SettingsEthernet,
    keywords = listOf("ifconfig", "ipconfig", "wifi", "ssid", "gateway", "dns", "mac", "интерфейсы", "адреса"),
    platforms = PlatformKind.native,
) { NetworkInterfacesScreen() }

@Composable
private fun NetworkInterfacesScreen() {
    var showAll by remember { mutableStateOf(false) }
    var refresh by remember { mutableStateOf(0) }
    var permission by remember { mutableStateOf(hasPermission(AppPermission.LOCATION)) }
    val scope = rememberCoroutineScope()
    val interfaces by produceState(emptyList<InterfaceInfo>(), refresh) { value = networkInterfaces() }
    val wifi = remember(refresh, permission) { wifiDetails() }

    if (currentPlatform == PlatformKind.ANDROID) {
        ResultCard(title = Res.string.connection.str()) {
            if (!permission) {
                Text(Res.string.iface_ssid_and_bssid_require.str(), style = MaterialTheme.typography.bodySmall)
                ActionButton(text = Res.string.grant_permission.str(), onClick = { scope.launch { permission = ensurePermission(AppPermission.LOCATION); refresh++ } })
            }
            wifi.forEach { (k, v) -> if (v.isNotBlank()) KeyValueRow(k, v, mono = k in setOf("BSSID", "Addresses", "DNS", "Gateway")) }
        }
    }
    SwitchRow(Res.string.show_inactive_interfaces.str(), showAll, { showAll = it })
    ActionButton(text = Res.string.refresh.str(), onClick = { refresh++ })
    interfaces.filter { showAll || (it.isUp && it.addresses.isNotEmpty()) }.forEach { nif ->
        ResultCard(title = "${nif.name}" + (if (nif.displayName != nif.name) " · ${nif.displayName}" else "")) {
            nif.mac?.let { KeyValueRow("MAC", it) }
            nif.addresses.forEach { KeyValueRow(if (it.contains(':')) "IPv6" else "IPv4", it) }
            KeyValueRow("MTU", nif.mtu.toString(), copyable = false)
            KeyValueRow(Res.string.state.str(), (if (nif.isUp) "up" else "down") + (if (nif.isLoopback) " · loopback" else ""), mono = false, copyable = false)
        }
    }
}
