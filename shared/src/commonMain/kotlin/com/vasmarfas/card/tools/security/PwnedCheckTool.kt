package com.vasmarfas.card.tools.security

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.vasmarfas.card.core.LocalLang
import com.vasmarfas.card.core.Net
import com.vasmarfas.card.core.Tr
import com.vasmarfas.card.core.fmtGrouped
import com.vasmarfas.card.core.openUrl
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.tools.developer.Sha1
import com.vasmarfas.card.tools.developer.toHex
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.LoadingRow
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.ToolInputField
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.launch

private const val API = "https://api.pwnedpasswords.com/range/"

val pwnedCheckTool = Tool(
    id = "pwned-check",
    category = ToolCategory.SECURITY,
    title = Res.string.leaked_password_check,
    description = Res.string.checks_a_password_against_the_have_i_been_pw,
    icon = Icons.Filled.Verified,
    keywords = listOf("pwned", "leak", "breach", "haveibeenpwned", "hibp", "compromised", "утечка", "слив", "скомпрометирован", "пароль"),
) { PwnedCheckScreen() }

@Composable
private fun PwnedCheckScreen() {
    val requestFailedText = Res.string.request_failed.str()
    var password by rememberSaveable { mutableStateOf("") }
    var loading by rememberSaveable { mutableStateOf(false) }
    var count by rememberSaveable { mutableStateOf(-1) }
    var prefix by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    ToolInputField(
        value = password,
        onValueChange = {
            password = it
            count = -1
            error = null
        },
        label = Res.string.password.str(),
        monospace = true,
    )
    ActionButton(
        text = Res.string.check.str(),
        icon = Icons.Filled.Search,
        enabled = password.isNotEmpty() && !loading,
        onClick = {
            scope.launch {
                loading = true
                error = null
                count = -1
                try {
                    val hash = Sha1.digest(password.encodeToByteArray()).toHex(upper = true)
                    prefix = hash.substring(0, 5)
                    val suffix = hash.substring(5)
                    val body = Net.client.get(API + prefix).bodyAsText()
                    val line = body.lineSequence().firstOrNull { it.substringBefore(':').trim().equals(suffix, ignoreCase = true) }
                    count = line?.substringAfter(':')?.trim()?.replace(",", "")?.toIntOrNull() ?: 0
                } catch (e: Exception) {
                    error = e.message ?: requestFailedText
                } finally {
                    loading = false
                }
            }
        },
    )
    if (loading) LoadingRow(Res.string.querying_the_breach_database.str())
    error?.let { ErrorText(it) }
    if (!loading && count >= 0) {
        ResultCard {
            if (count == 0) {
                KeyValueRow(
                    Res.string.result.str(),
                    Res.string.not_found_in_the_corpus.str(),
                    mono = false,
                    copyable = false,
                )
                Text(
                    Res.string.absence_from_the_corpus_does_not_make_a_weak.str(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                KeyValueRow(
                    Res.string.result.str(),
                    Tr("Found in breaches ${count.fmtGrouped()} times", "Встречается в утечках ${count.fmtGrouped()} раз").str(),
                    mono = false,
                    copyable = false,
                )
                ErrorText(Res.string.stop_using_this_password_everywhere.str())
            }
            KeyValueRow(Res.string.hash_prefix_sent.str(), prefix)
        }
    }
    ResultCard(Res.string.how_it_works.str()) {
        Text(
            Res.string.the_password_is_hashed_with_sha_1_on_the_dev.str(),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "haveibeenpwned.com",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { openUrl("https://haveibeenpwned.com/Passwords") },
        )
    }
}
