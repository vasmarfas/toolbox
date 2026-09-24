package com.vasmarfas.card.tools.documents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.tools.documents.pdf.PageRef
import com.vasmarfas.card.tools.documents.pdf.PdfAssembler
import com.vasmarfas.card.tools.documents.pdf.PdfDocument
import com.vasmarfas.card.tools.documents.pdf.PdfEncryptedException
import com.vasmarfas.card.tools.media.PickButton
import com.vasmarfas.card.tools.media.TaskProgress
import com.vasmarfas.card.tools.media.TaskState
import com.vasmarfas.card.ui.components.ActionButton
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

val mergePdfTool = Tool(
    id = "merge-pdf",
    category = ToolCategory.DOCUMENTS,
    title = Res.string.merge_pdf,
    description = Res.string.merge_pdf_description,
    icon = Icons.AutoMirrored.Filled.MergeType,
    keywords = listOf(
        "merge pdf", "join pdf", "combine pdf", "append pdf",
        "объединить pdf", "склеить pdf", "соединить pdf", "собрать pdf из нескольких",
    ),
) { MergePdfScreen() }

internal class OpenedPdf(val file: PlatformFile, val document: PdfDocument?, val detail: String)

internal suspend fun openPdf(file: PlatformFile, password: String = ""): OpenedPdf {
    val bytes = file.readBytes()
    return try {
        val document = withContext(Dispatchers.Default) { PdfDocument.parse(bytes, password) }
        OpenedPdf(file, document, getPluralString(Res.plurals.page_count, document.pageCount, document.pageCount))
    } catch (e: PdfEncryptedException) {
        OpenedPdf(file, null, getString(Res.string.pdf_protected))
    } catch (e: Exception) {
        OpenedPdf(file, null, getString(Res.string.not_a_pdf))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MergePdfScreen() {
    val scope = rememberCoroutineScope()
    val files = remember { mutableStateListOf<OpenedPdf>() }
    var result by remember { mutableStateOf<PdfResult?>(null) }
    val task = remember { TaskState() }

    fun add(picked: List<PlatformFile>) {
        result = null
        scope.launch { picked.forEach { files += openPdf(it) } }
    }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PickButton(Res.string.choose_pdfs.str(), pdfExtensions, multiple = true, icon = Icons.Filled.PictureAsPdf, empty = files.isEmpty()) {
            files.clear()
            add(it)
        }
        if (files.isNotEmpty()) PickButton(Res.string.add_files.str(), pdfExtensions, multiple = true, icon = Icons.Filled.Add) { add(it) }
    }
    if (files.isEmpty()) return
    OrderedFiles(
        names = files.map { it.file.name },
        details = files.map { it.detail },
        onMove = { from, to ->
            files.add(to, files.removeAt(from))
            result = null
        },
        onRemove = {
            files.removeAt(it)
            result = null
        },
    )
    val usable = files.mapNotNull { it.document }
    ActionButton(
        text = Res.string.merge.str(),
        icon = Icons.AutoMirrored.Filled.MergeType,
        enabled = !task.running && usable.size >= 2,
        onClick = {
            result = null
            task.launch(scope) {
                val pages = usable.flatMap { doc -> (0 until doc.pageCount).map { PageRef(doc, it) } }
                val bytes = withContext(Dispatchers.Default) { PdfAssembler.assemble(pages) }
                result = PdfResult("merged.pdf", bytes, pages.size)
            }
        },
    )
    TaskProgress(task, Res.string.rendering.str())
    result?.let { PdfResultCard(it) }
}
