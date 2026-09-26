package com.vasmarfas.card.core

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.random.Random

private val enabled = METRICA_DESKTOP_COUNTER.isNotEmpty() && METRICA_DESKTOP_TOKEN.isNotEmpty()
private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
private val user = LinkedHashMap<String, String>()

private val clientId: String by lazy {
    Prefs.store.get("analytics.desktop.cid") ?: (currentEpochMillis() / 1000 * 1_000_000_000 + Random.nextLong(1_000_000_000)).toString()
        .also { Prefs.store.put("analytics.desktop.cid", it) }
}

private fun hit(vararg query: Pair<String, String>) {
    if (!enabled) return
    scope.launch {
        runCatching {
            Net.client.get("https://mc.yandex.ru/collect") {
                parameter("tid", METRICA_DESKTOP_COUNTER)
                parameter("cid", clientId)
                parameter("ms", METRICA_DESKTOP_TOKEN)
                query.forEach { (key, value) -> parameter(key, value) }
                header(HttpHeaders.UserAgent, "Mobitool/$APP_VERSION (${System.getProperty("os.name")} ${System.getProperty("os.version")})")
            }
        }
    }
}

// Metrica draws goal parameters as a tree: the event, then each value one level deeper, as on the site
private fun tree(name: String, values: List<String>): JsonObject {
    var node: JsonElement = JsonPrimitive(values.lastOrNull() ?: "1")
    for (value in values.dropLast(1).asReversed()) node = JsonObject(mapOf(value to node))
    return JsonObject(mapOf(name to node))
}

actual fun initAnalytics() = Unit

actual fun logEvent(name: String, params: Map<String, String>, metrics: Map<String, Long>) {
    if (name == AnalyticsEvent.SCREEN_VIEW.eventName) return
    hit("t" to "event", "ea" to name, "params" to tree(name, params.values + metrics.values.map { it.toString() }).toString())
}

actual fun setUserProperty(name: String, value: String?) {
    if (value == null) user.remove(name) else user[name] = value
}

// a new visit starts only with a page view, events without one are dropped
actual fun trackPage(path: String, title: String) {
    val visit = JsonObject(mapOf("user" to JsonObject(user.mapValues { JsonPrimitive(it.value) })))
    hit("t" to "pageview", "dl" to "https://vasmarfas.com/$path", "dt" to title, "params" to visit.toString())
}
