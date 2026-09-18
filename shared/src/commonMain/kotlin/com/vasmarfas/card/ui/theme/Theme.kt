package com.vasmarfas.card.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import com.vasmarfas.card.core.platformDynamicColorScheme
import com.vasmarfas.card.data.AppSettings
import com.vasmarfas.card.data.ThemeMode

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VasmarfasTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val dynamic = if (settings.dynamicColor) platformDynamicColorScheme(dark) else null
    val generated = rememberDynamicColorScheme(
        seedColor = Color(settings.seedColor),
        isDark = dark,
        style = PaletteStyle.Vibrant,
    )
    CompositionLocalProvider(LocalStatusColors provides statusColors(dark)) {
        MaterialExpressiveTheme(
            colorScheme = dynamic ?: generated,
            motionScheme = MotionScheme.expressive(),
            content = content,
        )
    }
}

val seedPresets: List<Pair<Long, String>> = listOf(
    0xFF00696D to "Teal",
    0xFF3F51B5 to "Indigo",
    0xFF6750A4 to "Violet",
    0xFF006E1C to "Green",
    0xFFB3261E to "Red",
    0xFF9C4400 to "Orange",
    0xFF00629E to "Blue",
    0xFF7B4E7F to "Plum",
)
