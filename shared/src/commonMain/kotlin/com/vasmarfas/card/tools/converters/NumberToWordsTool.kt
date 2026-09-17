package com.vasmarfas.card.tools.converters

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import com.vasmarfas.card.core.Lang
import com.vasmarfas.card.core.LocalLang
import com.vasmarfas.card.core.fmtGrouped
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SegmentedChoice
import com.vasmarfas.card.ui.components.ToolInputField

val numberToWordsTool = Tool(
    id = "number-to-words",
    category = ToolCategory.CONVERTERS,
    title = Res.string.number_to_words,
    description = Res.string.spells_numbers_and_money_amounts_in_english,
    icon = Icons.Filled.Spellcheck,
    keywords = listOf("words", "spell", "amount", "invoice", "прописью", "сумма", "рубли", "копейки", "счёт"),
) { NumberToWordsScreen() }

@Composable
private fun NumberToWordsScreen() {
    val uiLang = LocalLang.current
    var input by rememberSaveable { mutableStateOf("1234567.89") }
    var lang by rememberSaveable { mutableStateOf(if (uiLang == Lang.RU) WordsLang.RU else WordsLang.EN) }
    var currency by rememberSaveable { mutableStateOf(WordsCurrency.NONE) }
    val amount = remember(input) { NumberWords.parse(input) }
    ToolInputField(
        value = input,
        onValueChange = { input = it },
        label = Res.string.number.str(),
        placeholder = "2 500 000.50",
        keyboardType = KeyboardType.Decimal,
        isError = input.isNotBlank() && amount == null,
    )
    SegmentedChoice(
        options = WordsLang.entries,
        selected = lang,
        onSelect = { lang = it },
        label = { if (it == WordsLang.RU) "Русский" else "English" },
    )
    SegmentedChoice(
        options = WordsCurrency.entries,
        selected = currency,
        onSelect = { currency = it },
        label = {
            when (it) {
                WordsCurrency.NONE -> Res.string.number.str()
                WordsCurrency.RUB -> "RUB ₽"
                WordsCurrency.USD -> "USD \$"
            }
        },
    )
    if (input.isNotBlank() && amount == null) {
        ErrorText(Res.string.enter_a_number_with_up_to_18_integer_digits.str())
    }
    if (amount != null) {
        ResultCard {
            KeyValueRow(Res.string.in_words.str(), NumberWords.spellAmount(amount, lang, currency), mono = false)
            if (currency != WordsCurrency.NONE) {
                val (whole, cents) = NumberWords.roundedCents(amount)
                val symbol = if (currency == WordsCurrency.RUB) "₽" else "\$"
                val sign = if (amount.negative && (whole != 0L || cents != 0)) "-" else ""
                KeyValueRow(Res.string.amount_2.str(), "$sign${whole.fmtGrouped()}.${cents.toString().padStart(2, '0')} $symbol")
            }
        }
    }
}
