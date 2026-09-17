package com.vasmarfas.card.tools.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.CopyIconButton
import com.vasmarfas.card.ui.components.DropdownChoice
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.MonoText
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SegmentedChoice
import com.vasmarfas.card.ui.components.ToolInputField
import com.vasmarfas.card.ui.components.rememberCopy

val materialSchemeTool = Tool(
    id = "material-scheme",
    category = ToolCategory.DESIGN,
    title = Res.string.material_3_scheme,
    description = Res.string.a_full_material_3_color_scheme_from_a_seed_c,
    icon = Icons.Filled.Style,
    keywords = listOf("material you", "scheme", "seed", "tonal", "theme", "схема", "тема", "материал", "палитра"),
) { MaterialSchemeScreen() }

@Composable
private fun MaterialSchemeScreen() {
    var input by rememberSaveable { mutableStateOf("#6750A4") }
    var style by rememberSaveable { mutableStateOf(PaletteStyle.TonalSpot) }
    var dark by rememberSaveable { mutableStateOf(false) }
    val seed = remember(input) { ColorMath.parse(input) }
    val copy = rememberCopy()
    ToolInputField(
        value = input,
        onValueChange = { input = it },
        label = Res.string.seed_color.str(),
        placeholder = "#6750A4",
        isError = input.isNotBlank() && seed == null,
        monospace = true,
    )
    ChoiceChips(
        options = listOf("#6750A4", "#00696D", "#3F51B5", "#B3261E", "#9C4400", "#006E1C"),
        selected = input.uppercase(),
        onSelect = { input = it },
        label = { it },
    )
    DropdownChoice(
        options = PaletteStyle.entries,
        selected = style,
        onSelect = { style = it },
        label = Res.string.palette_style.str(),
        text = { it.name },
    )
    SegmentedChoice(
        options = listOf(false, true),
        selected = dark,
        onSelect = { dark = it },
        label = { if (it) Res.string.dark.str() else Res.string.light.str() },
    )
    if (seed == null) {
        ErrorText(Res.string.unknown_color_format.str())
        return
    }
    val scheme = dynamicColorScheme(seedColor = seed.toColor(), isDark = dark, style = style)
    val groups = listOf(
        Res.string.primary to primaryRoles(scheme),
        Res.string.secondary to secondaryRoles(scheme),
        Res.string.tertiary to tertiaryRoles(scheme),
        Res.string.error_2 to errorRoles(scheme),
        Res.string.surfaces to surfaceRoles(scheme),
        Res.string.outline_and_inverse to outlineRoles(scheme),
    )
    groups.forEach { (title, roles) ->
        ResultCard(title.str()) {
            roles.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    pair.forEach { (name, color) ->
                        ColorSwatch(
                            color = color,
                            label = name,
                            modifier = Modifier.weight(1f),
                            caption = color.hex(),
                            onClick = { copy(color.hex()) },
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
    val all = groups.flatMap { it.second }
    ResultCard(Res.string.kotlin.str()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MonoText(SchemeExport.kotlin(dark, all), Modifier.weight(1f))
            CopyIconButton(SchemeExport.kotlin(dark, all))
        }
    }
    ResultCard(Res.string.css_variables.str()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MonoText(SchemeExport.css(all), Modifier.weight(1f))
            CopyIconButton(SchemeExport.css(all))
        }
    }
}

private fun role(name: String, color: Color): Pair<String, Rgba> = name to color.toRgba()

private fun primaryRoles(s: ColorScheme) = listOf(
    role("primary", s.primary),
    role("onPrimary", s.onPrimary),
    role("primaryContainer", s.primaryContainer),
    role("onPrimaryContainer", s.onPrimaryContainer),
)

private fun secondaryRoles(s: ColorScheme) = listOf(
    role("secondary", s.secondary),
    role("onSecondary", s.onSecondary),
    role("secondaryContainer", s.secondaryContainer),
    role("onSecondaryContainer", s.onSecondaryContainer),
)

private fun tertiaryRoles(s: ColorScheme) = listOf(
    role("tertiary", s.tertiary),
    role("onTertiary", s.onTertiary),
    role("tertiaryContainer", s.tertiaryContainer),
    role("onTertiaryContainer", s.onTertiaryContainer),
)

private fun errorRoles(s: ColorScheme) = listOf(
    role("error", s.error),
    role("onError", s.onError),
    role("errorContainer", s.errorContainer),
    role("onErrorContainer", s.onErrorContainer),
)

private fun surfaceRoles(s: ColorScheme) = listOf(
    role("background", s.background),
    role("onBackground", s.onBackground),
    role("surface", s.surface),
    role("onSurface", s.onSurface),
    role("surfaceVariant", s.surfaceVariant),
    role("onSurfaceVariant", s.onSurfaceVariant),
    role("surfaceContainerLowest", s.surfaceContainerLowest),
    role("surfaceContainerLow", s.surfaceContainerLow),
    role("surfaceContainer", s.surfaceContainer),
    role("surfaceContainerHigh", s.surfaceContainerHigh),
    role("surfaceContainerHighest", s.surfaceContainerHighest),
    role("surfaceBright", s.surfaceBright),
    role("surfaceDim", s.surfaceDim),
)

private fun outlineRoles(s: ColorScheme) = listOf(
    role("outline", s.outline),
    role("outlineVariant", s.outlineVariant),
    role("inverseSurface", s.inverseSurface),
    role("inverseOnSurface", s.inverseOnSurface),
    role("inversePrimary", s.inversePrimary),
    role("scrim", s.scrim),
)
