package com.vasmarfas.card.tools.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.Net
import com.vasmarfas.card.core.openUrl
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.Res
import com.vasmarfas.card.resources.ip_address_or_hostname_empty_my_ip
import com.vasmarfas.card.resources.my_ip
import com.vasmarfas.card.resources.my_ip_and_ip_lookup
import com.vasmarfas.card.resources.public_ip_address_geolocation_isp_asn_and_ti
import com.vasmarfas.card.resources.show_on_map
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

val ipInfoTool = Tool(
    id = "ip-info",
    category = ToolCategory.NETWORK,
    title = Res.string.my_ip_and_ip_lookup,
    description = Res.string.public_ip_address_geolocation_isp_asn_and_ti,
    icon = Icons.Filled.Public,
    keywords = listOf("geoip", "asn", "isp", "location", "external ip", "внешний ip", "провайдер", "геолокация"),
) { IpInfoScreen() }

private data class IpFacts(val rows: List<Pair<String, String>>, val lat: Double?, val lon: Double?)

private object IpApi {
    suspend fun lookup(ip: String): IpFacts {
        val primary = runCatching { ipwho(ip) }
        return primary.getOrElse { ipapi(ip) }
    }

    private fun JsonObject.s(key: String): String? = (this[key] as? JsonPrimitive)?.content?.takeIf { it != "null" && it.isNotBlank() }

    private suspend fun ipwho(ip: String): IpFacts {
        val text = Net.client.get("https://ipwho.is/$ip").bodyAsText()
        val json = Net.json.parseToJsonElement(text).jsonObject
        if (json.s("success") == "false") error(json.s("message") ?: "lookup failed")
        val connection = json["connection"] as? JsonObject
        val tz = json["timezone"] as? JsonObject
        val flag = (json["flag"] as? JsonObject)?.s("emoji") ?: ""
        val rows = listOfNotNull(
            json.s("ip")?.let { "IP" to it },
            json.s("type")?.let { "Type" to it },
            json.s("country")?.let { "Country" to "$flag $it (${json.s("country_code") ?: ""})".trim() },
            json.s("region")?.let { "Region" to it },
            json.s("city")?.let { "City" to it },
            json.s("postal")?.let { "Postal code" to it },
            connection?.s("isp")?.let { "ISP" to it },
            connection?.s("org")?.let { "Organization" to it },
            connection?.s("asn")?.let { "ASN" to "AS$it" },
            connection?.s("domain")?.let { "Domain" to it },
            tz?.s("id")?.let { "Time zone" to "$it (${tz.s("utc") ?: ""})" },
            tz?.s("current_time")?.let { "Local time" to it },
            json.s("latitude")?.let { lat -> "Coordinates" to "$lat, ${json.s("longitude")}" },
            json.s("is_eu")?.let { "EU" to it },
            json.s("calling_code")?.let { "Calling code" to "+$it" },
        )
        return IpFacts(rows, json.s("latitude")?.toDoubleOrNull(), json.s("longitude")?.toDoubleOrNull())
    }

    private suspend fun ipapi(ip: String): IpFacts {
        val path = if (ip.isEmpty()) "https://ipapi.co/json/" else "https://ipapi.co/$ip/json/"
        val text = Net.client.get(path).bodyAsText()
        val json = Net.json.parseToJsonElement(text).jsonObject
        if (json.s("error") == "true") error(json.s("reason") ?: "lookup failed")
        val rows = listOfNotNull(
            json.s("ip")?.let { "IP" to it },
            json.s("version")?.let { "Type" to it },
            json.s("country_name")?.let { "Country" to "$it (${json.s("country_code") ?: ""})" },
            json.s("region")?.let { "Region" to it },
            json.s("city")?.let { "City" to it },
            json.s("postal")?.let { "Postal code" to it },
            json.s("org")?.let { "Organization" to it },
            json.s("asn")?.let { "ASN" to it },
            json.s("timezone")?.let { "Time zone" to "$it (${json.s("utc_offset") ?: ""})" },
            json.s("latitude")?.let { lat -> "Coordinates" to "$lat, ${json.s("longitude")}" },
        )
        return IpFacts(rows, json.s("latitude")?.toDoubleOrNull(), json.s("longitude")?.toDoubleOrNull())
    }
}

@Composable
private fun IpInfoScreen() {
    var query by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var facts by remember { mutableStateOf<IpFacts?>(null) }
    val scope = rememberCoroutineScope()

    fun run(ip: String) {
        loading = true; error = null
        scope.launch {
            runCatching { IpApi.lookup(ip.trim()) }
                .onSuccess { facts = it }
                .onFailure { error = it.message ?: it.toString() }
            loading = false
        }
    }

    LaunchedEffect(Unit) { if (facts == null) run("") }

    ToolInputField(
        value = query,
        onValueChange = { query = it },
        label = Res.string.ip_address_or_hostname_empty_my_ip.str(),
        keyboardType = KeyboardType.Uri,
        monospace = true,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionButton(text = NetStrings.lookup.str(), onClick = { run(hostFrom(query)) }, enabled = !loading)
        TextButton(onClick = { query = ""; run("") }, enabled = !loading) { Text(Res.string.my_ip.str()) }
    }
    if (loading) LoadingRow()
    error?.let { ErrorText(it) }
    facts?.let { f ->
        ResultCard {
            f.rows.forEach { (k, v) -> KeyValueRow(k, v, mono = k == "IP" || k == "ASN" || k == "Coordinates") }
            if (f.lat != null && f.lon != null) {
                TextButton(onClick = { openUrl("https://www.openstreetmap.org/?mlat=${f.lat}&mlon=${f.lon}#map=10/${f.lat}/${f.lon}") }) {
                    Text(Res.string.show_on_map.str())
                }
            }
        }
    }
}
