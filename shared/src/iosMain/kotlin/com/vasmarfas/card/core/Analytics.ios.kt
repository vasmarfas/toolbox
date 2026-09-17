package com.vasmarfas.card.core

actual fun initAnalytics() = Unit

actual fun logEvent(name: String, params: Map<String, String>) = Unit
