package com.vasmarfas.card.tools.network

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.vasmarfas.card.core.LocalLang
import com.vasmarfas.card.core.NetCapabilities
import com.vasmarfas.card.core.currentEpochMillis
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.LoadingRow
import com.vasmarfas.card.ui.components.ResultCard
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

val publicDnsTool = Tool(
    id = "public-dns",
    category = ToolCategory.NETWORK,
    title = Res.string.public_dns_servers,
    description = Res.string.reference_of_public_resolvers_ipv4_ipv6_doh,
    icon = Icons.Filled.Shield,
    keywords = listOf("resolver", "doh", "dot", "cloudflare", "google", "quad9", "adguard", "yandex", "днс", "резолвер"),
    expandable = true,
) { PublicDnsScreen() }

private data class DnsProvider(val name: String, val ipv4: String, val ipv6: String, val doh: String?, val dot: String?, val note: StringResource)

private val providers = listOf(
    DnsProvider("Cloudflare", "1.1.1.1, 1.0.0.1", "2606:4700:4700::1111, 2606:4700:4700::1001", "https://cloudflare-dns.com/dns-query", "one.one.one.one", Res.string.fast_privacy_focused_1_1_1_2_blocks_malware),
    DnsProvider("Google", "8.8.8.8, 8.8.4.4", "2001:4860:4860::8888, 2001:4860:4860::8844", "https://dns.google/dns-query", "dns.google", Res.string.reliable_anycast_no_filtering),
    DnsProvider("Quad9", "9.9.9.9, 149.112.112.112", "2620:fe::fe, 2620:fe::9", "https://dns.quad9.net/dns-query", "dns.quad9.net", Res.string.blocks_malicious_domains_dnssec),
    DnsProvider("AdGuard", "94.140.14.14, 94.140.15.15", "2a10:50c0::ad1:ff, 2a10:50c0::ad2:ff", "https://dns.adguard-dns.com/dns-query", "dns.adguard-dns.com", Res.string.blocks_ads_and_trackers_94_140_14_140_is_unf),
    DnsProvider("Yandex", "77.88.8.8, 77.88.8.1", "2a02:6b8::feed:0ff, 2a02:6b8:0:1::feed:0ff", null, null, Res.string.basic_77_88_8_88_safe_77_88_8_7_family),
    DnsProvider("OpenDNS", "208.67.222.222, 208.67.220.220", "2620:119:35::35, 2620:119:53::53", "https://doh.opendns.com/dns-query", null, Res.string.cisco_familyshield_208_67_222_123),
    DnsProvider("Control D", "76.76.2.0, 76.76.10.0", "2606:1a40::, 2606:1a40:1::", "https://freedns.controld.com/p0", "p0.freedns.controld.com", Res.string.configurable_filters_p1_malware_p2_ads),
    DnsProvider("Mullvad", "194.242.2.2", "2a07:e340::2", "https://dns.mullvad.net/dns-query", "dns.mullvad.net", Res.string.no_logging_adblock_variant_at_194_242_2_3),
    DnsProvider("DNS4EU", "86.54.11.1", "2a13:1001::86:54:11:1", "https://unfiltered.joindns4.eu/dns-query", "unfiltered.joindns4.eu", Res.string.eu_public_resolver_protective_variants_avail),
)

@Composable
private fun PublicDnsScreen() {
    val resolverLabels = DnsResolver.entries.associateWith { it.label.str() }
    var timings by remember { mutableStateOf<Map<String, Long?>?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun benchmark() {
        loading = true
        scope.launch {
            val results = coroutineScope {
                DnsResolver.entries.filter { it.dohUrl != null || (it.udpAddress != null && NetCapabilities.udp) }.map { r ->
                    async {
                        val started = currentEpochMillis()
                        val ok = runCatching {
                            if (r.dohUrl != null) DnsClient.queryDoh(r.dohUrl, "example.com", 1, r.dohFlavour)
                            else DnsClient.queryUdp(r.udpAddress!!, "example.com", 1)
                        }.isSuccess
                        resolverLabels.getValue(r) to if (ok) currentEpochMillis() - started else null
                    }
                }.awaitAll()
            }.toMap()
            timings = results
            loading = false
        }
    }

    ActionButton(text = Res.string.measure_response_time.str(), onClick = ::benchmark, enabled = !loading)
    if (loading) LoadingRow()
    timings?.let { t ->
        ResultCard(title = Res.string.response_time_example_com_a.str()) {
            t.entries.sortedBy { it.value ?: Long.MAX_VALUE }.forEach { (name, ms) ->
                KeyValueRow(name, ms?.let { "$it ms" } ?: "—", copyable = false)
            }
        }
    }
    providers.forEach { p ->
        ResultCard(title = p.name) {
            KeyValueRow("IPv4", p.ipv4)
            KeyValueRow("IPv6", p.ipv6)
            p.doh?.let { KeyValueRow("DoH", it) }
            p.dot?.let { KeyValueRow("DoT", it) }
            KeyValueRow(Res.string.notes_2.str(), p.note.str(), mono = false, copyable = false)
        }
    }
}
