package com.vasmarfas.card.tools.text

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.MonoTable
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SegmentedChoice
import com.vasmarfas.card.ui.components.SwitchRow
import com.vasmarfas.card.ui.components.ToolInputField
import kotlin.math.roundToInt

private enum class CipherMode { ENCRYPT, DECRYPT }

val ciphersTool = Tool(
    id = "ciphers",
    category = ToolCategory.TEXT,
    title = Res.string.classic_ciphers,
    description = Res.string.ciphers_description,
    icon = Icons.Filled.Lock,
    keywords = listOf("cipher", "caesar", "rot13", "rot47", "atbash", "vigenere", "encrypt", "decrypt", "шифр", "цезарь", "виженер", "атбаш"),
) { CiphersScreen() }

@Composable
private fun CiphersScreen() {
    var cipher by rememberSaveable { mutableStateOf(Cipher.CAESAR) }
    var mode by rememberSaveable { mutableStateOf(CipherMode.ENCRYPT) }
    var input by rememberSaveable { mutableStateOf("") }
    var key by rememberSaveable { mutableStateOf("") }
    var shift by rememberSaveable { mutableStateOf(3) }
    var bruteForce by rememberSaveable { mutableStateOf(false) }
    ChoiceChips(options = Cipher.entries, selected = cipher, onSelect = { cipher = it }, label = { it.title.str() })
    if (cipher == Cipher.CAESAR || cipher == Cipher.VIGENERE) {
        SegmentedChoice(
            options = CipherMode.entries,
            selected = mode,
            onSelect = { mode = it },
            label = { if (it == CipherMode.ENCRYPT) Res.string.encrypt.str() else Res.string.decrypt.str() },
        )
    }
    ToolInputField(
        value = input,
        onValueChange = { input = it },
        label = Res.string.text.str(),
        singleLine = false,
        minLines = 3,
    )
    when (cipher) {
        Cipher.CAESAR -> {
            Text(Res.string.shift.str() + ": $shift")
            Slider(
                value = shift.toFloat(),
                onValueChange = { shift = it.roundToInt() },
                valueRange = 1f..32f,
                steps = 30,
                modifier = Modifier.fillMaxWidth(),
            )
            SwitchRow(Res.string.brute_force_all_shifts.str(), bruteForce, { bruteForce = it })
        }

        Cipher.VIGENERE -> ToolInputField(
            value = key,
            onValueChange = { key = it },
            label = Res.string.key.str(),
            placeholder = "LEMON",
        )

        else -> {}
    }
    val output = remember(cipher, mode, input, key, shift) {
        val decrypt = mode == CipherMode.DECRYPT
        when (cipher) {
            Cipher.CAESAR -> Ciphers.caesar(input, if (decrypt) -shift else shift)
            Cipher.ROT13 -> Ciphers.rot13(input)
            Cipher.ROT47 -> Ciphers.rot47(input)
            Cipher.ATBASH -> Ciphers.atbash(input)
            Cipher.VIGENERE -> Ciphers.vigenere(input, key, decrypt)
            Cipher.REVERSE -> input.reversed()
        }
    }
    if (input.isNotEmpty()) {
        OutputCard(output)
        if (cipher == Cipher.CAESAR && bruteForce) {
            val rows = remember(input) { Ciphers.bruteForce(input).map { (s, text) -> "${s.toString().padStart(2)}  $text" } }
            ResultCard(Res.string.all_shifts_decryption.str()) {
                MonoTable(rows)
            }
        }
    }
}
