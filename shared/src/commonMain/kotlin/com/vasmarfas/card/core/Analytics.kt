package com.vasmarfas.card.core

object AnalyticsConfig {
    const val FIREBASE_ENABLED = true
    const val FIREBASE_MEASUREMENT_ID = "G-9PBF7QDFHD"
    const val METRICA_COUNTER = "112719808"
    const val WEBMASTER_VERIFICATION = ""
}

enum class AnalyticsEvent(val eventName: String) {
    APP_OPEN("app_open"),
    SCREEN_VIEW("screen_view"),
    TOOL_OPEN("tool_open"),
    TOOL_ACTION("tool_action"),
    LANGUAGE_CHANGE("language_change"),
    THEME_CHANGE("theme_change"),
}

object AnalyticsParam {
    const val SCREEN = "screen"
    const val TOOL = "tool"
    const val ACTION = "action"
    const val VALUE = "value"
}

expect fun initAnalytics(enabled: Boolean)

expect fun setAnalyticsEnabled(enabled: Boolean)

expect fun logEvent(name: String, params: Map<String, String> = emptyMap())

object Analytics {
    private const val KEY_ENABLED = "analytics.enabled"
    private const val MAX_VALUE_LENGTH = 40

    fun isEnabled(store: KeyValueStore = Prefs.store): Boolean = store.get(KEY_ENABLED) == "true"

    fun setEnabled(value: Boolean, store: KeyValueStore = Prefs.store) {
        store.put(KEY_ENABLED, value.toString())
        setAnalyticsEnabled(value)
    }

    fun start() {
        initAnalytics(isEnabled())
    }

    fun log(event: AnalyticsEvent, params: Map<String, String> = emptyMap()) {
        if (!isEnabled()) return
        logEvent(event.eventName, safeParams(params))
    }

    internal fun safeParams(params: Map<String, String>): Map<String, String> =
        params.filter { (key, value) -> isIdentifier(key) && isIdentifier(value) }

    private fun isIdentifier(value: String): Boolean =
        value.length in 1..MAX_VALUE_LENGTH &&
            value.all { it in 'a'..'z' || it in '0'..'9' || it == '_' || it == '-' }
}
