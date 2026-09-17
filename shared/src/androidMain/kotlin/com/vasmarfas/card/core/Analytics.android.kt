package com.vasmarfas.card.core

import android.Manifest
import android.os.Bundle
import androidx.annotation.RequiresPermission
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance

private var firebase: FirebaseAnalytics? = null

@RequiresPermission(
    allOf = [
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.WAKE_LOCK,
    ],
)
private fun startSdks() {
    if (firebase != null || !AnalyticsConfig.FIREBASE_ENABLED) return
    firebase = runCatching { FirebaseAnalytics.getInstance(AppContextHolder.context) }.getOrNull()
}

/**
 * The manifest switches all three SDKs off at install time, so this is the only thing that ever
 * turns them on. Each one is guarded separately: without google-services.json they throw on first
 * access, and a build with Firebase left unconfigured still has to run.
 */
private fun setCollection(enabled: Boolean) {
    runCatching { firebase?.setAnalyticsCollectionEnabled(enabled) }
    runCatching { FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = enabled }
    runCatching { FirebasePerformance.getInstance().isPerformanceCollectionEnabled = enabled }
}

@RequiresPermission(
    allOf = [
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.WAKE_LOCK,
    ],
)
actual fun initAnalytics(enabled: Boolean) {
    if (!enabled) return
    startSdks()
    setCollection(true)
}

@RequiresPermission(
    allOf = [
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.WAKE_LOCK,
    ],
)
actual fun setAnalyticsEnabled(enabled: Boolean) {
    if (enabled) startSdks()
    setCollection(enabled)
}

actual fun logEvent(name: String, params: Map<String, String>) {
    val target = firebase ?: return
    val bundle = Bundle().apply { params.forEach { (key, value) -> putString(key, value) } }
    runCatching { target.logEvent(name, bundle) }
}
