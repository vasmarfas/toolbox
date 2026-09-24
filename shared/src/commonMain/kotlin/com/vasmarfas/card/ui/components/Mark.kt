package com.vasmarfas.card.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// same drawing as branding/store-src/favicon-mark.svg. The chevron and the wrench are painted in cut,
// so it has to be the colour the mark sits on
fun caseMark(fg: Color, cut: Color): ImageVector =
    ImageVector.Builder(name = "CaseMark", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 100f, viewportHeight = 100f)
        .path(stroke = SolidColor(fg), strokeLineWidth = 8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(36f, 23f)
            verticalLineTo(17f)
            quadTo(36f, 11f, 42f, 11f)
            horizontalLineTo(58f)
            quadTo(64f, 11f, 64f, 17f)
            verticalLineTo(23f)
        }
        .path(fill = SolidColor(fg)) {
            moveTo(20f, 23f)
            horizontalLineTo(80f)
            arcTo(13f, 13f, 0f, false, true, 93f, 36f)
            verticalLineTo(78f)
            arcTo(13f, 13f, 0f, false, true, 80f, 91f)
            horizontalLineTo(20f)
            arcTo(13f, 13f, 0f, false, true, 7f, 78f)
            verticalLineTo(36f)
            arcTo(13f, 13f, 0f, false, true, 20f, 23f)
            close()
        }
        .path(stroke = SolidColor(cut), strokeLineWidth = 11f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(24f, 42f)
            lineTo(40f, 57f)
            lineTo(24f, 72f)
            moveTo(66f, 62f)
            lineTo(66f, 76f)
        }
        .path(fill = SolidColor(cut)) {
            moveTo(54f, 51f)
            arcTo(12f, 12f, 0f, true, true, 78f, 51f)
            arcTo(12f, 12f, 0f, true, true, 54f, 51f)
            close()
        }
        .path(fill = SolidColor(fg)) {
            moveTo(62f, 38f)
            horizontalLineTo(70f)
            verticalLineTo(51f)
            horizontalLineTo(62f)
            close()
        }
        .build()

val PromptChevron: ImageVector by lazy {
    ImageVector.Builder(name = "PromptChevron", defaultWidth = 14.dp, defaultHeight = 24.dp, viewportWidth = 170f, viewportHeight = 295f)
        .path(stroke = SolidColor(Color.Black), strokeLineWidth = 50f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(25f, 25f)
            lineTo(145f, 145.6f)
            lineTo(25f, 266.25f)
        }
        .build()
}
