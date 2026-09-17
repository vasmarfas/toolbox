package com.vasmarfas.card.core

actual fun initAnalytics(enabled: Boolean) = Unit

actual fun setAnalyticsEnabled(enabled: Boolean) = Unit

actual fun logEvent(name: String, params: Map<String, String>) = Unit
