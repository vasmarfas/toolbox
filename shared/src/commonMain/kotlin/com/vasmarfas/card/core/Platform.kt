package com.vasmarfas.card.core

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import com.vasmarfas.card.resources.Res
import com.vasmarfas.card.resources.android
import com.vasmarfas.card.resources.desktop
import com.vasmarfas.card.resources.ios
import com.vasmarfas.card.resources.web
import org.jetbrains.compose.resources.StringResource

enum class PlatformKind(val title: StringResource) {
    ANDROID(Res.string.android),
    IOS(Res.string.ios),
    DESKTOP(Res.string.desktop),
    WEB(Res.string.web);

    companion object {
        val all: Set<PlatformKind> = entries.toSet()
        val native: Set<PlatformKind> = setOf(ANDROID, IOS, DESKTOP)
        val jvm: Set<PlatformKind> = setOf(ANDROID, DESKTOP)
        val mobile: Set<PlatformKind> = setOf(ANDROID, IOS)
        val mobileAndWeb: Set<PlatformKind> = setOf(ANDROID, IOS, WEB)
    }
}

data class PlatformInfo(
    val kind: PlatformKind,
    val osName: String,
    val osVersion: String,
    val deviceModel: String,
    val locale: String,
    val extra: List<Pair<String, String>> = emptyList(),
)

expect fun platformInfo(): PlatformInfo

/** Hardware thread count. The browser reports one even when more exist, see [parallelWorkers]. */
expect fun cpuCoreCount(): Int

/**
 * Threads a background workload can actually occupy. Everywhere but the browser that is the core
 * count; Kotlin/Wasm has no worker pool behind `Dispatchers.Default`, so extra coroutines there
 * take turns on the one thread and only add scheduling overhead.
 */
val parallelWorkers: Int get() = if (currentPlatform == PlatformKind.WEB) 1 else cpuCoreCount()

/** Host the page is served from, or null where there is no address bar. */
expect fun siteHost(): String?

expect fun openUrl(url: String)

expect fun setWindowTitle(title: String)

@Composable
expect fun platformDynamicColorScheme(dark: Boolean): ColorScheme?

expect fun supportsDynamicColor(): Boolean

expect fun currentEpochMillis(): Long

expect fun secureRandomBytes(count: Int): ByteArray

val currentPlatform: PlatformKind get() = platformInfo().kind
