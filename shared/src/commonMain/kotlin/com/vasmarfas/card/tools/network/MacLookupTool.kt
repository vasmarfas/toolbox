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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.Net
import com.vasmarfas.card.core.secureRandomBytes
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.LoadingRow
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.ToolInputField
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

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
    val notFoundUnregisteredOrRandomizedText = Res.string.mac_not_found_unregistered.str()
    var input by rememberSaveable { mutableStateOf("4C:5E:0C:12:34:56") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var vendor by remember { mutableStateOf<List<Pair<String, String>>?>(null) }
    val scope = rememberCoroutineScope()
    val hex = MacAddress.normalize(input)

    fun lookup() {
        val mac = hex ?: return
        loading = true; error = null; vendor = null
        scope.launch {
            runCatching {
                val text = Net.client.get("https://api.maclookup.app/v2/macs/${MacAddress.colon(mac)}").bodyAsText()
                val json = Net.json.parseToJsonElement(text).jsonObject
                fun s(k: String) = (json[k] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() && it != "null" }
                if (s("found") == "false") listOf("Vendor" to notFoundUnregisteredOrRandomizedText)
                else listOfNotNull(
                    s("company")?.let { "Vendor" to it },
                    s("address")?.let { "Address" to it },
                    s("country")?.let { "Country" to it },
                    s("macPrefix")?.let { "Prefix" to it },
                    s("blockStart")?.let { "Block" to "$it – ${s("blockEnd") ?: ""} (${s("blockType") ?: ""})" },
                    s("updated")?.let { "Updated" to it },
                )
            }.onSuccess { vendor = it }.onFailure { error = it.message ?: it.toString() }
            loading = false
        }
    }

    ToolInputField(
        value = input,
        onValueChange = { input = it },
        label = Res.string.mac_address.str(),
        placeholder = "00:1A:2B:3C:4D:5E · 001a.2b3c.4d5e · 001A2B3C4D5E",
        isError = input.isNotBlank() && hex == null,
        monospace = true,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionButton(text = Res.string.find_vendor.str(), onClick = ::lookup, enabled = hex != null && !loading)
        TextButton(onClick = { input = MacAddress.colon(MacAddress.random()); vendor = null }) { Text(Res.string.random_mac.str()) }
    }
    if (loading) LoadingRow()
    error?.let { ErrorText(it) }
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
            vendor?.forEach { (k, v) -> KeyValueRow(k, v, mono = false) }
        }
    }
}
