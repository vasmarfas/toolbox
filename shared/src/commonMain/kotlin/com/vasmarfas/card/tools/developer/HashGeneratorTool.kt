package com.vasmarfas.card.tools.developer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tag
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
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SegmentedChoice
import com.vasmarfas.card.ui.components.SwitchRow
import com.vasmarfas.card.ui.components.ToolInputField
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private enum class HashOutput { HEX_LOWER, HEX_UPPER, BASE64 }

val hashGeneratorTool = Tool(
    id = "hash-generator",
    category = ToolCategory.DEVELOPER,
    title = Res.string.hash_generator,
    description = Res.string.md5_sha_1_sha_256_sha_512_crc32_and_hmac_of,
    icon = Icons.Filled.Tag,
    keywords = listOf("hash", "md5", "sha1", "sha256", "sha512", "crc32", "hmac", "checksum", "digest", "хеш", "контрольная сумма"),
) { HashGeneratorScreen() }

@OptIn(ExperimentalEncodingApi::class)
private fun encodeOutput(bytes: ByteArray, output: HashOutput): String = when (output) {
    HashOutput.HEX_LOWER -> bytes.toHex()
    HashOutput.HEX_UPPER -> bytes.toHex(upper = true)
    HashOutput.BASE64 -> Base64.Default.encode(bytes)
}

@Composable
private fun HashGeneratorScreen() {
    var input by rememberSaveable { mutableStateOf("") }
    var inputIsHex by rememberSaveable { mutableStateOf(false) }
    var key by rememberSaveable { mutableStateOf("") }
    var output by rememberSaveable { mutableStateOf(HashOutput.HEX_LOWER) }
    ToolInputField(
        value = input,
        onValueChange = { input = it },
        label = if (inputIsHex) Res.string.bytes_hex.str() else Res.string.text_utf_8.str(),
        singleLine = false,
        minLines = 3,
        monospace = inputIsHex,
    )
    SwitchRow(Res.string.input_is_hex_bytes.str(), inputIsHex, { inputIsHex = it })
    ToolInputField(
        value = key,
        onValueChange = { key = it },
        label = Res.string.hmac_key_optional.str(),
    )
    SegmentedChoice(
        options = HashOutput.entries,
        selected = output,
        onSelect = { output = it },
        label = {
            when (it) {
                HashOutput.HEX_LOWER -> "hex"
                HashOutput.HEX_UPPER -> "HEX"
                HashOutput.BASE64 -> "Base64"
            }
        },
    )
    val bytes = remember(input, inputIsHex) { if (inputIsHex) hexToBytes(input) else input.encodeToByteArray() }
    if (bytes == null) {
        ErrorText(Res.string.invalid_hex_input.str())
        return
    }
    val hashes = remember(bytes, output) {
        HashAlgorithm.entries.map { it.title to encodeOutput(it.digest(bytes), output) } +
            ("CRC32" to encodeOutput(Crc32.digest(bytes), output))
    }
    ResultCard {
        hashes.forEach { (name, value) -> KeyValueRow(name, value) }
        KeyValueRow(Res.string.input_size.str(), "${bytes.size} B", copyable = false)
    }
    if (key.isNotEmpty()) {
        val macs = remember(bytes, key, output) {
            listOf(HashAlgorithm.SHA1, HashAlgorithm.SHA256, HashAlgorithm.SHA512, HashAlgorithm.MD5).map {
                "HMAC-${it.title}" to encodeOutput(hmac(it, key.encodeToByteArray(), bytes), output)
            }
        }
        ResultCard("HMAC") {
            macs.forEach { (name, value) -> KeyValueRow(name, value) }
        }
    }
}
