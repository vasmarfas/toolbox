package com.vasmarfas.card.tools.calculators

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Percent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.vasmarfas.card.core.str
import com.vasmarfas.card.core.toDoubleLenient
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.tools.converters.fmtSig
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ResultCard
import org.jetbrains.compose.resources.StringResource

private enum class PercentForm(val title: StringResource, val first: StringResource, val second: StringResource) {
    OF(Res.string.x_of_y, Res.string.percent_x, Res.string.value_y),
    RATIO(Res.string.x_is_what_of_y, Res.string.part_x, Res.string.whole_y),
    CHANGE(Res.string.change_from_x_to_y, Res.string.from_x, Res.string.to_y),
    ADJUST(Res.string.y_plus_or_minus_x, Res.string.percent_x, Res.string.value_y),
}

val percentageTool = Tool(
    id = "percentage",
    category = ToolCategory.CALCULATORS,
    title = Res.string.percentage_calculator,
    description = Res.string.percent_of_a_number_share_as_a_percent_perce,
    icon = Icons.Filled.Percent,
    keywords = listOf("percent", "ratio", "increase", "decrease", "проценты", "доля", "изменение", "прибавить", "вычесть"),
) { PercentageScreen() }

@Composable
private fun PercentageScreen() {
    var form by rememberSaveable { mutableStateOf(PercentForm.OF) }
    var xText by rememberSaveable { mutableStateOf("15") }
    var yText by rememberSaveable { mutableStateOf("200") }
    val x = xText.toDoubleLenient()
    val y = yText.toDoubleLenient()
    ChoiceChips(
        options = PercentForm.entries,
        selected = form,
        onSelect = { form = it },
        label = { it.title.str() },
    )
    NumberField(
        value = xText,
        onValueChange = { xText = it },
        label = form.first.str(),
        suffix = if (form == PercentForm.OF || form == PercentForm.ADJUST) "%" else null,
        isError = xText.isNotBlank() && x == null,
    )
    NumberField(
        value = yText,
        onValueChange = { yText = it },
        label = form.second.str(),
        isError = yText.isNotBlank() && y == null,
    )
    if (x != null && y != null) {
        ResultCard {
            when (form) {
                PercentForm.OF -> KeyValueRow("${x.fmtSig()}% × ${y.fmtSig()}", Percentage.percentOf(x, y).fmtSig())
                PercentForm.RATIO -> {
                    KeyValueRow("${x.fmtSig()} / ${y.fmtSig()}", if (y == 0.0) "—" else "${Percentage.whatPercent(x, y).fmtSig()}%")
                }
                PercentForm.CHANGE -> {
                    KeyValueRow(Res.string.change.str(), if (x == 0.0) "—" else "${Percentage.change(x, y).fmtSig()}%")
                    KeyValueRow(Res.string.difference.str(), (y - x).fmtSig())
                }
                PercentForm.ADJUST -> {
                    KeyValueRow("${y.fmtSig()} + ${x.fmtSig()}%", Percentage.addPercent(y, x).fmtSig())
                    KeyValueRow("${y.fmtSig()} − ${x.fmtSig()}%", Percentage.subtractPercent(y, x).fmtSig())
                    KeyValueRow("${x.fmtSig()}% × ${y.fmtSig()}", Percentage.percentOf(x, y).fmtSig())
                }
            }
        }
    }
}
