package com.vasmarfas.card.tools.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.fmt
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.ToolInputField

val contrastCheckerTool = Tool(
    id = "contrast-checker",
    category = ToolCategory.DESIGN,
    title = Res.string.contrast_checker,
    description = Res.string.wcag_2_1_contrast_ratio_for_a_text_and_backg,
    icon = Icons.Filled.Contrast,
    keywords = listOf("wcag", "contrast", "accessibility", "aa", "aaa", "контраст", "доступность", "читаемость"),
) { ContrastCheckerScreen() }

@Composable
private fun ContrastCheckerScreen() {
    var fgText by rememberSaveable { mutableStateOf("#767676") }
    var bgText by rememberSaveable { mutableStateOf("#FFFFFF") }
    val fg = remember(fgText) { ColorMath.parse(fgText) }
    val bg = remember(bgText) { ColorMath.parse(bgText) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        ToolInputField(
            value = fgText,
            onValueChange = { fgText = it },
            label = Res.string.text.str(),
            modifier = Modifier.weight(1f),
            isError = fg == null,
            monospace = true,
        )
        ToolInputField(
            value = bgText,
            onValueChange = { bgText = it },
            label = Res.string.background.str(),
            modifier = Modifier.weight(1f),
            isError = bg == null,
            monospace = true,
        )
    }
    ActionButton(
        text = Res.string.swap.str(),
        onClick = {
            val t = fgText
            fgText = bgText
            bgText = t
        },
    )
    if (fg == null || bg == null) {
        ErrorText(Res.string.unknown_color_format.str())
        return
    }
    val check = Wcag.check(fg, bg)
    Preview(fg, bg)
    ResultCard {
        KeyValueRow(Res.string.contrast_ratio.str(), "${Wcag.rounded(check.ratio).fmt(2)} : 1")
        KeyValueRow(Res.string.aa_normal_text.str(), verdict(check.aaNormal), mono = false, copyable = false)
        KeyValueRow(Res.string.aa_large_text.str(), verdict(check.aaLarge), mono = false, copyable = false)
        KeyValueRow(Res.string.aaa_normal_text.str(), verdict(check.aaaNormal), mono = false, copyable = false)
        KeyValueRow(Res.string.aaa_large_text.str(), verdict(check.aaaLarge), mono = false, copyable = false)
    }
    if (!check.aaNormal) {
        val fixed = Wcag.nearestPassing(fg, bg, 4.5)
        ResultCard(Res.string.nearest_passing_text_color.str()) {
            if (fixed == null) {
                Text(Res.string.no_lightness_of_this_hue_reaches_4_5_1_on_th.str())
            } else {
                WideSwatch(fixed, fixed.hex(), "${Wcag.rounded(Wcag.ratio(fixed, bg)).fmt(2)} : 1")
                ActionButton(text = Res.string.use_it.str(), onClick = { fgText = fixed.hex() })
            }
        }
    }
}

@Composable
private fun verdict(pass: Boolean): String = (if (pass) Res.string.pass_ else Res.string.fail).str()

@Composable
private fun Preview(fg: Rgba, bg: Rgba) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg.toColor())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = Res.string.large_text_24_sp.str(),
            style = MaterialTheme.typography.headlineSmall,
            color = fg.toColor(),
        )
        Text(
            text = Res.string.normal_body_text_at_14_sp_for_reading_long_p.str(),
            style = MaterialTheme.typography.bodyMedium,
            color = fg.toColor(),
        )
    }
}
