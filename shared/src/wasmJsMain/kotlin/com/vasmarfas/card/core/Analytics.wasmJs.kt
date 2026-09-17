package com.vasmarfas.card.core

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

private fun jsStart(metricaCounter: String): Unit =
    js("{ if (window.vasmarfasAnalytics) window.vasmarfasAnalytics.start(metricaCounter); }")

private fun jsEvent(name: String, paramsJson: String): Unit =
    js("{ if (window.vasmarfasAnalytics) window.vasmarfasAnalytics.event(name, paramsJson); }")

actual fun initAnalytics() {
    jsStart(AnalyticsConfig.METRICA_COUNTER)
}

actual fun logEvent(name: String, params: Map<String, String>) {
    val json = Net.json.encodeToString(MapSerializer(String.serializer(), String.serializer()), params)
    jsEvent(name, json)
}
