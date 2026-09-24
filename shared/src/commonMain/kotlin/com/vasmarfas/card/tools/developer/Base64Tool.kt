package com.vasmarfas.card.tools.developer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.tools.text.OutputCard
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.SwitchRow
import com.vasmarfas.card.ui.components.ToolInputField
import org.jetbrains.compose.resources.StringResource

private enum class Base64Mode(val title: StringResource) {
    TEXT_TO_BASE64(Res.string.text_base64),
    BASE64_TO_TEXT(Res.string.base64_text),
    TEXT_TO_HEX(Res.string.text_hex),
    HEX_TO_TEXT(Res.string.hex_text),
}

val base64Tool = Tool(
    id = "base64",
    category = ToolCategory.DEVELOPER,
    title = Res.string.base64_and_hex,
    description = Res.string.base64_description,
    icon = Icons.Filled.Code,
    keywords = listOf("base64", "hex", "encode", "decode", "binary", "url-safe", "кодирование", "декодирование"),
) { Base64Screen() }

@Composable
private fun Base64Screen() {
    var mode by rememberSaveable { mutableStateOf(Base64Mode.TEXT_TO_BASE64) }
    var input by rememberSaveable { mutableStateOf("") }
    var urlSafe by rememberSaveable { mutableStateOf(false) }
    var padding by rememberSaveable { mutableStateOf(true) }
    var spacedHex by rememberSaveable { mutableStateOf(true) }
    ChoiceChips(options = Base64Mode.entries, selected = mode, onSelect = { mode = it }, label = { it.title.str() })
    ToolInputField(
        value = input,
        onValueChange = { input = it },
        label = when (mode) {
            Base64Mode.TEXT_TO_BASE64, Base64Mode.TEXT_TO_HEX -> Res.string.text.str()
            Base64Mode.BASE64_TO_TEXT -> "Base64"
            Base64Mode.HEX_TO_TEXT -> "Hex"
        },
        singleLine = false,
        minLines = 4,
        monospace = mode != Base64Mode.TEXT_TO_BASE64 && mode != Base64Mode.TEXT_TO_HEX,
    )
    when (mode) {
        Base64Mode.TEXT_TO_BASE64 -> {
            SwitchRow(Res.string.url_safe_alphabet_and.str(), urlSafe, { urlSafe = it })
            SwitchRow(Res.string.padding.str(), padding, { padding = it })
        }
        Base64Mode.TEXT_TO_HEX -> SwitchRow(Res.string.separate_bytes_with_spaces.str(), spacedHex, { spacedHex = it })
        else -> {}
    }
    if (input.isEmpty()) return
    when (mode) {
        Base64Mode.TEXT_TO_BASE64 -> {
            val bytes = input.encodeToByteArray()
            OutputCard(remember(input, urlSafe, padding) { Base64Tools.encode(bytes, urlSafe, padding) })
            KeyValueRow(Res.string.input_bytes.str(), bytes.size.toString(), copyable = false)
        }

        Base64Mode.TEXT_TO_HEX -> {
            val hex = remember(input, spacedHex) {
                val raw = input.encodeToByteArray().toHex()
                if (spacedHex) raw.chunked(2).joinToString(" ") else raw
            }
            OutputCard(hex)
        }

        Base64Mode.BASE64_TO_TEXT, Base64Mode.HEX_TO_TEXT -> {
            val bytes = remember(input, mode) { if (mode == Base64Mode.BASE64_TO_TEXT) Base64Tools.decode(input) else hexToBytes(input) }
            if (bytes == null) {
                ErrorText(if (mode == Base64Mode.BASE64_TO_TEXT) Res.string.invalid_base64.str() else Res.string.invalid_hex.str())
                return
            }
            val text = remember(bytes) { Base64Tools.utf8OrNull(bytes) }
            if (text != null) {
                OutputCard(text)
            } else {
                Text(Res.string.base64_not_valid_utf_8.str(), style = MaterialTheme.typography.bodyMedium)
                OutputCard(Base64Tools.hexDump(bytes))
            }
            KeyValueRow(Res.string.decoded_bytes.str(), bytes.size.toString(), copyable = false)
            if (mode == Base64Mode.BASE64_TO_TEXT) KeyValueRow("Hex", bytes.toHex())
            else KeyValueRow("Base64", Base64Tools.encode(bytes, urlSafe = false, padding = true))
        }
    }
}
