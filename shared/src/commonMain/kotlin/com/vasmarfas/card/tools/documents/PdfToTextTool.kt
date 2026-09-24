package com.vasmarfas.card.tools.documents

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.vasmarfas.card.core.renamed
import com.vasmarfas.card.core.saveBytes
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.tools.documents.pdf.PdfText
import com.vasmarfas.card.tools.media.Hint
import com.vasmarfas.card.tools.media.PickButton
import com.vasmarfas.card.tools.media.SaveButton
import com.vasmarfas.card.tools.media.TaskProgress
import com.vasmarfas.card.tools.media.TaskState
import com.vasmarfas.card.ui.components.CopyIconButton
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SelectableText
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString

val pdfToTextTool = Tool(
    id = "pdf-to-text",
    category = ToolCategory.DOCUMENTS,
    title = Res.string.pdf_to_text,
    description = Res.string.pdf_to_text_description,
    icon = Icons.AutoMirrored.Filled.TextSnippet,
    keywords = listOf(
        "pdf to text", "extract text from pdf", "copy text from pdf", "pdf to txt",
        "текст из pdf", "извлечь текст", "скопировать текст из pdf", "pdf в txt", "pdf в текст",
    ),
) { PdfToTextScreen() }

private class Extracted(val text: String, val emptyPages: Int)

private const val MAX_SHOWN = 20_000

@Composable
private fun PdfToTextScreen() {
    val scope = rememberCoroutineScope()
    var opened by remember { mutableStateOf<OpenedPdf?>(null) }
    var extracted by remember { mutableStateOf<Extracted?>(null) }
    val task = remember { TaskState() }

    PickButton(Res.string.choose_pdf.str(), pdfExtensions, icon = Icons.Filled.PictureAsPdf, empty = opened == null) { files ->
        extracted = null
        scope.launch {
            val pdf = openPdf(files.first())
            opened = pdf
            val document = pdf.document ?: return@launch
            task.launch(scope) { progress ->
                val out = StringBuilder()
                var empty = 0
                for (i in 0 until document.pageCount) {
                    val page = withContext(Dispatchers.Default) { runCatching { PdfText.extract(document, i) }.getOrDefault("") }.trim()
                    if (page.isEmpty()) empty++
                    if (document.pageCount > 1) out.append(getString(Res.string.page_number, i + 1)).append("\n\n")
                    out.append(page).append("\n\n")
                    progress((i + 1f) / document.pageCount)
                }
                extracted = Extracted(out.toString().trimEnd() + "\n", empty)
            }
        }
    }
    val pdf = opened ?: return
    if (pdf.document == null) {
        ErrorText(pdf.detail)
        return
    }
    TaskProgress(task, Res.string.reading_document.str())
    val result = extracted ?: return
    if (result.emptyPages > 0) Hint(Res.string.no_text_layer.str())
    ResultCard {
        Row {
            Text("${pdf.file.name} · ${pdf.detail}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            CopyIconButton(result.text)
        }
        SelectableText(if (result.text.length > MAX_SHOWN) result.text.take(MAX_SHOWN) + "…" else result.text)
    }
    SaveButton(Res.string.save_txt.str()) { saveBytes(result.text.encodeToByteArray(), renamed(pdf.file.name, "txt")) }
}
