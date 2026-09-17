package com.vasmarfas.card.core

import android.Manifest
import android.os.Bundle
import androidx.annotation.RequiresPermission
import com.google.firebase.analytics.FirebaseAnalytics

private var firebase: FirebaseAnalytics? = null

// without google-services.json the SDKs throw on first access, and a build with Firebase left
// unconfigured still has to run
@RequiresPermission(
    allOf = [
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.WAKE_LOCK,
    ],
)
actual fun initAnalytics() {
    if (!AnalyticsConfig.FIREBASE_ENABLED) return
    firebase = runCatching { FirebaseAnalytics.getInstance(AppContextHolder.context) }.getOrNull()
}

actual fun logEvent(name: String, params: Map<String, String>) {
    val target = firebase ?: return
    val bundle = Bundle().apply { params.forEach { (key, value) -> putString(key, value) } }
    runCatching { target.logEvent(name, bundle) }
}
