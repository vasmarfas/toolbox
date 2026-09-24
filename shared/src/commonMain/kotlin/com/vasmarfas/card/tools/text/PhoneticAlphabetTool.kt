package com.vasmarfas.card.tools.text

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.LocalLang
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.ToolInputField

val phoneticAlphabetTool = Tool(
    id = "phonetic-alphabet",
    category = ToolCategory.TEXT,
    title = Res.string.phonetic_alphabet,
    description = Res.string.phonetic_alphabet_description,
    icon = Icons.Filled.RecordVoiceOver,
    keywords = listOf(
        "nato", "spelling", "alphabet", "alfa bravo", "phone", "dictate",
        "фонетический", "по буквам", "диктовка", "анна борис", "алфавит", "телефон",
    ),
) { PhoneticAlphabetScreen() }

@Composable
private fun PhoneticAlphabetScreen() {
    var input by rememberSaveable { mutableStateOf("") }
    ToolInputField(
        value = input,
        onValueChange = { input = it },
        label = Res.string.phonetic_input.str(),
        placeholder = "AB12CD · Иванов",
    )
    if (input.isBlank()) return
    val spelled = PhoneticAlphabet.spell(input.trim(), LocalLang.current)
    val space = Res.string.phonetic_space.str()
    OutputCard(
        spelled.joinToString(", ") { it.word ?: if (it.char == ' ') space else it.char.toString() },
        title = Res.string.phonetic_spelled.str(),
    )
    ResultCard {
        spelled.forEach { item ->
            Row {
                Text(
                    item.char.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                    modifier = Modifier.width(40.dp),
                )
                Text(item.word ?: if (item.char == ' ') space else "—", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
