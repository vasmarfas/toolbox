package com.vasmarfas.card.core

actual fun platformNetCapabilities() = PlatformNetCapabilities(
    icmpPing = false,
    tcp = false,
    udp = false,
    interfaces = false,
    tls = false,
)

actual suspend fun icmpPing(host: String, sequence: Int, timeoutMs: Int, ttl: Int?): PingReply =
    PingReply(sequence, null, null, null, "unsupported")

actual suspend fun tcpConnect(host: String, port: Int, timeoutMs: Int): Long? = null

actual suspend fun resolveHost(host: String): List<String> = emptyList()

actual suspend fun reverseLookup(address: String): String? = null

actual suspend fun wakeOnLan(mac: String, broadcast: String, port: Int): Boolean = false

actual suspend fun networkInterfaces(): List<InterfaceInfo> = emptyList()

actual suspend fun tlsHandshake(host: String, port: Int, timeoutMs: Int): TlsInfo = throw UnsupportedOperationException("unsupported")

actual suspend fun whoisQuery(server: String, query: String, timeoutMs: Int): String = throw UnsupportedOperationException("unsupported")

actual suspend fun udpQuery(host: String, port: Int, payload: ByteArray, timeoutMs: Int): ByteArray? = null

actual suspend fun ssdpDiscover(timeoutMs: Int): List<DiscoveredDevice> = emptyList()

actual suspend fun mdnsQuery(serviceName: String, timeoutMs: Int): List<ByteArray> = emptyList()

actual fun wifiDetails(): Map<String, String> = emptyMap()
