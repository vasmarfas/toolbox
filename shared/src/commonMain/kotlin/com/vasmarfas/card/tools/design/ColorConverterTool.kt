package com.vasmarfas.card.tools.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SegmentedChoice
import com.vasmarfas.card.ui.components.SwitchRow
import com.vasmarfas.card.ui.components.ToolInputField
import com.vasmarfas.card.ui.components.ToolSection
import com.vasmarfas.card.ui.components.expandedHeight
import com.vasmarfas.card.ui.components.expandedSquare
import com.vasmarfas.card.ui.components.trackTouch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import org.jetbrains.compose.resources.StringResource

private enum class PaletteShape(val title: StringResource) {
    SQUARE(Res.string.square),
    WHEEL(Res.string.wheel),
}

private enum class ChannelSet { RGB, HSL }

val colorConverterTool = Tool(
    id = "color-converter",
    category = ToolCategory.DESIGN,
    title = Res.string.color_converter,
    description = Res.string.color_converter_description,
    icon = Icons.Filled.ColorLens,
    keywords = listOf("hex", "rgb", "hsl", "hsv", "cmyk", "alpha", "color picker", "color wheel", "color", "цвет", "конвертер", "палитра", "цветовой круг", "прозрачность", "код цвета"),
    expandable = true,
) { ColorConverterScreen() }

@Composable
private fun ColorConverterScreen() {
    var input by rememberSaveable { mutableStateOf("#3F51B5") }
    var withAlpha by rememberSaveable { mutableStateOf(false) }
    var shape by rememberSaveable { mutableStateOf(PaletteShape.SQUARE) }
    var channels by rememberSaveable { mutableStateOf(ChannelSet.RGB) }
    var storedHue by rememberSaveable { mutableStateOf(0f) }
    var storedSaturation by rememberSaveable { mutableStateOf(1f) }
    val parsed = remember(input) { ColorMath.parse(input) }
    ToolInputField(
        value = input,
        onValueChange = { input = it },
        label = Res.string.color.str(),
        placeholder = "#3F51B5 · rgba(63, 81, 181, 0.5) · hsl(231, 48%, 48%) · indigo",
        isError = input.isNotBlank() && parsed == null,
        monospace = true,
    )
    SwitchRow(
        label = Res.string.with_transparency.str(),
        checked = withAlpha,
        onCheckedChange = { on ->
            withAlpha = on
            parsed?.let { input = if (on) it.hex(withAlpha = true) else it.copy(a = 255).hex() }
        },
        description = Res.string.color_alpha_channel_in_hex.str(),
    )
    if (parsed == null) {
        ErrorText(Res.string.unknown_color_format.str())
        return
    }
    val color = if (withAlpha) parsed else parsed.copy(a = 255)
    val hsl = ColorMath.toHsl(color)
    val hsv = ColorMath.toHsv(color)
    val cmyk = ColorMath.toCmyk(color)
    val (name, exact) = ColorMath.nearestCssName(color)
    val hue = if (hsv.s > 0.001 && hsv.v > 0.001) hsv.h.toFloat() else storedHue
    val saturation = if (hsv.v > 0.001) hsv.s.toFloat() else storedSaturation
    val opaque = color.copy(a = 255).toColor()

    fun show(value: Rgba, h: Float = hue, s: Float = saturation) {
        storedHue = h
        storedSaturation = s
        input = value.hex(withAlpha)
    }

    ToolSection(Res.string.palette.str()) {
        ChoiceChips(
            options = PaletteShape.entries,
            selected = shape,
            onSelect = { shape = it },
            label = { it.title.str() },
        )
        if (shape == PaletteShape.SQUARE) {
            SaturationValueField(hue, saturation, hsv.v.toFloat()) { s, v ->
                show(ColorMath.fromHsv(Hsv(hue.toDouble(), s.toDouble(), v.toDouble()), color.a), s = s)
            }
            PickerBar(
                fraction = hue / 360f,
                onChange = { show(ColorMath.fromHsv(Hsv(it * 360.0, saturation.toDouble(), hsv.v), color.a), h = it * 360f) },
            ) {
                drawRect(Brush.horizontalGradient(hueRamp))
            }
        } else {
            val brightest = ColorMath.fromHsv(Hsv(hue.toDouble(), saturation.toDouble(), 1.0)).toColor()
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ColorWheel(hue, saturation, hsv.v.toFloat(), Modifier.size(expandedSquare(normal = 260.dp, reserved = 560.dp))) { h, s ->
                    show(ColorMath.fromHsv(Hsv(h.toDouble(), s.toDouble(), hsv.v), color.a), h = h, s = s)
                }
            }
            PickerBar(
                fraction = hsv.v.toFloat(),
                onChange = { show(ColorMath.fromHsv(Hsv(hue.toDouble(), saturation.toDouble(), it.toDouble()), color.a)) },
            ) {
                drawRect(Brush.horizontalGradient(listOf(Color.Black, brightest)))
            }
            Text(
                text = "${Res.string.brightness.str()} ${(hsv.v * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (withAlpha) {
            PickerBar(
                fraction = color.alphaFraction.toFloat(),
                onChange = { show(color.copy(a = (it * 255).roundToInt())) },
            ) {
                drawCheckerboard()
                drawRect(Brush.horizontalGradient(listOf(opaque.copy(alpha = 0f), opaque)))
            }
            Text(
                text = "${Res.string.opacity.str()} ${(color.alphaFraction * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    WideSwatch(color, color.hex(withAlpha), ColorMath.rgbString(color))

    ToolSection(Res.string.channels.str()) {
        SegmentedChoice(options = ChannelSet.entries, selected = channels, onSelect = { channels = it }, label = { it.name })
        if (channels == ChannelSet.RGB) {
            ChannelSlider("R", color.r.toFloat(), 255f) { show(color.copy(r = it.roundToInt())) }
            ChannelSlider("G", color.g.toFloat(), 255f) { show(color.copy(g = it.roundToInt())) }
            ChannelSlider("B", color.b.toFloat(), 255f) { show(color.copy(b = it.roundToInt())) }
        } else {
            ChannelSlider("H", hue, 360f) { show(ColorMath.fromHsl(Hsl(it.toDouble(), hsl.s, hsl.l), color.a), h = it) }
            ChannelSlider("S", (hsl.s * 100).toFloat(), 100f) { show(ColorMath.fromHsl(Hsl(hue.toDouble(), it / 100.0, hsl.l), color.a)) }
            ChannelSlider("L", (hsl.l * 100).toFloat(), 100f) { show(ColorMath.fromHsl(Hsl(hue.toDouble(), hsl.s, it / 100.0), color.a)) }
        }
        if (withAlpha) {
            ChannelSlider("A", color.a.toFloat(), 255f) { show(color.copy(a = it.roundToInt())) }
        }
    }

    ResultCard {
        KeyValueRow("HEX", color.hex(withAlpha))
        KeyValueRow("RGB", ColorMath.rgbString(color))
        KeyValueRow("HSL", ColorMath.hslString(color))
        KeyValueRow("HSV", ColorMath.hsvString(color))
        KeyValueRow("CMYK", ColorMath.cmykString(color))
        KeyValueRow("Compose", color.composeLiteral)
        KeyValueRow(Res.string.android_aarrggbb.str(), color.androidHex)
        KeyValueRow(
            Res.string.css_name.str(),
            if (exact) name else "$name (${Res.string.nearest.str()})",
            mono = false,
        )
        Text(
            text = "H ${hsl.h.roundToInt()}° · S ${(hsl.s * 100).roundToInt()}% · L ${(hsl.l * 100).roundToInt()}% · V ${(hsv.v * 100).roundToInt()}% · K ${(cmyk.k * 100).roundToInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val hueRamp = List(7) { ColorMath.fromHsv(Hsv(it * 60.0, 1.0, 1.0)).toColor() }

private val wheelRamp = List(13) { ColorMath.fromHsv(Hsv(it * 30.0, 1.0, 1.0)).toColor() }

private val wheelWash = listOf(Color.White, Color.White.copy(alpha = 0f))

@Composable
private fun SaturationValueField(hue: Float, saturation: Float, value: Float, onChange: (Float, Float) -> Unit) {
    val tint = remember(hue) { ColorMath.fromHsv(Hsv(hue.toDouble(), 1.0, 1.0)).toColor() }
    val handler by rememberUpdatedState(onChange)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(expandedHeight(normal = 180.dp, reserved = 560.dp))
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                val point: (Offset) -> Unit = {
                    handler((it.x / size.width).coerceIn(0f, 1f), (1f - it.y / size.height).coerceIn(0f, 1f))
                }
                trackTouch(onStart = point, onMove = point)
            },
    ) {
        drawRect(Brush.horizontalGradient(listOf(Color.White, tint)))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        drawHandle(saturation * size.width, (1f - value) * size.height)
    }
}

@Composable
private fun ColorWheel(hue: Float, saturation: Float, value: Float, modifier: Modifier = Modifier, onChange: (Float, Float) -> Unit) {
    val handler by rememberUpdatedState(onChange)
    Canvas(
        modifier.pointerInput(Unit) {
            val point: (Offset) -> Unit = {
                val dx = it.x - size.width / 2f
                val dy = it.y - size.height / 2f
                val reach = minOf(size.width, size.height) / 2f
                handler((atan2(dy, dx) * 180 / PI).toFloat().mod(360f), (hypot(dx, dy) / reach).coerceIn(0f, 1f))
            }
            trackTouch(onStart = point, onMove = point)
        },
    ) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2, size.height / 2)
        drawCircle(Brush.sweepGradient(wheelRamp, center), radius, center)
        drawCircle(Brush.radialGradient(wheelWash, center, radius), radius, center)
        drawCircle(Color.Black.copy(alpha = 1f - value), radius, center)
        val radians = hue * PI.toFloat() / 180f
        drawHandle(center.x + cos(radians) * saturation * radius, center.y + sin(radians) * saturation * radius)
    }
}

@Composable
private fun PickerBar(fraction: Float, onChange: (Float) -> Unit, background: DrawScope.() -> Unit) {
    val handler by rememberUpdatedState(onChange)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                val point: (Offset) -> Unit = { handler((it.x / size.width).coerceIn(0f, 1f)) }
                trackTouch(onStart = point, onMove = point)
            },
    ) {
        background()
        drawHandle(fraction * size.width, size.height / 2)
    }
}

private fun DrawScope.drawHandle(x: Float, y: Float) {
    val radius = 8.dp.toPx()
    drawCircle(Color.Black.copy(alpha = 0.45f), radius + 1.dp.toPx(), Offset(x, y), style = Stroke(1.dp.toPx()))
    drawCircle(Color.White, radius, Offset(x, y), style = Stroke(2.dp.toPx()))
}

@Composable
private fun ChannelSlider(label: String, value: Float, max: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.width(36.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(value.roundToInt().toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value.coerceIn(0f, max),
            onValueChange = onChange,
            valueRange = 0f..max,
            modifier = Modifier.weight(1f),
        )
    }
}
