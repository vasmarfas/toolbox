package com.vasmarfas.card.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private fun ImageVector.Builder.chevron(dx: Float, dy: Float) = path(
    stroke = SolidColor(Color.Black),
    strokeLineWidth = 50f,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
) {
    moveTo(66f - dx, 121f - dy)
    lineTo(186f - dx, 241.6f - dy)
    lineTo(66f - dx, 362.25f - dy)
}

val PromptMark: ImageVector by lazy {
    ImageVector.Builder(name = "PromptMark", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 512f, viewportHeight = 512f)
        .chevron(0f, 0f)
        .path(stroke = SolidColor(Color.Black), strokeLineWidth = 50f, strokeLineCap = StrokeCap.Round) {
            moveTo(261f, 366f)
            lineTo(421f, 366f)
        }
        .build()
}

val PromptChevron: ImageVector by lazy {
    ImageVector.Builder(name = "PromptChevron", defaultWidth = 14.dp, defaultHeight = 24.dp, viewportWidth = 170f, viewportHeight = 295f)
        .chevron(41f, 96f)
        .build()
}
