package com.vasmarfas.card.core

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

private fun jsStart(): Unit = js("{ if (window.vasmarfasAnalytics) window.vasmarfasAnalytics.start(); }")

private fun jsEvent(name: String, paramsJson: String): Unit =
    js("{ if (window.vasmarfasAnalytics) window.vasmarfasAnalytics.event(name, paramsJson); }")

private fun jsProperty(name: String, value: String?): Unit =
    js("{ if (window.vasmarfasAnalytics) window.vasmarfasAnalytics.property(name, value); }")

private fun jsPage(path: String, title: String): Unit =
    js("{ if (window.vasmarfasAnalytics) window.vasmarfasAnalytics.page(path, title); }")

actual fun initAnalytics() {
    jsStart()
}

actual fun logEvent(name: String, params: Map<String, String>, metrics: Map<String, Long>) {
    val json = buildJsonObject {
        params.forEach { (key, value) -> put(key, JsonPrimitive(value)) }
        metrics.forEach { (key, value) -> put(key, JsonPrimitive(value)) }
    }
    jsEvent(name, json.toString())
}

actual fun setUserProperty(name: String, value: String?) {
    jsProperty(name, value)
}

actual fun trackPage(path: String, title: String) {
    jsPage(path, title)
}
