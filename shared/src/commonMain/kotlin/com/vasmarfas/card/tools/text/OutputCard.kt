package com.vasmarfas.card.tools.text

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vasmarfas.card.core.fmtGrouped
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.ui.components.CopyIconButton
import com.vasmarfas.card.ui.components.Hint
import com.vasmarfas.card.ui.components.MonoText
import com.vasmarfas.card.ui.components.ResultCard
import org.jetbrains.compose.resources.stringResource

private const val SHOWN_CHARS = 20_000

@Composable
fun OutputCard(text: String, title: String = Res.string.result.str(), modifier: Modifier = Modifier) {
    ResultCard(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            CopyIconButton(text)
        }
        MonoText(if (text.length > SHOWN_CHARS) text.take(SHOWN_CHARS) + "…" else text)
        if (text.length > SHOWN_CHARS) Hint(stringResource(Res.string.output_shown_start, text.length.fmtGrouped()))
    }
}
