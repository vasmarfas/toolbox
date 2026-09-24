package com.vasmarfas.card.tools.developer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Functions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import com.vasmarfas.card.core.str
import com.vasmarfas.card.core.toDoubleLenient
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.tools.converters.fmtReadable
import com.vasmarfas.card.tools.converters.fmtSig
import com.vasmarfas.card.tools.text.OutputCard
import com.vasmarfas.card.ui.components.AnswerCard
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SegmentedChoice
import com.vasmarfas.card.ui.components.ToolInputField
import com.vasmarfas.card.ui.theme.LocalStatusColors
import org.jetbrains.compose.resources.StringResource

val floatInspectorTool = Tool(
    id = "float-inspector",
    category = ToolCategory.DEVELOPER,
    title = Res.string.float_inspector,
    description = Res.string.float_inspector_description,
    icon = Icons.Filled.Functions,
    keywords = listOf(
        "ieee 754", "float", "double", "mantissa", "exponent", "bits", "precision", "0.1 + 0.2",
        "плавающая точка", "мантисса", "порядок", "точность", "биты", "дробное",
    ),
) { FloatInspectorScreen() }

private fun kindLabel(kind: FloatClass): StringResource = when (kind) {
    FloatClass.ZERO -> Res.string.float_kind_zero
    FloatClass.SUBNORMAL -> Res.string.float_kind_subnormal
    FloatClass.NORMAL -> Res.string.float_kind_normal
    FloatClass.INFINITE -> Res.string.float_kind_infinite
    FloatClass.NAN -> Res.string.float_kind_nan
}

@Composable
private fun FloatInspectorScreen() {
    var input by rememberSaveable { mutableStateOf("0.1") }
    var format by rememberSaveable { mutableStateOf(FloatFormat.SINGLE) }
    val text = input.trim()
    val decimal = if (text.startsWith("0x", ignoreCase = true)) null else text.toDoubleLenient()
    val layout = if (decimal != null) FloatBits.of(decimal, format) else FloatBits.fromHex(text, format)
    ToolInputField(
        value = input,
        onValueChange = { input = it },
        label = Res.string.float_input.str(),
        placeholder = "0.1 · 1e-7 · 0x3DCCCCCD",
        keyboardType = KeyboardType.Ascii,
        isError = text.isNotEmpty() && layout == null,
        monospace = true,
    )
    SegmentedChoice(
        options = FloatFormat.entries,
        selected = format,
        onSelect = { format = it },
        label = { if (it == FloatFormat.SINGLE) Res.string.float_single.str() else Res.string.float_double.str() },
    )
    if (layout == null) {
        if (text.isNotEmpty()) ErrorText(Res.string.float_invalid.str())
        return
    }
    val finite = layout.value.isFinite()
    AnswerCard(
        if (finite) layout.value.fmtSig(if (format == FloatFormat.SINGLE) 9 else 17) else layout.value.toString(),
        Res.string.float_stored_as.str(),
    )
    val status = LocalStatusColors.current
    val bits = layout.binary
    ResultCard(Res.string.float_bits.str()) {
        KeyValueRow(
            "${Res.string.float_sign.str()} · ${Res.string.float_exponent.str()} · ${Res.string.float_mantissa.str()}",
            buildAnnotatedString {
                withStyle(SpanStyle(color = status.bad)) { append(bits.take(1)) }
                append(" ")
                withStyle(SpanStyle(color = status.info)) { append(bits.substring(1, 1 + format.exponentBits)) }
                append(" ")
                withStyle(SpanStyle(color = status.good)) { append(bits.drop(1 + format.exponentBits)) }
            },
            copyValue = bits,
        )
        KeyValueRow("Hex", "0x${layout.hex}")
        KeyValueRow(Res.string.float_sign.str(), if (layout.sign == 1) "1 (−)" else "0 (+)", copyable = false)
        KeyValueRow(Res.string.float_exponent.str(), "${layout.exponentField} − ${format.bias} = ${layout.exponent}", copyable = false)
        KeyValueRow(Res.string.float_class.str(), kindLabel(layout.kind).str(), mono = false, copyable = false)
    }
    if (!finite) return
    OutputCard(FloatBits.exactDecimal(layout.value), title = Res.string.float_exact_value.str())
    ResultCard {
        if (decimal != null && format == FloatFormat.SINGLE) {
            val error = layout.value - decimal
            KeyValueRow(Res.string.float_error.str(), error.fmtReadable(3), copyValue = error.fmtSig(3))
        }
        val up = FloatBits.nextUp(layout)
        val ulp = up - layout.value
        KeyValueRow(Res.string.float_ulp.str(), ulp.fmtReadable(3), copyValue = ulp.fmtSig(3))
        KeyValueRow(Res.string.float_next_up.str(), up.fmtSig(17))
        KeyValueRow(Res.string.float_next_down.str(), FloatBits.nextDown(layout).fmtSig(17))
    }
}
