package com.vasmarfas.card.tools.fitness

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.vasmarfas.card.core.fmt
import com.vasmarfas.card.core.str
import com.vasmarfas.card.core.toDoubleLenient
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.Hint
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SegmentedChoice

val hba1cTool = Tool(
    id = "hba1c-glucose",
    category = ToolCategory.FITNESS,
    title = Res.string.hba1c,
    description = Res.string.hba1c_glucose_description,
    icon = Icons.Filled.Bloodtype,
    keywords = listOf(
        "hba1c", "a1c", "glucose", "diabetes", "ifcc", "dcct", "eag",
        "гликированный", "гемоглобин", "глюкоза", "сахар", "диабет",
    ),
) { HbA1cScreen() }

private enum class A1cInput { PERCENT, MMOL_PER_MOL, AVERAGE_GLUCOSE }

@Composable
private fun HbA1cScreen() {
    var mode by rememberSaveable { mutableStateOf(A1cInput.PERCENT) }
    var text by rememberSaveable { mutableStateOf("6.5") }

    Hint(Res.string.hba1c_intro.str())
    SegmentedChoice(
        options = A1cInput.entries,
        selected = mode,
        onSelect = { mode = it },
        label = {
            when (it) {
                A1cInput.PERCENT -> Res.string.a1c_dcct.str()
                A1cInput.MMOL_PER_MOL -> Res.string.a1c_ifcc.str()
                A1cInput.AVERAGE_GLUCOSE -> Res.string.average_glucose.str()
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
    NumberField(
        value = text,
        onValueChange = { text = it },
        label = when (mode) {
            A1cInput.PERCENT -> Res.string.a1c_dcct.str()
            A1cInput.MMOL_PER_MOL -> Res.string.a1c_ifcc.str()
            A1cInput.AVERAGE_GLUCOSE -> Res.string.average_glucose.str()
        },
        suffix = when (mode) {
            A1cInput.PERCENT -> "%"
            A1cInput.MMOL_PER_MOL -> Res.string.unit_mmol_per_mol.str()
            A1cInput.AVERAGE_GLUCOSE -> Res.string.unit_mmol_per_l.str()
        },
        modifier = Modifier.fillMaxWidth(),
        isError = text.toDoubleLenient().let { it == null || it <= 0 },
    )

    val entered = text.toDoubleLenient()
    if (entered == null || entered <= 0) {
        ErrorText(Res.string.enter_a_positive_number.str())
        return
    }

    val a1c = when (mode) {
        A1cInput.PERCENT -> entered
        A1cInput.MMOL_PER_MOL -> Glycemia.dcctFromIfcc(entered)
        A1cInput.AVERAGE_GLUCOSE -> Glycemia.a1cFromEagMmolL(entered)
    }
    if (a1c <= 0 || a1c > 25) {
        ErrorText(Res.string.a1c_out_of_range.str())
        return
    }

    ResultCard {
        KeyValueRow(
            Res.string.a1c_meaning.str(),
            when (Glycemia.category(a1c)) {
                0 -> Res.string.a1c_normal
                1 -> Res.string.a1c_prediabetes
                else -> Res.string.a1c_diabetes
            }.str(),
            mono = false,
            copyable = false,
        )
        KeyValueRow(Res.string.a1c_dcct.str(), "${a1c.fmt(2)} %", copyable = false)
        KeyValueRow(
            Res.string.a1c_ifcc.str(),
            "${Glycemia.ifccMmolMol(a1c).fmt(1)} ${Res.string.unit_mmol_per_mol.str()}",
            copyable = false,
        )
        KeyValueRow(
            Res.string.average_glucose.str(),
            "${Glycemia.eagMmolL(a1c).fmt(2)} ${Res.string.unit_mmol_per_l.str()}",
            copyable = false,
        )
        KeyValueRow(
            Res.string.average_glucose.str(),
            "${Glycemia.eagMgDl(a1c).fmt(0)} ${Res.string.unit_mg_per_dl.str()}",
            copyable = false,
        )
        Hint(Res.string.a1c_average_hint.str())
    }
    Text(
        Res.string.hba1c_note.str(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
