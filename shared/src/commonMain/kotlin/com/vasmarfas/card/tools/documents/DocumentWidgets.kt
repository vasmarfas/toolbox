package com.vasmarfas.card.tools.documents

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.vasmarfas.card.core.fmt
import com.vasmarfas.card.core.formatBytes
import com.vasmarfas.card.core.limitedTo
import com.vasmarfas.card.core.saveBytes
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.documents.pdf.PdfArray
import com.vasmarfas.card.tools.documents.pdf.PdfAssembler
import com.vasmarfas.card.tools.documents.pdf.PdfDict
import com.vasmarfas.card.tools.documents.pdf.PdfInt
import com.vasmarfas.card.tools.documents.pdf.PdfName
import com.vasmarfas.card.tools.documents.pdf.PdfObject
import com.vasmarfas.card.tools.documents.pdf.PdfReal
import com.vasmarfas.card.tools.documents.pdf.PdfString
import com.vasmarfas.card.tools.documents.pdf.PdfWriter
import com.vasmarfas.card.tools.documents.pdf.TrueTypeFont
import com.vasmarfas.card.tools.media.FileLine
import com.vasmarfas.card.tools.media.ImageTarget
import com.vasmarfas.card.tools.media.SaveButton
import com.vasmarfas.card.tools.media.decodeImage
import com.vasmarfas.card.tools.media.encodeImage
import com.vasmarfas.card.ui.components.ResultCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.pluralStringResource
import kotlin.math.min

val pdfExtensions = setOf("pdf")

class PdfResult(val name: String, val bytes: ByteArray, val pages: Int)

@Composable
fun PdfResultCard(result: PdfResult) {
    ResultCard(title = Res.string.result.str()) {
        Text(
            pluralStringResource(Res.plurals.page_count, result.pages, result.pages) + " · " + formatBytes(result.bytes.size.toLong(), binary = false),
            style = MaterialTheme.typography.bodyLarge,
        )
        SaveButton { saveBytes(result.bytes, result.name) }
    }
}

// 1-based input such as 1-3, 7, 10- to 0-based indices in the order given
fun parsePages(text: String, count: Int): List<Int>? {
    val out = LinkedHashSet<Int>()
    for (part in text.split(',', ';', ' ').map { it.trim() }.filter { it.isNotEmpty() }) {
        val dash = part.indexOf('-')
        val (from, to) = if (dash < 0) {
            val n = part.toIntOrNull() ?: return null
            n to n
        } else {
            val start = if (dash == 0) 1 else part.substring(0, dash).toIntOrNull() ?: return null
            val end = if (dash == part.lastIndex) count else part.substring(dash + 1).toIntOrNull() ?: return null
            start to end
        }
        if (from < 1 || to > count || from > to) return null
        for (n in from..to) out += n - 1
    }
    return out.toList()
}

@Composable
fun OrderedFiles(names: List<String>, details: List<String>, onMove: (from: Int, to: Int) -> Unit, onRemove: (Int) -> Unit, icon: ImageVector = Icons.Filled.PictureAsPdf) {
    names.forEachIndexed { i, name ->
        FileLine(
            name = name,
            detail = details.getOrElse(i) { "" },
            icon = icon,
            trailing = {
                Row {
                    IconButton(onClick = { onMove(i, i - 1) }, enabled = i > 0) { Icon(Icons.Filled.ArrowUpward, contentDescription = Res.string.move_earlier.str()) }
                    IconButton(onClick = { onMove(i, i + 1) }, enabled = i < names.lastIndex) { Icon(Icons.Filled.ArrowDownward, contentDescription = Res.string.move_later.str()) }
                    IconButton(onClick = { onRemove(i) }) { Icon(Icons.Filled.Delete, contentDescription = Res.string.remove.str()) }
                }
            },
        )
    }
}

private val families = HashMap<String, FontFamily>()

private suspend fun family(name: String, italics: Boolean = true): FontFamily = families[name] ?: withContext(Dispatchers.Default) {
    suspend fun font(style: String) = TrueTypeFont(Res.readBytes("files/fonts/$name-$style.ttf"))
    if (italics) {
        FontFamily(font("Regular"), font("Bold"), font("Italic"), font("BoldItalic"))
    } else {
        FontFamily(font("Regular"), font("Bold"))
    }
}.also { families[name] = it }

suspend fun documentFonts(serif: Boolean): DocFonts {
    val sans = family("MobitoolSans")
    return DocFonts(body = if (serif) family("MobitoolSerif") else sans, headings = sans, code = family("MobitoolMono", italics = false))
}

suspend fun preparePictures(blocks: List<Block>, longSide: Int = 1800): Map<Block.Picture, PreparedImage> {
    val out = HashMap<Block.Picture, PreparedImage>()
    suspend fun visit(list: List<Block>) {
        for (block in list) {
            when (block) {
                is Block.Picture -> decodeImage(block.bytes)?.limitedTo(longSide)?.let { image ->
                    out[block] = PreparedImage(withContext(Dispatchers.Default) { encodeImage(image, ImageTarget.JPEG, 85) }, image.width, image.height)
                }
                is Block.ListBlock -> block.items.forEach { visit(it) }
                is Block.Quote -> visit(block.blocks)
                is Block.Table -> block.rows.forEach { row -> row.forEach { visit(it.blocks) } }
                else -> Unit
            }
        }
    }
    visit(blocks)
    return out
}

// a null format makes the page the size of the picture at 150 dpi
object ImagePdf {
    fun build(images: List<PreparedImage>, format: PageFormat?, margin: Float, title: String? = null): ByteArray {
        val writer = PdfWriter()
        val catalog = writer.reserve()
        val tree = writer.reserve()
        val pages = images.map { image ->
            val landscape = image.width > image.height
            val pageWidth = format?.let { if (landscape) it.height else it.width } ?: (image.width * 72f / 150 + margin * 2)
            val pageHeight = format?.let { if (landscape) it.width else it.height } ?: (image.height * 72f / 150 + margin * 2)
            val scale = min((pageWidth - margin * 2) / image.width, (pageHeight - margin * 2) / image.height)
            val w = image.width * scale
            val h = image.height * scale
            val picture = writer.stream(
                PdfDict(
                    "Type" to PdfName("XObject"),
                    "Subtype" to PdfName("Image"),
                    "Width" to PdfInt.of(image.width),
                    "Height" to PdfInt.of(image.height),
                    "ColorSpace" to PdfName("DeviceRGB"),
                    "BitsPerComponent" to PdfInt.of(8),
                    "Filter" to PdfName("DCTDecode"),
                ),
                image.jpeg,
                compress = false,
            )
            val content = "q ${real(w)} 0 0 ${real(h)} ${real((pageWidth - w) / 2)} ${real((pageHeight - h) / 2)} cm /Im1 Do Q"
            writer.add(
                PdfDict(
                    "Type" to PdfName("Page"),
                    "Parent" to tree,
                    "MediaBox" to PdfArray(PdfInt.of(0), PdfInt.of(0), PdfReal(pageWidth.toDouble()), PdfReal(pageHeight.toDouble())),
                    "Resources" to PdfDict("XObject" to PdfDict("Im1" to picture)),
                    "Contents" to writer.stream(PdfDict(), content.encodeToByteArray()),
                ),
            )
        }
        writer[tree] = PdfDict("Type" to PdfName("Pages"), "Kids" to PdfArray(pages.toMutableList<PdfObject>()), "Count" to PdfInt.of(pages.size))
        writer[catalog] = PdfDict("Type" to PdfName("Catalog"), "Pages" to tree)
        val info = PdfDict("Producer" to PdfString.ofText(PdfAssembler.PRODUCER))
        title?.let { info["Title"] = PdfString.ofText(it) }
        return writer.toByteArray(catalog, writer.add(info))
    }

    private fun real(v: Float): String = v.toDouble().fmt(2)
}
