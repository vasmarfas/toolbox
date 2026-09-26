package com.vasmarfas.card.core

object AnalyticsBridge {
    var logEvent: ((String, Map<String, Any>) -> Unit)? = null
    var setUserProperty: ((String, String?) -> Unit)? = null
}

actual fun initAnalytics() = Unit

actual fun logEvent(name: String, params: Map<String, String>, metrics: Map<String, Long>) {
    AnalyticsBridge.logEvent?.invoke(name, params + metrics)
}

actual fun setUserProperty(name: String, value: String?) {
    AnalyticsBridge.setUserProperty?.invoke(name, value)
}

actual fun trackPage(path: String, title: String) = Unit
