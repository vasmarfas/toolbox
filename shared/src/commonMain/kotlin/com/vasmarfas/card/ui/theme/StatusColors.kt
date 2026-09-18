package com.vasmarfas.card.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
class StatusColors(
    val good: Color,
    val warn: Color,
    val bad: Color,
    val series: List<Color>,
)

private val lightStatus = StatusColors(
    good = Color(0xFF2E7032),
    warn = Color(0xFF8A5A00),
    bad = Color(0xFFB3261E),
    series = listOf(Color(0xFFB3261E), Color(0xFF2E7032), Color(0xFF15507F)),
)

private val darkStatus = StatusColors(
    good = Color(0xFF4CAF6A),
    warn = Color(0xFFC98A21),
    bad = Color(0xFFF0796F),
    series = listOf(Color(0xFFF0796F), Color(0xFF4CAF6A), Color(0xFF6FB2F0)),
)

fun statusColors(dark: Boolean): StatusColors = if (dark) darkStatus else lightStatus

val LocalStatusColors = staticCompositionLocalOf { lightStatus }
