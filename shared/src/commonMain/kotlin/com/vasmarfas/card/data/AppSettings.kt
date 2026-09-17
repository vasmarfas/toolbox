package com.vasmarfas.card.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.vasmarfas.card.core.KeyValueStore
import com.vasmarfas.card.core.Lang
import com.vasmarfas.card.core.Prefs
import com.vasmarfas.card.core.applyPlatformLocale
import com.vasmarfas.card.core.platformInfo

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class AppSettings(private val store: KeyValueStore = Prefs.store) {
    var lang: Lang by mutableStateOf(
        Lang.fromCode(store.get(KEY_LANG)) ?: Lang.fromSystemTag(platformInfo().locale)
    )
        private set

    var themeMode: ThemeMode by mutableStateOf(
        store.get(KEY_THEME)?.let { v -> ThemeMode.entries.firstOrNull { it.name == v } } ?: ThemeMode.SYSTEM
    )
        private set

    var dynamicColor: Boolean by mutableStateOf(store.get(KEY_DYNAMIC) != "false")
        private set

    var seedColor: Long by mutableStateOf(store.get(KEY_SEED)?.toLongOrNull() ?: DEFAULT_SEED)
        private set

    var wideTables: Boolean by mutableStateOf(store.get(KEY_WIDE_TABLES) == "true")
        private set

    var startOnTools: Boolean by mutableStateOf(store.get(KEY_START_ON_TOOLS) != "false")
        private set

    var favorites: Set<String> by mutableStateOf(readList(KEY_FAVORITES).toSet())
        private set

    var recent: List<String> by mutableStateOf(readList(KEY_RECENT))
        private set

    init {
        applyPlatformLocale(lang.code)
    }

    fun updateLang(value: Lang) {
        applyPlatformLocale(value.code)
        lang = value
        store.put(KEY_LANG, value.code)
    }

    fun updateThemeMode(value: ThemeMode) {
        themeMode = value
        store.put(KEY_THEME, value.name)
    }

    fun updateDynamicColor(value: Boolean) {
        dynamicColor = value
        store.put(KEY_DYNAMIC, value.toString())
    }

    fun updateSeedColor(value: Long) {
        seedColor = value
        store.put(KEY_SEED, value.toString())
    }

    fun updateWideTables(value: Boolean) {
        wideTables = value
        store.put(KEY_WIDE_TABLES, value.toString())
    }

    fun updateStartOnTools(value: Boolean) {
        startOnTools = value
        store.put(KEY_START_ON_TOOLS, value.toString())
    }

    val firstRun: Boolean get() = store.get(KEY_LAUNCHED) == null

    fun markLaunched() {
        store.put(KEY_LAUNCHED, "true")
    }

    fun toggleFavorite(id: String) {
        favorites = if (id in favorites) favorites - id else favorites + id
        store.put(KEY_FAVORITES, favorites.joinToString(","))
    }

    fun markRecent(id: String) {
        recent = (listOf(id) + recent.filter { it != id }).take(8)
        store.put(KEY_RECENT, recent.joinToString(","))
    }

    private fun readList(key: String): List<String> =
        store.get(key)?.split(',')?.filter { it.isNotBlank() } ?: emptyList()

    companion object {
        const val DEFAULT_SEED = 0xFF00696D
        private const val KEY_LANG = "settings.lang"
        private const val KEY_THEME = "settings.theme"
        private const val KEY_DYNAMIC = "settings.dynamic"
        private const val KEY_SEED = "settings.seed"
        private const val KEY_WIDE_TABLES = "settings.wideTables"
        private const val KEY_START_ON_TOOLS = "settings.startOnTools"
        private const val KEY_LAUNCHED = "app.launched"
        private const val KEY_FAVORITES = "tools.favorites"
        private const val KEY_RECENT = "tools.recent"
    }
}

val LocalSettings = staticCompositionLocalOf<AppSettings> { error("AppSettings not provided") }
