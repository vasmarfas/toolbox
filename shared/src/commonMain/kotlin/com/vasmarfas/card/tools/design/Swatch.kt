package com.vasmarfas.card.tools.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vasmarfas.card.ui.components.monoFamily

fun Rgba.toColor(): Color = Color(r / 255f, g / 255f, b / 255f, a / 255f)

fun Color.toRgba(): Rgba = Rgba((red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt(), (alpha * 255).toInt())

fun Rgba.readableOn(): Color = if (Wcag.relativeLuminance(this) > 0.4) Color.Black else Color.White

fun DrawScope.drawCheckerboard() {
    val cell = 6.dp.toPx()
    drawRect(Color(0xFFE9E9E9))
    var y = 0f
    var row = 0
    while (y < size.height) {
        var x = if (row % 2 == 0) 0f else cell
        while (x < size.width) {
            drawRect(
                color = Color(0xFFBFBFBF),
                topLeft = Offset(x, y),
                size = Size(minOf(cell, size.width - x), minOf(cell, size.height - y)),
            )
            x += cell * 2
        }
        y += cell
        row++
    }
}

@Composable
fun ColorSwatch(
    color: Rgba,
    label: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    height: Dp = 64.dp,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            if (color.a != 255) drawCheckerboard()
            drawRect(color.toColor())
        }
        Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = color.readableOn(),
                textAlign = TextAlign.Center,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = MaterialTheme.typography.labelMedium.fontSize),
            )
            if (caption != null) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = monoFamily()),
                    color = color.readableOn(),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 7.sp, maxFontSize = MaterialTheme.typography.labelSmall.fontSize),
                )
            }
        }
    }
}

@Composable
fun WideSwatch(color: Rgba, label: String, caption: String? = null, onClick: (() -> Unit)? = null) {
    ColorSwatch(color, label, Modifier.fillMaxWidth(), caption, 72.dp, onClick)
}
