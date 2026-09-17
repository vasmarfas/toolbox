package com.vasmarfas.card.core

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import androidx.annotation.RequiresPermission

@RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
actual fun wifiDetails(): Map<String, String> {
    val context = AppContextHolder.context
    val result = LinkedHashMap<String, String>()
    runCatching {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return@runCatching
        val caps = cm.getNetworkCapabilities(network)
        val link: LinkProperties? = cm.getLinkProperties(network)
        val transport = when {
            caps == null -> "unknown"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "other"
        }
        result["Transport"] = transport
        caps?.let {
            result["Downstream"] = "${it.linkDownstreamBandwidthKbps / 1000} Mbit/s"
            result["Upstream"] = "${it.linkUpstreamBandwidthKbps / 1000} Mbit/s"
            result["Metered"] = (!it.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)).toString()
            result["Validated"] = it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED).toString()
        }
        link?.let {
            result["Interface"] = it.interfaceName ?: ""
            result["Addresses"] = it.linkAddresses.joinToString(", ") { a -> a.toString() }
            result["DNS"] = it.dnsServers.joinToString(", ") { d -> d.hostAddress ?: "" }
            result["Gateway"] = it.routes.firstOrNull { r -> r.isDefaultRoute }?.gateway?.hostAddress ?: ""
            result["Domains"] = it.domains ?: ""
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                result["MTU"] = it.mtu.toString()
            }
        }
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            caps?.transportInfo as? android.net.wifi.WifiInfo
        } else {
            @Suppress("DEPRECATION")
            wifi?.connectionInfo
        }
        info?.let {
            result["SSID"] = it.ssid?.trim('"') ?: ""
            result["BSSID"] = it.bssid ?: ""
            result["RSSI"] = "${it.rssi} dBm"
            result["Link speed"] = "${it.linkSpeed} Mbit/s"
            result["Frequency"] = "${it.frequency} MHz"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                result["Wi-Fi standard"] = when (it.wifiStandard) {
                    android.net.wifi.ScanResult.WIFI_STANDARD_11AX -> "802.11ax (Wi-Fi 6)"
                    android.net.wifi.ScanResult.WIFI_STANDARD_11AC -> "802.11ac (Wi-Fi 5)"
                    android.net.wifi.ScanResult.WIFI_STANDARD_11N -> "802.11n (Wi-Fi 4)"
                    else -> it.wifiStandard.toString()
                }
            }
        }
        wifi?.let {
            result["Wi-Fi enabled"] = it.isWifiEnabled.toString()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                result["5 GHz supported"] = it.is5GHzBandSupported.toString()
                result["6 GHz supported"] = it.is6GHzBandSupported.toString()
            }
        }
    }
    return result
}
