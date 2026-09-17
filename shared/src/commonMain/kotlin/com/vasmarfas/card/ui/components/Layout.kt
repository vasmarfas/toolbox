package com.vasmarfas.card.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class LayoutSize { COMPACT, MEDIUM, EXPANDED }

fun layoutSizeFor(width: Dp): LayoutSize = when {
    width < 600.dp -> LayoutSize.COMPACT
    width < 1240.dp -> LayoutSize.MEDIUM
    else -> LayoutSize.EXPANDED
}

val LocalLayoutSize = staticCompositionLocalOf { LayoutSize.COMPACT }
