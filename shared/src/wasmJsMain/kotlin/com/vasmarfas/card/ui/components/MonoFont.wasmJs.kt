package com.vasmarfas.card.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import com.vasmarfas.card.resources.Res

private var bundled by mutableStateOf<FontFamily?>(null)
private var requested = false

@Composable
actual fun monoFamily(): FontFamily {
    bundled?.let { return it }
    LaunchedEffect(Unit) {
        if (requested) return@LaunchedEffect
        requested = true
        runCatching {
            val regular = Res.readBytes("files/fonts/MobitoolMono-Regular.ttf")
            val bold = Res.readBytes("files/fonts/MobitoolMono-Bold.ttf")
            bundled = FontFamily(Font("MobitoolMono", regular), Font("MobitoolMono-Bold", bold, FontWeight.Bold))
        }
    }
    return FontFamily.Monospace
}
