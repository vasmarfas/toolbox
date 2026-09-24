package com.vasmarfas.card.tools.documents

import com.vasmarfas.card.core.fmt
import com.vasmarfas.card.tools.documents.pdf.PdfArray
import com.vasmarfas.card.tools.documents.pdf.PdfAssembler
import com.vasmarfas.card.tools.documents.pdf.PdfDict
import com.vasmarfas.card.tools.documents.pdf.PdfInt
import com.vasmarfas.card.tools.documents.pdf.PdfName
import com.vasmarfas.card.tools.documents.pdf.PdfNull
import com.vasmarfas.card.tools.documents.pdf.PdfObject
import com.vasmarfas.card.tools.documents.pdf.PdfReal
import com.vasmarfas.card.tools.documents.pdf.PdfRef
import com.vasmarfas.card.tools.documents.pdf.PdfString
import com.vasmarfas.card.tools.documents.pdf.PdfWriter
import kotlin.math.max
import kotlin.math.min

enum class PageFormat(val width: Float, val height: Float) {
    A4(595.28f, 841.89f),
    A5(419.53f, 595.28f),
    LETTER(612f, 792f),
}

class TypesetOptions(
    val page: PageFormat = PageFormat.A4,
    val margin: Float = 56.7f,
    val fontSize: Float = 11f,
    val pageNumbers: Boolean = true,
)

class PreparedImage(val jpeg: ByteArray, val width: Int, val height: Int)

object PdfTypesetter {
    fun typeset(doc: Doc, fonts: DocFonts, images: Map<Block.Picture, PreparedImage>, options: TypesetOptions = TypesetOptions()): ByteArray =
        Typesetter(doc, fonts, images, options).run()
}

private class Link(val x0: Float, val y0: Float, val x1: Float, val y1: Float, val uri: String)

private class PageOps {
    val content = StringBuilder()
    val links = ArrayList<Link>()
}

private class OutlineEntry(val level: Int, val title: String, val page: Int, val y: Float)

private fun interface Decoration {
    fun draw(ops: StringBuilder, top: Float, bottom: Float)
}

private sealed interface CellItem {
    val height: Float
}

private class TextItem(val line: Line, val indent: Float) : CellItem {
    override val height: Float get() = line.height
}

private class ImageItem(val image: PreparedImage, val width: Float, override val height: Float) : CellItem

private class GapItem(override val height: Float) : CellItem

private const val BULLET = '•'
private val LINK_COLOR = floatArrayOf(0.08f, 0.29f, 0.66f)

private fun num(v: Float): String = v.toDouble().fmt(2)

// URI actions are 7-bit ASCII, the rest is percent-encoded as UTF-8
private fun asciiUri(uri: String): String {
    val out = StringBuilder()
    for (b in uri.encodeToByteArray()) {
        val v = b.toInt() and 0xFF
        if (v in 0x21..0x7E) out.append(v.toChar()) else out.append('%').append(hex4(v).substring(2))
    }
    return out.toString()
}

private class Typesetter(val doc: Doc, val fonts: DocFonts, val images: Map<Block.Picture, PreparedImage>, val options: TypesetOptions) {
    private val writer = PdfWriter()
    private val faces = Faces(writer, listOf(fonts.body, fonts.headings, fonts.code))
    private val pages = ArrayList<PageOps>()
    private val imageRefs = ArrayList<PdfRef>()
    private val imageIndex = HashMap<PreparedImage, Int>()
    private val outline = ArrayList<OutlineEntry>()
    private val decorations = ArrayList<Decoration>()
    private val base = options.fontSize
    private val left = options.margin
    private val right = options.page.width - options.margin
    private val top = options.page.height - options.margin
    private val bottom = options.margin + if (options.pageNumbers) base else 0f
    private var y = top
    private lateinit var page: PageOps

    fun run(): ByteArray {
        newPage()
        blocks(doc.blocks, left, right - left)
        return finish()
    }

    private fun newPage() {
        page = PageOps()
        pages += page
        y = top
    }

    private val atTop: Boolean get() = y == top

    private fun gap(height: Float) {
        if (atTop) return
        if (y - height < bottom) {
            newPage()
            return
        }
        for (d in decorations) d.draw(page.content, y, y - height)
        y -= height
    }

    private fun reserve(height: Float): Float {
        if (y - height < bottom && !atTop) newPage()
        val at = y
        for (d in decorations) d.draw(page.content, at, at - height)
        y -= height
        return at
    }

    private fun style(family: FontFamily = fonts.body, size: Float = base, text: Inline.Text? = null, bold: Boolean = false, italic: Boolean = false): RunStyle {
        if (text == null) return RunStyle(family, size, bold, italic)
        val runFamily = if (text.code) fonts.code else family
        val runSize = if (text.script == Script.NORMAL) size else size * 0.7f
        return RunStyle(
            family = runFamily,
            size = if (text.code) runSize * 0.92f else runSize,
            bold = bold || text.bold,
            italic = italic || text.italic,
            underline = text.underline || text.link != null,
            strike = text.strike,
            rise = when (text.script) {
                Script.SUPER -> size * 0.33f
                Script.SUB -> -size * 0.15f
                Script.NORMAL -> 0f
            },
            link = text.link,
        )
    }

    private fun pieces(content: List<Inline>, family: FontFamily, size: Float, bold: Boolean = false, italic: Boolean = false, prefix: String? = null): List<Piece> {
        val builder = PieceBuilder(faces)
        if (prefix != null) builder.text(prefix, style(family, size, bold = bold, italic = italic))
        for (inline in content) {
            when (inline) {
                is Inline.Text -> builder.text(inline.text, style(family, size, inline, bold, italic))
                Inline.LineBreak -> builder.hardBreak()
            }
        }
        return builder.finish()
    }

    private fun blocks(list: List<Block>, x: Float, width: Float) {
        list.forEachIndexed { i, block -> block(block, x, width, list.getOrNull(i + 1)) }
    }

    private fun block(block: Block, x: Float, width: Float, next: Block?) {
        when (block) {
            is Block.Heading -> heading(block, x, width, next)
            is Block.Paragraph -> {
                paragraph(pieces(block.content, fonts.body, base), x, width, block.align, 1.4f)
                gap(base * 0.55f)
            }
            is Block.ListBlock -> {
                list(block, x, width)
                gap(base * 0.4f)
            }
            is Block.Quote -> quote(block, x, width)
            is Block.Code -> code(block.text, x, width)
            is Block.Table -> {
                table(block, x, width)
                gap(base * 0.8f)
            }
            is Block.Picture -> picture(block, x, width)
            Block.Rule -> {
                val at = reserve(base * 1.2f)
                page.content.append("0.7 G 0.6 w ").append(num(x)).append(' ').append(num(at - base * 0.6f)).append(" m ")
                    .append(num(x + width)).append(' ').append(num(at - base * 0.6f)).append(" l S\n")
            }
            Block.PageBreak -> if (!atTop) newPage()
        }
    }

    private fun heading(block: Block.Heading, x: Float, width: Float, next: Block?) {
        val level = block.level.coerceIn(1, 6)
        val size = when (level) {
            1 -> base * 1.8f
            2 -> base * 1.45f
            3 -> base * 1.2f
            else -> base * 1.05f
        }
        gap(if (level <= 2) base * 1.1f else base * 0.8f)
        val lines = LineBreaker.lines(pieces(block.content, fonts.headings, size, bold = true, italic = level >= 5), width, Align.START, 1.25f, size)
        val following = if (next is Block.Paragraph || next is Block.ListBlock) base * 1.4f * 2 else 0f
        if (y - lines.sumOf { it.height.toDouble() }.toFloat() - following < bottom && !atTop) newPage()
        outline += OutlineEntry(level, plain(block.content), pages.lastIndex, y)
        for (line in lines) drawLine(line, x, reserve(line.height))
        gap(base * 0.45f)
    }

    private fun paragraph(pieces: List<Piece>, x: Float, width: Float, align: Align, spacing: Float, marker: String? = null, markerWidth: Float = 0f) {
        val lines = LineBreaker.lines(pieces, width, align, spacing, base)
        lines.forEachIndexed { i, line ->
            val at = reserve(line.height)
            if (i == 0 && marker != null) {
                val markerLine = LineBreaker.lines(PieceBuilder(faces).apply { text(marker, style()) }.finish(), markerWidth, Align.END, spacing, base).first()
                drawLine(markerLine, x - markerWidth - base * 0.35f, at - line.baseline + markerLine.baseline)
            }
            drawLine(line, x, at)
        }
    }

    private fun list(block: Block.ListBlock, x: Float, width: Float) {
        val indent = base * 1.6f
        block.items.forEachIndexed { i, item ->
            val marker = if (block.ordered) "${block.start + i}." else BULLET.toString()
            val first = item.firstOrNull()
            if (first is Block.Paragraph) {
                paragraph(pieces(first.content, fonts.body, base), x + indent, width - indent, first.align, 1.4f, marker, indent)
                blocks(item.drop(1), x + indent, width - indent)
            } else {
                paragraph(pieces(emptyList(), fonts.body, base, prefix = marker), x, indent, Align.START, 1.4f)
                blocks(item, x + indent, width - indent)
            }
            if (item.size > 1) gap(base * 0.3f)
        }
    }

    private fun quote(block: Block.Quote, x: Float, width: Float) {
        val bar = x + base * 0.2f
        val decoration = Decoration { ops, from, to ->
            ops.append("0.75 g ").append(num(bar)).append(' ').append(num(to)).append(" 2.2 ").append(num(from - to)).append(" re f\n")
        }
        decorations += decoration
        blocks(block.blocks, x + base * 1.3f, width - base * 1.3f)
        decorations -= decoration
        gap(base * 0.3f)
    }

    private fun code(text: String, x: Float, width: Float) {
        val size = base * 0.85f
        val pad = base * 0.5f
        val decoration = Decoration { ops, from, to ->
            ops.append("0.95 g ").append(num(x)).append(' ').append(num(to)).append(' ').append(num(width)).append(' ').append(num(from - to)).append(" re f\n")
        }
        gap(base * 0.2f)
        decorations += decoration
        reserve(pad / 2)
        for (source in text.trimEnd('\n').split('\n')) {
            val builder = PieceBuilder(faces)
            builder.text(source.ifEmpty { " " }, RunStyle(fonts.code, size), keepSpaces = true)
            for (line in LineBreaker.lines(builder.finish(), width - pad * 2, Align.START, 1.3f, size)) drawLine(line, x + pad, reserve(line.height))
        }
        reserve(pad / 2)
        decorations -= decoration
        gap(base * 0.6f)
    }

    private fun picture(block: Block.Picture, x: Float, width: Float) {
        val prepared = images[block]
        if (prepared == null) {
            if (block.alt.isNotBlank()) paragraph(pieces(listOf(Inline.Text("[${block.alt}]", italic = true)), fonts.body, base), x, width, Align.START, 1.4f)
            return
        }
        val (w, h) = pictureSize(block, prepared, width, top - bottom)
        val at = reserve(h)
        drawImage(prepared, x + (width - w) / 2, at - h, w, h)
        gap(base * 0.6f)
    }

    private fun pictureSize(block: Block.Picture, prepared: PreparedImage, maxWidth: Float, maxHeight: Float): Pair<Float, Float> {
        var w = if (block.widthPt > 0f) block.widthPt else prepared.width * 0.75f
        var h = if (block.heightPt > 0f) block.heightPt else w * prepared.height / prepared.width
        val scale = min(1f, min(maxWidth / w, maxHeight / h))
        w *= scale
        h *= scale
        return w to h
    }

    private fun imageNumber(prepared: PreparedImage): Int = imageIndex.getOrPut(prepared) {
        imageRefs += writer.stream(
            PdfDict(
                "Type" to PdfName("XObject"),
                "Subtype" to PdfName("Image"),
                "Width" to PdfInt.of(prepared.width),
                "Height" to PdfInt.of(prepared.height),
                "ColorSpace" to PdfName("DeviceRGB"),
                "BitsPerComponent" to PdfInt.of(8),
                "Filter" to PdfName("DCTDecode"),
            ),
            prepared.jpeg,
            compress = false,
        )
        imageRefs.size
    }

    private fun drawImage(prepared: PreparedImage, x: Float, y: Float, w: Float, h: Float) {
        val n = imageNumber(prepared)
        page.content.append("q ").append(num(w)).append(" 0 0 ").append(num(h)).append(' ').append(num(x)).append(' ').append(num(y)).append(" cm /Im").append(n).append(" Do Q\n")
    }

    private fun drawLine(line: Line, x: Float, lineTop: Float) {
        val baseline = lineTop - line.baseline
        val ops = page.content
        for (k in line.boxes.indices) {
            val box = line.boxes[k]
            val bx = x + line.xs[k]
            val by = baseline + box.style.rise
            if (box.style.link != null) {
                ops.append(num(LINK_COLOR[0])).append(' ').append(num(LINK_COLOR[1])).append(' ').append(num(LINK_COLOR[2])).append(" rg ")
            } else {
                ops.append(num(box.style.gray)).append(" g ")
            }
            ops.append("BT /").append(box.face.resource).append(' ').append(num(box.style.size)).append(" Tf 1 0 0 1 ").append(num(bx)).append(' ').append(num(by)).append(" Tm [<")
            for (g in box.glyphs.indices) {
                ops.append(hex4(box.glyphs[g]))
                val kern = box.kerns[g]
                if (kern != 0 && g + 1 < box.glyphs.size) ops.append('>').append(kern).append('<')
            }
            ops.append(">] TJ ET\n")
            val scale = box.face.scale(box.style.size)
            if (box.face.ruble) {
                val stem = box.style.size * if (box.face.font.weightClass >= 600) 0.12f else 0.075f
                rule(bx - box.style.size * 0.06f, by + box.face.font.capHeight * scale * 0.3f - stem / 2, box.width * 0.62f, stem)
            }
            if (box.style.underline) rule(bx, by - box.style.size * 0.12f, box.width, box.style.size * 0.06f)
            if (box.style.strike) rule(bx, by + box.face.font.xHeight * scale * 0.5f, box.width, box.style.size * 0.06f)
            box.style.link?.let { uri ->
                page.links += Link(bx, by + box.face.font.descent * scale, bx + box.width, by + box.face.font.ascent * scale, uri)
            }
        }
    }

    private fun rule(x: Float, y: Float, width: Float, thickness: Float) {
        page.content.append(num(x)).append(' ').append(num(y)).append(' ').append(num(width)).append(' ').append(num(max(thickness, 0.4f))).append(" re f\n")
    }

    private fun table(block: Block.Table, x: Float, width: Float) {
        val columns = block.rows.maxOfOrNull { row -> row.sumOf { it.colSpan.coerceAtLeast(1) } } ?: return
        if (columns == 0) return
        val pad = base * 0.35f
        val widths = columnWidths(block, columns, width, pad)
        val laid = block.rows.mapIndexed { r, row ->
            var column = 0
            row.map { cell ->
                val span = cell.colSpan.coerceIn(1, columns - column)
                val cellWidth = (column until column + span).sumOf { widths[it].toDouble() }.toFloat()
                val start = column
                column += span
                Triple(start, cellWidth, cellItems(cell.blocks, cellWidth - pad * 2, header = r < block.headerRows))
            }
        }
        val heights = laid.map { cells -> (cells.maxOfOrNull { c -> c.third.sumOf { it.height.toDouble() }.toFloat() } ?: 0f) + pad * 2 }
        val header = (0 until block.headerRows.coerceAtMost(laid.size)).toList()
        val headerHeight = header.sumOf { heights[it].toDouble() }.toFloat()
        val offsets = FloatArray(columns + 1).also { o -> for (c in 0 until columns) o[c + 1] = o[c] + widths[c] }
        var started = false
        for (r in laid.indices) {
            val height = heights[r]
            if (height > top - bottom - headerHeight) {
                laid[r].forEach { (_, _, items) -> flow(items, x + pad) }
                continue
            }
            if (y - height < bottom && !atTop) {
                newPage()
                if (started && r >= header.size) for (h in header) drawRow(laid[h], heights[h], x, offsets, pad, shaded = true)
            }
            drawRow(laid[r], height, x, offsets, pad, shaded = r < block.headerRows)
            started = true
        }
    }

    private fun drawRow(cells: List<Triple<Int, Float, List<CellItem>>>, height: Float, x: Float, offsets: FloatArray, pad: Float, shaded: Boolean) {
        val at = reserve(height)
        val ops = page.content
        for ((column, cellWidth, items) in cells) {
            val cx = x + offsets[column]
            if (shaded) ops.append("0.93 g ").append(num(cx)).append(' ').append(num(at - height)).append(' ').append(num(cellWidth)).append(' ').append(num(height)).append(" re f\n")
            var cy = at - pad
            for (item in items) {
                when (item) {
                    is TextItem -> drawLine(item.line, cx + pad + item.indent, cy)
                    is ImageItem -> drawImage(item.image, cx + pad, cy - item.height, item.width, item.height)
                    is GapItem -> Unit
                }
                cy -= item.height
            }
            ops.append("0.6 G 0.5 w ").append(num(cx)).append(' ').append(num(at - height)).append(' ').append(num(cellWidth)).append(' ').append(num(height)).append(" re S\n")
        }
    }

    // a row taller than a page is set as plain text so nothing in it is lost
    private fun flow(items: List<CellItem>, x: Float) {
        for (item in items) {
            when (item) {
                is TextItem -> drawLine(item.line, x + item.indent, reserve(item.line.height))
                is ImageItem -> {
                    val at = reserve(item.height)
                    drawImage(item.image, x, at - item.height, item.width, item.height)
                }
                is GapItem -> gap(item.height)
            }
        }
    }

    private fun cellItems(blocks: List<Block>, width: Float, header: Boolean, indent: Float = 0f): List<CellItem> {
        val out = ArrayList<CellItem>()
        val size = base * 0.95f
        fun text(content: List<Inline>, align: Align = Align.START, bold: Boolean = header, prefix: String? = null, family: FontFamily = if (header) fonts.headings else fonts.body) {
            LineBreaker.lines(pieces(content, family, size, bold = bold, prefix = prefix), width - indent, align, 1.3f, size).mapTo(out) { TextItem(it, indent) }
        }
        for ((i, block) in blocks.withIndex()) {
            if (i > 0) out += GapItem(base * 0.3f)
            when (block) {
                is Block.Heading -> text(block.content, bold = true)
                is Block.Paragraph -> text(block.content, block.align)
                is Block.ListBlock -> block.items.forEachIndexed { n, item ->
                    val marker = if (block.ordered) "${block.start + n}. " else "$BULLET "
                    val first = item.firstOrNull() as? Block.Paragraph
                    if (first != null) text(first.content, prefix = marker) else text(emptyList(), prefix = marker)
                    out += cellItems(if (first != null) item.drop(1) else item, width, header, indent + base)
                }
                is Block.Quote -> out += cellItems(block.blocks, width, header, indent + base)
                is Block.Code -> text(listOf(Inline.Text(block.text, code = true)))
                is Block.Table -> block.rows.forEach { row ->
                    text(listOf(Inline.Text(row.joinToString("  |  ") { cell -> cell.blocks.joinToString(" ") { plainBlock(it) } })))
                }
                is Block.Picture -> images[block]?.let { prepared ->
                    val (w, h) = pictureSize(block, prepared, width - indent, top - bottom - base * 4)
                    out += ImageItem(prepared, w, h)
                }
                Block.Rule, Block.PageBreak -> out += GapItem(base * 0.5f)
            }
        }
        return out
    }

    private fun columnWidths(block: Block.Table, columns: Int, width: Float, pad: Float): FloatArray {
        val least = FloatArray(columns) { pad * 2 + base }
        val most = FloatArray(columns) { pad * 2 + base }
        for ((r, row) in block.rows.withIndex()) {
            val header = r < block.headerRows
            var column = 0
            for (cell in row) {
                if (column >= columns) break
                if (cell.colSpan <= 1) {
                    var longestWord = 0f
                    var widest = 0f
                    for (b in cell.blocks) {
                        val inlines = when (b) {
                            is Block.Paragraph -> b.content
                            is Block.Heading -> b.content
                            else -> listOf(Inline.Text(plainBlock(b)))
                        }
                        var lineWidth = 0f
                        for (piece in pieces(inlines, if (header) fonts.headings else fonts.body, base * 0.95f, bold = header)) {
                            when (piece) {
                                is Word -> {
                                    longestWord = max(longestWord, piece.width)
                                    lineWidth += piece.width
                                }
                                is Glue -> lineWidth += piece.width
                                HardBreak -> {
                                    widest = max(widest, lineWidth)
                                    lineWidth = 0f
                                }
                            }
                        }
                        widest = max(widest, lineWidth)
                    }
                    least[column] = max(least[column], min(longestWord, width / 3) + pad * 2)
                    most[column] = max(most[column], widest + pad * 2)
                }
                column += cell.colSpan.coerceAtLeast(1)
            }
        }
        val total = most.sum()
        if (total <= width) return FloatArray(columns) { most[it] * width / total }
        val minimum = least.sum()
        if (minimum >= width) return FloatArray(columns) { least[it] * width / minimum }
        val flexible = (0 until columns).sumOf { (most[it] - least[it]).toDouble() }.toFloat()
        return FloatArray(columns) { least[it] + (most[it] - least[it]) * (width - minimum) / flexible }
    }

    private fun plain(content: List<Inline>): String = content.joinToString("") {
        when (it) {
            is Inline.Text -> it.text
            Inline.LineBreak -> " "
        }
    }.trim()

    private fun plainBlock(block: Block): String = when (block) {
        is Block.Heading -> plain(block.content)
        is Block.Paragraph -> plain(block.content)
        is Block.ListBlock -> block.items.joinToString(" ") { item -> item.joinToString(" ") { plainBlock(it) } }
        is Block.Quote -> block.blocks.joinToString(" ") { plainBlock(it) }
        is Block.Code -> block.text
        is Block.Table -> block.rows.joinToString(" ") { row -> row.joinToString(" ") { cell -> cell.blocks.joinToString(" ") { plainBlock(it) } } }
        is Block.Picture -> block.alt
        Block.Rule, Block.PageBreak -> ""
    }

    private fun finish(): ByteArray {
        if (options.pageNumbers && pages.size > 1) {
            pages.forEachIndexed { i, p ->
                page = p
                val line = LineBreaker.lines(PieceBuilder(faces).apply { text("${i + 1}", RunStyle(fonts.headings, base * 0.8f, gray = 0.45f)) }.finish(), right - left, Align.CENTER, 1f, base)
                drawLine(line.first(), left, options.margin / 2 + base)
            }
        }
        val catalog = writer.reserve()
        val tree = writer.reserve()
        val pageRefs = pages.map { writer.reserve() }
        val fontsDict = PdfDict()
        val xobjects = PdfDict()
        imageRefs.forEachIndexed { i, ref -> xobjects["Im${i + 1}"] = ref }
        for (face in faces.all) fontsDict[face.resource] = face.ref
        val resources = PdfDict("Font" to fontsDict)
        if (imageRefs.isNotEmpty()) resources["XObject"] = xobjects
        val shared = writer.add(resources)
        pages.forEachIndexed { i, p ->
            val dict = PdfDict(
                "Type" to PdfName("Page"),
                "Parent" to tree,
                "MediaBox" to PdfArray(PdfInt.of(0), PdfInt.of(0), PdfReal(options.page.width.toDouble()), PdfReal(options.page.height.toDouble())),
                "Resources" to shared,
                "Contents" to writer.stream(PdfDict(), p.content.toString().encodeToByteArray()),
            )
            if (p.links.isNotEmpty()) {
                dict["Annots"] = PdfArray(
                    p.links.mapTo(ArrayList<PdfObject>()) { link ->
                        PdfDict(
                            "Type" to PdfName("Annot"),
                            "Subtype" to PdfName("Link"),
                            "Rect" to PdfArray(PdfReal(link.x0.toDouble()), PdfReal(link.y0.toDouble()), PdfReal(link.x1.toDouble()), PdfReal(link.y1.toDouble())),
                            "Border" to PdfArray(PdfInt.of(0), PdfInt.of(0), PdfInt.of(0)),
                            "A" to PdfDict("S" to PdfName("URI"), "URI" to PdfString(asciiUri(link.uri).encodeToByteArray())),
                        )
                    },
                )
            }
            writer[pageRefs[i]] = dict
        }
        for (face in faces.all) face.write(writer)
        writer[tree] = PdfDict("Type" to PdfName("Pages"), "Kids" to PdfArray(pageRefs.toMutableList<PdfObject>()), "Count" to PdfInt.of(pageRefs.size))
        val root = PdfDict("Type" to PdfName("Catalog"), "Pages" to tree)
        doc.language?.let { root["Lang"] = PdfString.ofText(it) }
        if (outline.isNotEmpty()) {
            root["Outlines"] = outlines(pageRefs)
            root["PageMode"] = PdfName("UseOutlines")
        }
        writer[catalog] = root
        val info = PdfDict("Producer" to PdfString.ofText(PdfAssembler.PRODUCER), "Creator" to PdfString.ofText(PdfAssembler.PRODUCER))
        doc.title?.let { info["Title"] = PdfString.ofText(it) }
        doc.author?.let { info["Author"] = PdfString.ofText(it) }
        return writer.toByteArray(catalog, writer.add(info))
    }

    private fun outlines(pageRefs: List<PdfRef>): PdfRef {
        val root = writer.reserve()
        val refs = outline.map { writer.reserve() }
        val parents = IntArray(outline.size) { -1 }
        val stack = ArrayList<Int>()
        for (i in outline.indices) {
            while (stack.isNotEmpty() && outline[stack.last()].level >= outline[i].level) stack.removeAt(stack.lastIndex)
            parents[i] = stack.lastOrNull() ?: -1
            stack += i
        }
        val children = HashMap<Int, MutableList<Int>>()
        for (i in outline.indices) children.getOrPut(parents[i]) { ArrayList() } += i
        for (i in outline.indices) {
            val entry = outline[i]
            val siblings = children.getValue(parents[i])
            val at = siblings.indexOf(i)
            val dict = PdfDict(
                "Title" to PdfString.ofText(entry.title.ifEmpty { "…" }),
                "Parent" to if (parents[i] >= 0) refs[parents[i]] else root,
                "Dest" to PdfArray(pageRefs[entry.page], PdfName("XYZ"), PdfNull, PdfReal(entry.y.toDouble()), PdfNull),
            )
            if (at > 0) dict["Prev"] = refs[siblings[at - 1]]
            if (at + 1 < siblings.size) dict["Next"] = refs[siblings[at + 1]]
            children[i]?.let { kids ->
                dict["First"] = refs[kids.first()]
                dict["Last"] = refs[kids.last()]
                dict["Count"] = PdfInt.of(-kids.size)
            }
            writer[refs[i]] = dict
        }
        val top = children.getValue(-1)
        writer[root] = PdfDict("Type" to PdfName("Outlines"), "First" to refs[top.first()], "Last" to refs[top.last()], "Count" to PdfInt.of(top.size))
        return root
    }
}
