package com.vasmarfas.card.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class ChromeState {
    var immersive by mutableStateOf(false)
}

val LocalChrome = staticCompositionLocalOf { ChromeState() }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun expandedHeight(normal: Dp, reserved: Dp): Dp {
    if (!LocalChrome.current.immersive) return normal
    val window = LocalWindowInfo.current.containerSize.height
    val available = with(LocalDensity.current) { window.toDp() } - reserved
    return if (available > normal) available else normal
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun expandedSquare(normal: Dp, reserved: Dp): Dp {
    if (!LocalChrome.current.immersive) return normal
    val window = LocalWindowInfo.current.containerSize
    val side = with(LocalDensity.current) { minOf(window.width.toDp() - 32.dp, window.height.toDp() - reserved) }
    return if (side > normal) side else normal
}

@Composable
fun expandedTextStyle(normal: TextStyle): TextStyle =
    if (LocalChrome.current.immersive) normal.copy(fontSize = normal.fontSize * 1.6f, lineHeight = normal.lineHeight * 1.6f) else normal
