package com.vasmarfas.card.tools.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.ToolInputField
import com.vasmarfas.card.ui.components.rememberCopy

val colorPaletteTool = Tool(
    id = "color-palette",
    category = ToolCategory.DESIGN,
    title = Res.string.color_palette,
    description = Res.string.color_palette_description,
    icon = Icons.Filled.Palette,
    keywords = listOf("palette", "harmony", "complementary", "triadic", "tints", "палитра", "гармония", "оттенки", "цветовой круг"),
    expandable = true,
) { ColorPaletteScreen() }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPaletteScreen() {
    var input by rememberSaveable { mutableStateOf("#326773") }
    val base = remember(input) { ColorMath.parse(input) }
    val copy = rememberCopy()
    ToolInputField(
        value = input,
        onValueChange = { input = it },
        label = Res.string.base_color.str(),
        placeholder = "#00696D · teal · hsl(182, 100%, 21%)",
        isError = input.isNotBlank() && base == null,
        monospace = true,
    )
    ChoiceChips(
        options = listOf("#00696D", "#3F51B5", "#B3261E", "#9C4400", "#006E1C", "#7B4E7F"),
        selected = input.uppercase(),
        onSelect = { input = it },
        label = { it },
    )
    if (base == null) {
        ErrorText(Res.string.unknown_color_format.str())
        return
    }
    WideSwatch(base, base.hex(), ColorMath.hslString(base)) { copy(base.hex()) }

    val groups = listOf(
        Res.string.complementary to Palette.complementary(base),
        Res.string.analogous to Palette.analogous(base),
        Res.string.triadic to Palette.triadic(base),
        Res.string.tetradic to Palette.tetradic(base),
        Res.string.split_complementary to Palette.splitComplementary(base),
        Res.string.tints to Palette.tints(base),
        Res.string.shades to Palette.shades(base),
    )
    groups.forEach { (title, colors) ->
        ResultCard(title.str()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                colors.forEach { color ->
                    ColorSwatch(
                        color = color,
                        label = color.hex(),
                        modifier = Modifier.weight(1f),
                        onClick = { copy(color.hex()) },
                    )
                }
            }
            Text(
                text = colors.joinToString(", ") { it.hex() },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    ResultCard(Res.string.tonal_ramp.str()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            Palette.tonalRamp(base).forEach { (tone, color) ->
                ColorSwatch(
                    color = color,
                    label = tone.toString(),
                    modifier = Modifier.weight(1f),
                    height = 52.dp,
                    onClick = { copy(color.hex()) },
                )
            }
        }
    }
}
