package com.vasmarfas.card.tools.developer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Token
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.vasmarfas.card.core.Tr
import com.vasmarfas.card.core.currentEpochMillis
import com.vasmarfas.card.core.formatDurationMs
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.tools.text.OutputCard
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.ToolInputField
import kotlinx.serialization.json.JsonPrimitive

val jwtDecoderTool = Tool(
    id = "jwt-decoder",
    category = ToolCategory.DEVELOPER,
    title = Res.string.jwt_decoder,
    description = Res.string.decode_header_and_payload_explain_standard_c,
    icon = Icons.Filled.Token,
    keywords = listOf("jwt", "token", "jws", "oauth", "oidc", "claims", "bearer", "токен", "джвт", "авторизация"),
) { JwtDecoderScreen() }

@Composable
private fun JwtDecoderScreen() {
    var input by rememberSaveable { mutableStateOf("") }
    ToolInputField(
        value = input,
        onValueChange = { input = it },
        label = Res.string.token.str(),
        singleLine = false,
        minLines = 4,
        placeholder = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.…",
        monospace = true,
    )
    if (input.isBlank()) return
    val result = remember(input) { Jwt.decode(input) }
    val parts = result.getOrNull()
    if (parts == null) {
        val error = result.exceptionOrNull()
        ErrorText(if (error is JwtException) error.text.str() else error?.message ?: "")
        return
    }
    val now = remember(input) { currentEpochMillis() / 1000 }
    val exp = parts.payload["exp"]?.let { Jwt.epochSeconds(it) }
    val nbf = parts.payload["nbf"]?.let { Jwt.epochSeconds(it) }
    ResultCard(Res.string.status.str()) {
        val status = when {
            exp != null && exp < now -> Tr("Expired ${formatDurationMs((now - exp) * 1000)} ago", "Истёк ${formatDurationMs((now - exp) * 1000)} назад").str()
            nbf != null && nbf > now -> Tr("Not valid yet, becomes valid in ${formatDurationMs((nbf - now) * 1000)}", "Ещё не действует, начнёт через ${formatDurationMs((nbf - now) * 1000)}").str()
            exp != null -> Tr("Valid for ${formatDurationMs((exp - now) * 1000)}", "Действителен ещё ${formatDurationMs((exp - now) * 1000)}").str()
            else -> Res.string.no_expiration_claim.str()
        }
        KeyValueRow(Res.string.validity.str(), status, mono = false, copyable = false)
        val alg = (parts.header["alg"] as? JsonPrimitive)?.content ?: "?"
        KeyValueRow(Res.string.algorithm.str(), alg, copyable = false)
        if (alg.equals("none", ignoreCase = true)) {
            ErrorText(Res.string.alg_none_the_token_is_unsigned.str())
        }
        KeyValueRow(Res.string.signature.str(), if (parts.signature.isEmpty()) "—" else "${parts.signature.length} ${Res.string.chars_base64url.str()}", copyable = false)
        Text(
            Res.string.the_signature_is_not_verified_here_a_well_fo.str(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    ResultCard(Res.string.claims.str()) {
        parts.payload.forEach { (key, value) ->
            val name = Jwt.claimNames[key]?.str()
            val label = if (name != null) "$key — $name" else key
            val text = Jwt.claimText(value)
            val seconds = if (key in Jwt.timeClaims) Jwt.epochSeconds(value) else null
            KeyValueRow(label, if (seconds != null) "$text  (${localDateTime(seconds * 1000).formatted()})" else text)
        }
    }
    OutputCard(JsonTools.format(parts.header, 2), title = Res.string.header.str())
    OutputCard(JsonTools.format(parts.payload, 2), title = Res.string.payload.str())
}
