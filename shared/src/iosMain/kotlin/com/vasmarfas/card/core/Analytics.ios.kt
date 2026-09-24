package com.vasmarfas.card.core

actual fun initAnalytics() = Unit

actual fun logEvent(name: String, params: Map<String, String>, metrics: Map<String, Long>) = Unit

actual fun setUserProperty(name: String, value: String?) = Unit

actual fun trackPage(path: String, title: String) = Unit
