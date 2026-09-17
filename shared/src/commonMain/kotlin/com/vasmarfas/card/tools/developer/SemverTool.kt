package com.vasmarfas.card.tools.developer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
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
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.ToolInputField
import com.vasmarfas.card.ui.components.ToolSection

val semverTool = Tool(
    id = "semver",
    category = ToolCategory.DEVELOPER,
    title = Res.string.semantic_versioning,
    description = Res.string.parse_and_compare_versions_with_pre_release,
    icon = Icons.AutoMirrored.Filled.CompareArrows,
    keywords = listOf("semver", "version", "compare", "range", "npm", "caret", "tilde", "версия", "сравнение", "диапазон"),
) { SemverScreen() }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SemverScreen() {
    var left by rememberSaveable { mutableStateOf("1.4.2") }
    var right by rememberSaveable { mutableStateOf("1.10.0-rc.1") }
    var range by rememberSaveable { mutableStateOf("^1.4.0") }
    val a = remember(left) { SemVerOps.parse(left) }
    val b = remember(right) { SemVerOps.parse(right) }
    ToolInputField(
        value = left,
        onValueChange = { left = it },
        label = Res.string.version.str(),
        isError = left.isNotBlank() && a == null,
        monospace = true,
    )
    if (a == null) {
        if (left.isNotBlank()) ErrorText(Res.string.not_a_semantic_version_expected_major_minor.str())
        return
    }
    ResultCard {
        KeyValueRow("major", a.major.toString(), copyable = false)
        KeyValueRow("minor", a.minor.toString(), copyable = false)
        KeyValueRow("patch", a.patch.toString(), copyable = false)
        KeyValueRow("pre-release", a.preRelease ?: "—", copyable = false)
        KeyValueRow("build", a.build ?: "—", copyable = false)
        KeyValueRow(
            Res.string.kind.str(),
            when {
                a.preRelease != null -> Res.string.pre_release.str()
                a.major == 0 -> Res.string.initial_development_0_x.str()
                else -> Res.string.stable_release.str()
            },
            mono = false,
            copyable = false,
        )
    }
    ToolSection(Res.string.bump.str()) {
        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ActionButton("major → ${SemVerOps.bumpMajor(a)}", onClick = { left = SemVerOps.bumpMajor(a).toString() })
            ActionButton("minor → ${SemVerOps.bumpMinor(a)}", onClick = { left = SemVerOps.bumpMinor(a).toString() })
            ActionButton("patch → ${SemVerOps.bumpPatch(a)}", onClick = { left = SemVerOps.bumpPatch(a).toString() })
            ActionButton("pre → ${SemVerOps.bumpPreRelease(a)}", onClick = { left = SemVerOps.bumpPreRelease(a).toString() })
        }
    }
    ToolSection(Res.string.compare.str()) {
        ToolInputField(
            value = right,
            onValueChange = { right = it },
            label = Res.string.second_version.str(),
            isError = right.isNotBlank() && b == null,
            monospace = true,
        )
        if (b == null) {
            if (right.isNotBlank()) ErrorText(Res.string.not_a_semantic_version.str())
        } else {
            val cmp = a.compareTo(b)
            val sign = if (cmp < 0) "<" else if (cmp > 0) ">" else "="
            ResultCard {
                KeyValueRow(Res.string.result.str(), "$a $sign $b")
                KeyValueRow(
                    Res.string.newer.str(),
                    if (cmp == 0) Res.string.versions_are_equal_build_metadata_ignored.str() else (if (cmp > 0) a else b).toString(),
                    mono = false,
                    copyable = false,
                )
            }
        }
    }
    ToolSection(Res.string.range_check.str()) {
        ToolInputField(
            value = range,
            onValueChange = { range = it },
            label = Res.string.range.str(),
            placeholder = "^1.2.3 · ~1.2 · >=1.0.0 <2.0.0 · 1.x || 2.x",
            monospace = true,
        )
        if (range.isNotBlank()) {
            val ok = remember(a, range) { SemVerOps.satisfies(a, range) }
            ResultCard {
                KeyValueRow(
                    "$a ∈ $range",
                    if (ok) Res.string.yes_satisfies_the_range.str() else Res.string.no_outside_the_range.str(),
                    mono = false,
                    copyable = false,
                )
            }
        }
    }
}
