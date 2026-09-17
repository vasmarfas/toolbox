package com.vasmarfas.card.tools.calculators

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.tools.converters.fmtSig
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SegmentedChoice
import com.vasmarfas.card.ui.components.ToolInputField

private class HistoryEntry(val expression: String, val result: String)

private val inserts = listOf("sin(", "cos(", "tan(", "sqrt(", "ln(", "log(", "^", "!", "pi", "e", "(", ")")

val calculatorTool = Tool(
    id = "calculator",
    category = ToolCategory.CALCULATORS,
    title = Res.string.scientific_calculator,
    description = Res.string.expressions_with_parentheses_powers_trigonom,
    icon = Icons.Filled.Calculate,
    keywords = listOf("math", "expression", "sin", "cos", "sqrt", "log", "factorial", "математика", "выражение", "корень", "синус", "логарифм", "факториал"),
) { CalculatorScreen() }

@Composable
private fun CalculatorScreen() {
    var input by rememberSaveable { mutableStateOf("") }
    var degrees by rememberSaveable { mutableStateOf(true) }
    val history = remember { mutableStateListOf<HistoryEntry>() }
    val evaluation = remember(input, degrees) {
        if (input.isBlank()) null else runCatching { ExpressionParser.evaluate(input, degrees) }
    }
    val result = evaluation?.getOrNull()
    val failure = evaluation?.exceptionOrNull()
    ToolInputField(
        value = input,
        onValueChange = { input = it },
        label = Res.string.expression.str(),
        placeholder = "2(3+4)^2 - sin(30) + 5!",
        isError = failure != null,
        trailingIcon = if (input.isEmpty()) null else {
            {
                IconButton(onClick = { input = "" }) {
                    Icon(Icons.Filled.Clear, contentDescription = Res.string.clear.str())
                }
            }
        },
        monospace = true,
    )
    SegmentedChoice(
        options = listOf(true, false),
        selected = degrees,
        onSelect = { degrees = it },
        label = { if (it) Res.string.degrees.str() else Res.string.radians.str() },
    )
    ChoiceChips(
        options = inserts,
        selected = null,
        onSelect = { input += it },
        label = { it },
    )
    if (failure != null) {
        ErrorText((failure as? ExpressionException)?.error?.str() ?: failure.message ?: Res.string.error.str())
    }
    if (result != null) {
        val formatted = result.fmtSig(12)
        ResultCard {
            KeyValueRow("=", formatted)
            ActionButton(
                text = Res.string.save_to_history.str(),
                onClick = {
                    history.add(0, HistoryEntry(input, formatted))
                    if (history.size > 10) history.removeAt(history.lastIndex)
                },
                icon = Icons.Filled.History,
            )
        }
    }
    if (history.isNotEmpty()) {
        ResultCard(Res.string.history_tap_to_reuse.str()) {
            history.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { input = entry.result }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        entry.expression,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text("= ${entry.result}", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
                }
            }
            ActionButton(text = Res.string.clear_history.str(), onClick = { history.clear() })
        }
    }
}
