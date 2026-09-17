package com.vasmarfas.card.tools.developer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.openUrl
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.Res
import com.vasmarfas.card.resources.markdown_preview
import com.vasmarfas.card.resources.preview
import com.vasmarfas.card.resources.renders_headings_bold_and_italic_inline_code
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.ToolInputField

private const val SAMPLE = """# Markdown preview

Supports **bold**, *italic*, `code`, ~~strikethrough~~ and [links](https://kotlinlang.org).

## Lists

- first item
- second item
  - nested item

1. step one
2. step two

> A blockquote.

```kotlin
fun main() {
    println("Hello")
}
```

---
"""

val markdownPreviewTool = Tool(
    id = "markdown-preview",
    category = ToolCategory.DEVELOPER,
    title = Res.string.markdown_preview,
    description = Res.string.renders_headings_bold_and_italic_inline_code,
    icon = Icons.AutoMirrored.Filled.Article,
    keywords = listOf("markdown", "md", "preview", "render", "readme", "разметка", "просмотр", "предпросмотр"),
    expandable = true,
) { MarkdownPreviewScreen() }

@Composable
private fun MarkdownPreviewScreen() {
    var input by rememberSaveable { mutableStateOf(SAMPLE) }
    ToolInputField(
        value = input,
        onValueChange = { input = it },
        label = "Markdown",
        singleLine = false,
        minLines = 8,
        monospace = true,
    )
    if (input.isBlank()) return
    val blocks = remember(input) { Markdown.blocks(input) }
    ResultCard(Res.string.preview.str()) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            blocks.forEach { block -> BlockView(block) }
        }
    }
}

@Composable
private fun BlockView(block: MdBlock) {
    when (block) {
        is MdBlock.Heading -> InlineText(
            block.text,
            style = when (block.level) {
                1 -> MaterialTheme.typography.headlineMedium
                2 -> MaterialTheme.typography.headlineSmall
                3 -> MaterialTheme.typography.titleLarge
                4 -> MaterialTheme.typography.titleMedium
                else -> MaterialTheme.typography.titleSmall
            },
            modifier = Modifier.padding(top = 6.dp),
        )

        is MdBlock.Paragraph -> InlineText(block.text)

        is MdBlock.ListItem -> Row(
            modifier = Modifier.fillMaxWidth().padding(start = (block.indent * 16).dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(block.marker, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(24.dp))
            InlineText(block.text)
        }

        is MdBlock.Quote -> Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.width(3.dp).fillMaxHeight().background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
            InlineText(block.text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        is MdBlock.Code -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(8.dp))
                .padding(12.dp),
        ) {
            if (block.language != null) {
                Text(block.language, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                block.code,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                softWrap = false,
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            )
        }

        MdBlock.Rule -> HorizontalDivider(Modifier.padding(vertical = 4.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InlineText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val spans = remember(text) { Markdown.spans(text) }
    val links = spans.filterIsInstance<MdSpan.Link>()
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(annotate(spans), style = style, color = color)
        if (links.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                links.forEach { link ->
                    Text(
                        text = link.url,
                        style = MaterialTheme.typography.labelSmall.copy(textDecoration = TextDecoration.Underline),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable { openUrl(link.url) },
                    )
                }
            }
        }
    }
}

@Composable
private fun annotate(spans: List<MdSpan>): AnnotatedString {
    val codeBackground = MaterialTheme.colorScheme.surfaceContainerHighest
    val linkColor = MaterialTheme.colorScheme.primary
    return buildAnnotatedString {
        spans.forEach { span ->
            when (span) {
                is MdSpan.Text -> append(span.text)
                is MdSpan.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(span.text) }
                is MdSpan.Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(span.text) }
                is MdSpan.BoldItalic -> withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) { append(span.text) }
                is MdSpan.Strike -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(span.text) }
                is MdSpan.Code -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)) { append(span.text) }
                is MdSpan.Link -> withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                    append(span.text.ifEmpty { span.url })
                }
            }
        }
    }
}
