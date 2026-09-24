package com.vasmarfas.card.tools.security

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.vasmarfas.card.core.fmt
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.ToolInputField

val passwordStrengthTool = Tool(
    id = "password-strength",
    category = ToolCategory.SECURITY,
    title = Res.string.password_strength,
    description = Res.string.password_strength_description,
    icon = Icons.Filled.Shield,
    keywords = listOf("password", "strength", "entropy", "crack", "audit", "weak", "пароль", "стойкость", "энтропия", "надёжность", "проверка"),
) { PasswordStrengthScreen() }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PasswordStrengthScreen() {
    var password by rememberSaveable { mutableStateOf("") }
    ToolInputField(
        value = password,
        onValueChange = { password = it },
        label = Res.string.password.str(),
        monospace = true,
    )
    Text(
        Res.string.strength_password_is_analyzed.str(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (password.isEmpty()) return
    val result = remember(password) { PasswordStrength.analyze(password) }
    LinearWavyProgressIndicator(
        progress = { (result.score + 1) / 5f },
        modifier = Modifier.fillMaxWidth(),
    )
    ResultCard {
        KeyValueRow(Res.string.rating.str(), PasswordStrength.scoreLabel(result.score).str(), mono = false, copyable = false)
        KeyValueRow(Res.string.entropy.str(), "${result.bits.fmt(1)} ${Res.string.unit_bit.str()}", copyable = false)
        KeyValueRow(Res.string.length.str(), result.length.toString(), copyable = false)
        KeyValueRow(Res.string.alphabet_size.str(), result.alphabetSize.toString(), copyable = false)
        if (result.rawBits - result.bits > 0.5) {
            KeyValueRow(
                Res.string.penalty_for_patterns.str(),
                "−${(result.rawBits - result.bits).fmt(1)} ${Res.string.unit_bit.str()}",
                copyable = false,
            )
        }
    }
    ResultCard(Res.string.crack_time.str()) {
        KeyValueRow(
            Res.string.online_throttled_10_4_s.str(),
            PasswordGen.crackTimeLabel(PasswordGen.crackTimeSeconds(result.bits, 1e4)).str(),
            mono = false,
            copyable = false,
        )
        KeyValueRow(
            Res.string.offline_slow_hash_10_9_s.str(),
            PasswordGen.crackTimeLabel(PasswordGen.crackTimeSeconds(result.bits, 1e9)).str(),
            mono = false,
            copyable = false,
        )
        KeyValueRow(
            Res.string.offline_fast_hash_10_12_s.str(),
            PasswordGen.crackTimeLabel(PasswordGen.crackTimeSeconds(result.bits, 1e12)).str(),
            mono = false,
            copyable = false,
        )
    }
    if (result.issues.isNotEmpty()) {
        ResultCard(Res.string.detected_patterns.str()) {
            result.issues.forEach { issue ->
                KeyValueRow(issue.text.str(), "−${issue.penaltyBits.fmt(1)} ${Res.string.unit_bit.str()}", mono = false, copyable = false)
            }
        }
    }
    ResultCard(Res.string.suggestions.str()) {
        result.suggestions.forEach { suggestion ->
            Text("• " + suggestion.str(), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
