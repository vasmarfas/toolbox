package com.vasmarfas.card.tools.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.LocalLang
import com.vasmarfas.card.core.PlatformKind
import com.vasmarfas.card.core.currentPlatform
import com.vasmarfas.card.core.regionName
import com.vasmarfas.card.core.secureRandomBytes
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.Hint
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.LoadingRow
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.ToolInputField
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

val macLookupTool = Tool(
    id = "mac-lookup",
    category = ToolCategory.NETWORK,
    title = Res.string.mac_address_lookup,
    description = Res.string.mac_lookup_description,
    icon = Icons.Filled.Memory,
    keywords = listOf("oui", "vendor", "manufacturer", "производитель", "мак", "адрес"),
) { MacLookupScreen() }

object MacAddress {
    fun normalize(input: String): String? {
        val hex = input.trim().replace(Regex("[^0-9A-Fa-f]"), "").uppercase()
        return if (hex.length == 12) hex else null
    }

    fun prefix(input: String): String? = input.trim().replace(Regex("[^0-9A-Fa-f]"), "").uppercase().takeIf { it.length in 6..12 }

    fun colon(hex: String) = hex.chunked(2).joinToString(":")
    fun hyphen(hex: String) = hex.chunked(2).joinToString("-")
    fun dotted(hex: String) = hex.lowercase().chunked(4).joinToString(".")
    fun isMulticast(hex: String) = (hex.substring(0, 2).toInt(16) and 1) == 1
    fun isLocal(hex: String) = (hex.substring(0, 2).toInt(16) and 2) == 2
    fun random(): String {
        val bytes = secureRandomBytes(6)
        bytes[0] = ((bytes[0].toInt() and 0xFC) or 0x02).toByte()
        return bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase() }
    }
}

@Composable
private fun MacLookupScreen() {
    val lang = LocalLang.current
    var input by rememberSaveable { mutableStateOf("4C:5E:0C:12:34:56") }
    val registry by produceState<MacRegistry?>(null) { value = runCatching { MacVendors.registry() }.getOrNull() }
    val hex = MacAddress.normalize(input)
    val prefix = MacAddress.prefix(input)
    var answers by remember(prefix) { mutableStateOf<List<MacAnswer>?>(null) }
    var checking by remember(prefix) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ToolInputField(
        value = input,
        onValueChange = { input = it },
        label = Res.string.mac_address.str(),
        placeholder = "00:1A:2B:3C:4D:5E · 001a.2b3c.4d5e · 001A2B3C4D5E",
        isError = input.isNotBlank() && prefix == null,
        monospace = true,
    )
    val online = currentPlatform != PlatformKind.WEB
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (online) {
            ActionButton(
                text = Res.string.mac_check_online.str(),
                onClick = {
                    val mac = prefix ?: return@ActionButton
                    checking = true
                    scope.launch {
                        answers = MacVendors.online(mac)
                        checking = false
                    }
                },
                enabled = prefix != null && !checking,
            )
        }
        TextButton(onClick = { input = MacAddress.colon(MacAddress.random()) }) { Text(Res.string.random_mac.str()) }
    }
    if (!online) Hint(Res.string.mac_web_no_vendor.str())
    if (prefix != null) {
        val block = registry?.find(prefix)
        ResultCard {
            when {
                block != null -> {
                    KeyValueRow(Res.string.mac_vendor.str(), block.vendor, mono = false)
                    block.country?.let { KeyValueRow(Res.string.country.str(), regionName(it, lang) ?: it, mono = false, copyable = false) }
                    KeyValueRow(Res.string.mac_block.str(), "${block.type} · ${MacAddress.colon(block.first)} – ${MacAddress.colon(block.last)}", copyable = false)
                }
                registry == null -> LoadingRow()
                MacAddress.isLocal(prefix) -> KeyValueRow(Res.string.mac_vendor.str(), Res.string.mac_local_no_vendor.str(), mono = false, copyable = false)
                else -> KeyValueRow(Res.string.mac_vendor.str(), Res.string.mac_not_found_unregistered.str(), mono = false, copyable = false)
            }
            registry?.let { KeyValueRow(Res.string.source.str(), stringResource(Res.string.mac_ieee_registry, it.date), mono = false, copyable = false) }
            if (checking) LoadingRow()
            answers?.forEach { answer ->
                val vendor = answer.vendor
                val text = when {
                    answer.failed -> Res.string.mac_no_answer.str()
                    vendor == null -> Res.string.mac_not_found_short.str()
                    block != null && MacVendors.sameVendor(block.vendor, vendor) -> Res.string.mac_same.str()
                    else -> vendor
                }
                KeyValueRow(answer.source, text, mono = false, copyable = text == vendor)
            }
        }
    }
    if (hex != null) {
        ResultCard {
            KeyValueRow(Res.string.mac_format_colon.str(), MacAddress.colon(hex))
            KeyValueRow(Res.string.mac_format_hyphen.str(), MacAddress.hyphen(hex))
            KeyValueRow(Res.string.mac_format_cisco.str(), MacAddress.dotted(hex))
            KeyValueRow(Res.string.mac_format_plain.str(), hex)
            KeyValueRow("OUI", hex.substring(0, 6))
            KeyValueRow(
                Res.string.bits.str(),
                (if (MacAddress.isMulticast(hex)) "multicast" else "unicast") + " · " + (if (MacAddress.isLocal(hex)) Res.string.locally_administered.str() else Res.string.universally_administered.str()),
                mono = false,
                copyable = false,
            )
            KeyValueRow("EUI-64", hex.substring(0, 6) + "FFFE" + hex.substring(6), mono = true)
            val modified = ((hex.substring(0, 2).toInt(16) xor 0x02).toString(16).padStart(2, '0').uppercase()) + hex.substring(2, 6) + "FFFE" + hex.substring(6)
            KeyValueRow("IPv6 link-local", "fe80::" + modified.lowercase().chunked(4).joinToString(":").replace(Regex("(^|:)0+(?=[0-9a-f])"), "$1"))
        }
    }
}
