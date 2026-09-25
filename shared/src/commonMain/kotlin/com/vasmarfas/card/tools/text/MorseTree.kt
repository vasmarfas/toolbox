package com.vasmarfas.card.tools.text

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*

private val dotCells = mapOf(
    "." to IntOffset(1, 0), ".." to IntOffset(2, 0), "..." to IntOffset(3, 0), "...." to IntOffset(4, 0),
    ".-" to IntOffset(1, 1), "..-" to IntOffset(2, 1), "...-" to IntOffset(3, 1), "..-." to IntOffset(3, 2),
    "..-.." to IntOffset(4, 2), "..--" to IntOffset(2, 3), ".-." to IntOffset(2, 4), ".-.." to IntOffset(3, 4),
    ".-.-" to IntOffset(2, 5), ".--" to IntOffset(1, 5), ".--." to IntOffset(2, 6), ".---" to IntOffset(1, 7),
)

internal fun chartCell(code: String): IntOffset =
    dotCells.getValue(if (code.first() == '.') code else code.map { if (it == '.') '-' else '.' }.joinToString(""))

@Composable
fun MorseTree(alphabet: MorseAlphabet, highlight: String?, modifier: Modifier = Modifier) {
    val letters = remember(alphabet) { Morse.letters(alphabet) }
    val codes = remember(letters) { letters.keys.flatMap { code -> (1..code.length).map { code.take(it) } }.toSet() }
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val label = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold)
    val startLabel = measurer.measure(Res.string.start.str(), MaterialTheme.typography.labelMedium.copy(color = colors.onSurfaceVariant))
    Canvas(modifier.fillMaxWidth().height(304.dp)) {
        val col = minOf(size.width / 9, 56.dp.toPx())
        val row = 38.dp.toPx()
        val top = 26.dp.toPx()
        val gap = 15.dp.toPx()
        fun point(code: String): Offset {
            if (code.isEmpty()) return Offset(size.width / 2, top)
            val cell = chartCell(code)
            return Offset(size.width / 2 + (if (code.first() == '.') -1 else 1) * cell.x * col, top + cell.y * row)
        }
        fun lit(code: String) = highlight != null && highlight.startsWith(code)
        fun down(code: String) = point(code.dropLast(1)).x == point(code).x

        for (on in listOf(false, true)) {
            for (code in codes.filter { lit(it) == on }) {
                val from = point(code.dropLast(1))
                val to = point(code)
                val path = Path().apply {
                    moveTo(from.x, from.y)
                    lineTo(from.x, to.y)
                    lineTo(to.x, to.y)
                }
                val width = if (on) 3.dp.toPx() else 1.5.dp.toPx()
                drawPath(path, if (on) colors.primary else colors.outlineVariant, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        drawCircle(colors.surface, 5.dp.toPx(), point(""))
        drawCircle(if (highlight != null) colors.primary else colors.onSurfaceVariant, 5.dp.toPx(), point(""), style = Stroke(1.5.dp.toPx()))
        drawText(startLabel, topLeft = point("") - Offset(startLabel.size.width / 2f, gap + startLabel.size.height / 2f))

        for (code in codes) {
            val at = point(code)
            val ink = if (lit(code)) colors.primary else colors.onSurfaceVariant
            if (code.last() == '.') {
                drawCircle(ink, 4.dp.toPx(), at)
            } else {
                val long = 16.dp.toPx()
                val thick = 6.dp.toPx()
                val bar = if (down(code)) Size(thick, long) else Size(long, thick)
                drawRoundRect(ink, at - Offset(bar.width / 2, bar.height / 2), bar, CornerRadius(thick / 2))
            }
        }
        for ((code, letter) in letters) {
            val mark = point(code)
            val at = if (down(code)) mark.copy(x = mark.x - (if (code.first() == '.') -1 else 1) * gap) else mark.copy(y = mark.y - gap)
            val current = code == highlight
            if (current) drawCircle(colors.primary, 10.dp.toPx(), at)
            val ink = when {
                current -> colors.onPrimary
                lit(code) -> colors.primary
                else -> colors.onSurface
            }
            val text = measurer.measure(letter.uppercaseChar().toString(), label.copy(color = ink))
            drawText(text, topLeft = at - Offset(text.size.width / 2f, text.size.height / 2f))
        }
    }
}
