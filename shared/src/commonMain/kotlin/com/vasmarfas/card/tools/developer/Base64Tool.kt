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
import com.vasmarfas.card.core.saveBytes
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.tools.media.SaveButton
import com.vasmarfas.card.tools.text.OutputCard
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.LoadedFile
import com.vasmarfas.card.ui.components.LoadedFileCard
import com.vasmarfas.card.ui.components.OpenFileButton
import com.vasmarfas.card.ui.components.SwitchRow
import com.vasmarfas.card.ui.components.ToolInputField
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
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

// the encoded text is held in memory and shown in part, larger files are better done by a command line tool
private const val MAX_FILE_BYTES = 20L * 1024 * 1024

@Composable
private fun Base64Screen() {
    var mode by rememberSaveable { mutableStateOf(Base64Mode.TEXT_TO_BASE64) }
    var input by rememberSaveable { mutableStateOf("") }
    var urlSafe by rememberSaveable { mutableStateOf(false) }
    var padding by rememberSaveable { mutableStateOf(true) }
    var spacedHex by rememberSaveable { mutableStateOf(true) }
    var dataUri by rememberSaveable { mutableStateOf(false) }
    var file by remember { mutableStateOf<LoadedFile?>(null) }
    var fileError by remember { mutableStateOf<String?>(null) }
    val tooLarge = Res.string.file_over_20_mb.str()
    val encoding = mode == Base64Mode.TEXT_TO_BASE64 || mode == Base64Mode.TEXT_TO_HEX
    ChoiceChips(options = Base64Mode.entries, selected = mode, onSelect = { mode = it }, label = { it.title.str() })
    val loaded = file?.takeIf { encoding }
    if (loaded == null) {
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
            monospace = !encoding,
        )
    } else {
        LoadedFileCard(loaded) { file = null }
    }
    if (encoding) {
        OpenFileButton { picked ->
            fileError = null
            if (picked.size() > MAX_FILE_BYTES) fileError = tooLarge else file = LoadedFile(picked.name, picked.readBytes())
        }
        fileError?.let { ErrorText(it) }
    }
    when (mode) {
        Base64Mode.TEXT_TO_BASE64 -> {
            SwitchRow(Res.string.url_safe_alphabet_and.str(), urlSafe, { urlSafe = it })
            SwitchRow(Res.string.padding.str(), padding, { padding = it })
            if (loaded != null) SwitchRow(Res.string.as_data_uri.str(), dataUri, { dataUri = it }, description = Res.string.as_data_uri_hint.str())
        }
        Base64Mode.TEXT_TO_HEX -> SwitchRow(Res.string.separate_bytes_with_spaces.str(), spacedHex, { spacedHex = it })
        else -> {}
    }
    if (input.isEmpty() && loaded == null) return
    when (mode) {
        Base64Mode.TEXT_TO_BASE64 -> {
            val bytes = loaded?.bytes ?: input.encodeToByteArray()
            val encoded = remember(bytes, urlSafe, padding) { Base64Tools.encode(bytes, urlSafe, padding) }
            val mime = loaded?.let { FileSignatures.detect(it.bytes)?.mime ?: "application/octet-stream" }
            OutputCard(if (dataUri && mime != null) "data:$mime;base64,$encoded" else encoded)
            KeyValueRow(Res.string.input_bytes.str(), bytes.size.toString(), copyable = false)
        }

        Base64Mode.TEXT_TO_HEX -> {
            val bytes = loaded?.bytes ?: input.encodeToByteArray()
            val hex = remember(bytes, spacedHex) {
                val raw = bytes.toHex()
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
            val kind = remember(bytes) { FileSignatures.detect(bytes) }
            kind?.let { KeyValueRow(Res.string.mime_by_content.str(), it.mime) }
            SaveButton(Res.string.save_as_file.str()) { saveBytes(bytes, "decoded." + (kind?.extension?.ifEmpty { null } ?: "bin")) }
            if (mode == Base64Mode.BASE64_TO_TEXT) KeyValueRow("Hex", bytes.toHex())
            else KeyValueRow("Base64", Base64Tools.encode(bytes, urlSafe = false, padding = true))
        }
    }
}
