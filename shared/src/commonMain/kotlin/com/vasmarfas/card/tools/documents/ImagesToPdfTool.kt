package com.vasmarfas.card.tools.documents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.PickKind
import com.vasmarfas.card.core.formatBytes
import com.vasmarfas.card.core.limitedTo
import com.vasmarfas.card.core.renamed
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.tools.media.ImageTarget
import com.vasmarfas.card.tools.media.PickButton
import com.vasmarfas.card.tools.media.TaskProgress
import com.vasmarfas.card.tools.media.TaskState
import com.vasmarfas.card.tools.media.decodeImage
import com.vasmarfas.card.tools.media.encodeImage
import com.vasmarfas.card.tools.media.imageExtensions
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.ToolSection
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val imagesToPdfTool = Tool(
    id = "images-to-pdf",
    category = ToolCategory.DOCUMENTS,
    title = Res.string.images_to_pdf,
    description = Res.string.images_to_pdf_description,
    icon = Icons.Filled.PictureAsPdf,
    keywords = listOf(
        "images to pdf", "jpg to pdf", "png to pdf", "photo to pdf", "scan to pdf", "picture to pdf",
        "фото в pdf", "картинки в pdf", "jpg в pdf", "сканы в pdf", "сделать pdf из фото", "объединить фото в pdf",
    ),
) { ImagesToPdfScreen() }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImagesToPdfScreen() {
    val scope = rememberCoroutineScope()
    val files = remember { mutableStateListOf<PlatformFile>() }
    var format by rememberSaveable { mutableStateOf<PageFormat?>(PageFormat.A4) }
    var margin by rememberSaveable { mutableStateOf(14.17f) }
    var result by remember { mutableStateOf<PdfResult?>(null) }
    val task = remember { TaskState() }
    val unreadable = Res.string.image_not_readable.str()

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PickButton(Res.string.choose_images.str(), imageExtensions, PickKind.IMAGE, multiple = true, icon = Icons.Filled.AddPhotoAlternate, empty = files.isEmpty()) {
            files.clear()
            files += it
            result = null
        }
        if (files.isNotEmpty()) {
            PickButton(Res.string.add_files.str(), imageExtensions, PickKind.IMAGE, multiple = true, icon = Icons.Filled.Add) {
                files += it
                result = null
            }
        }
    }
    if (files.isEmpty()) return
    OrderedFiles(
        names = files.map { it.name },
        details = files.map { formatBytes(it.size(), binary = false) },
        onMove = { from, to -> files.add(to, files.removeAt(from)) },
        onRemove = { files.removeAt(it) },
        icon = Icons.Filled.Image,
    )

    ToolSection(Res.string.page_size.str()) {
        ChoiceChips(
            options = listOf(PageFormat.A4, PageFormat.A5, PageFormat.LETTER, null),
            selected = format,
            onSelect = { format = it },
            label = {
                when (it) {
                    null -> Res.string.page_like_picture.str()
                    PageFormat.LETTER -> "Letter"
                    else -> it.name
                }
            },
        )
    }
    ToolSection(Res.string.margins.str()) {
        ChoiceChips(
            options = listOf(0f, 14.17f, 42.52f),
            selected = margin,
            onSelect = { margin = it },
            label = {
                when (it) {
                    0f -> Res.string.margins_none.str()
                    14.17f -> Res.string.margins_narrow.str()
                    else -> Res.string.margins_normal.str()
                }
            },
        )
    }
    ActionButton(
        text = Res.string.make_pdf.str(),
        icon = Icons.Filled.PictureAsPdf,
        enabled = !task.running,
        onClick = {
            result = null
            val sources = files.toList()
            val page = format
            val border = margin
            task.launch(scope) { progress ->
                val images = sources.mapIndexed { i, file ->
                    val image = decodeImage(file.readBytes())?.limitedTo(if (page == null) 3000 else 2400)
                        ?: throw IllegalStateException("${file.name}: $unreadable")
                    progress((i + 1f) / (sources.size + 1))
                    PreparedImage(withContext(Dispatchers.Default) { encodeImage(image, ImageTarget.JPEG, 85) }, image.width, image.height)
                }
                val bytes = withContext(Dispatchers.Default) { ImagePdf.build(images, page, border) }
                result = PdfResult(renamed(sources.first().name, "pdf"), bytes, images.size)
            }
        },
    )
    TaskProgress(task, Res.string.rendering.str())
    result?.let { PdfResultCard(it) }
}
