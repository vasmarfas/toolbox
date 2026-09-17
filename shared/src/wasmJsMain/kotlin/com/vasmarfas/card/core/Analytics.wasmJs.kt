package com.vasmarfas.card.core

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

private fun jsEnable(metricaCounter: String): Unit =
    js("{ if (window.vasmarfasAnalytics) window.vasmarfasAnalytics.enable(metricaCounter); }")

private fun jsDisable(): Unit = js("{ if (window.vasmarfasAnalytics) window.vasmarfasAnalytics.disable(); }")

private fun jsEvent(name: String, paramsJson: String): Unit =
    js("{ if (window.vasmarfasAnalytics) window.vasmarfasAnalytics.event(name, paramsJson); }")

actual fun initAnalytics(enabled: Boolean) {
    if (enabled) jsEnable(AnalyticsConfig.METRICA_COUNTER)
}

actual fun setAnalyticsEnabled(enabled: Boolean) {
    if (enabled) jsEnable(AnalyticsConfig.METRICA_COUNTER) else jsDisable()
}

actual fun logEvent(name: String, params: Map<String, String>) {
    val json = Net.json.encodeToString(MapSerializer(String.serializer(), String.serializer()), params)
    jsEvent(name, json)
}
